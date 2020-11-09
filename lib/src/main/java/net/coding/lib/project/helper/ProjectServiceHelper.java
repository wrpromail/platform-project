package net.coding.lib.project.helper;


import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.bean.setting.AtSetting;
import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.gson.JSON;
import net.coding.common.util.TextUtils;
import net.coding.e.proto.ActivitiesProto;
import net.coding.grpc.client.activity.ActivityGrpcClient;
import net.coding.grpc.client.template.TemplateGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.ActivityEnums;
import net.coding.lib.project.event.NotificationEvent;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProfanityWordService;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectPreferenceService;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.download.DownloadImageService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.lib.project.utils.ResourceUtil;
import net.coding.lib.project.utils.TextUtil;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import proto.notification.NotificationProto;
import proto.platform.user.UserProto;

import static java.util.stream.Collectors.toList;
import static net.coding.common.constants.ProjectConstants.PROJECT_PRIVATE;
import static net.coding.common.constants.ValidationConstants.TWEET_LIMIT_IMAGES;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_STATUS_TRUE;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_TYPE_PROJECT_TWEET;
import static net.coding.lib.project.exception.CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TWEET_IMAGE_LIMIT_N;

@Component
@Slf4j
public class ProjectServiceHelper {

    public static final Pattern AT_REG = Pattern.compile("(@([^@\\s<>()（）：:，,。…~!！？?'‘\"]+))(.{0}|\\s)");

    @Resource
    private UserGrpcClient userGrpcClient;

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private ProfanityWordService profanityWordService;

    @Resource
    private ProjectResourceLinkService projectResourceLinkService;

    @Resource
    private ProjectPreferenceService projectPreferenceService;

    @Resource
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private AsyncEventBus asyncEventBus;

    @Autowired
    private TeamGrpcClient teamGrpcClient;

    @Autowired
    private ActivityGrpcClient activityGrpcClient;

    @Autowired
    private NotificationGrpcClient notifiactionGrpcClient;

    @Autowired
    private TemplateGrpcClient templateGrpcClient;


    /**
     * 解析 {@code content} 并检查内容是否合法.
     *
     * @param content 文本
     * @param project 项目
     * @return 解析后的文本
     * @throws CoreException 图片数目超出限制/包含限制词
     */
    public String updateAndCheckContent(String content, Project project, Integer teamId) throws CoreException {
        String newContent = projectResourceLinkService.linkize(content, project);
        newContent = templateGrpcClient.markdownWithMonkey(teamId, newContent, false);
        newContent = TextUtil.filterUserInputContent(newContent);
        //newContent = downloadImageService.filterHTML(newContent);

        // 图片数目超过限制
        if (limitTweetImages(newContent, TWEET_LIMIT_IMAGES)) {
            throw CoreException.of(TWEET_IMAGE_LIMIT_N, TWEET_LIMIT_IMAGES);
        }
        // 包含限制词
        String profanity = profanityWordService.checkContent(newContent);
        if (StringUtils.isNotEmpty(profanity)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, profanity);
        }
        return newContent;
    }

    /**
     * 判断用户冒泡的图片数量是否超限
     */
    public boolean limitTweetImages(String content, int amount) {
        Document doc = Jsoup.parse(content);
        Elements eles = doc.select("img.bubble-markdown-image");
        return eles.size() > amount;
    }

    public Set<Integer> parseAtUser(Integer userId, Project project, String content, Integer targetOwnerId) {
        Set<Integer> userIdSet = new HashSet<>();
        if (userId == null || project == null || StringUtils.isEmpty(content)) {
            return userIdSet;
        }

        if (null == project.getTeamOwnerId()) {
            return userIdSet;
        }

        Matcher matcher = AT_REG.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(2);
            if ("all".equals(StringUtils.lowerCase(name))) {
                userIdSet.addAll(projectMemberService.findListByProjectId(project.getId()).stream().map(projectMember -> projectMember.getId()).collect(Collectors.toSet()));
                break;
            }
            UserProto.User user= userGrpcClient.getUserByNameAndTeamId(name, project.getTeamOwnerId());
            if(null == user) {
                user = userGrpcClient.getUserByGlobalKey(name);
            }
            if (null == user) {
                continue;
            }
            if (project.getType().equals(PROJECT_PRIVATE) && !this.isMember(user, project.getId())) {
                continue;
            }
            if (Objects.equals(Integer.valueOf(user.getId()), targetOwnerId)) {
                continue;
            }
            userIdSet.add(user.getId());
        }
        userIdSet.remove(userId);
        return userIdSet;
    }

    /**
     * 通知项目内所有人创建了一条冒泡，创建者自身及被 @ 的人除外
     */
    public void notifyMembers(Integer userId, String content, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus().shortValue() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            List<Integer> userIds = filterNotifyUserIds(userId, content, project, tweet);

            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String message = ResourceUtil.ui("notification_create_project_tweet",
                        userLink, projectPath, this.getTweetHtmlLink(tweet, project, projectPath));

                // 站内通知
                asyncEventBus.post(NotificationEvent.builder()
                        .userIds(userIds)
                        .content(message)
                        .targetType(tweet.getClass().getSimpleName())
                        .targetId(tweet.getId().toString())
                        .baseSettingClass(AtSetting.class)
                        .build());
            }
        }
    }

    /**
     * 通知项目内所有人创建了一条冒泡，创建者自身及被 @ 的人除外
     */
    public void notifyUpdateMembers(Integer userId, String content, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus().shortValue() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            List<Integer> userIds = filterNotifyUserIds(userId, content, project, tweet);

            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String message = ResourceUtil.ui("notification_update_project_notice",
                        userLink, projectPath, this.getTweetHtmlLink(tweet, project, projectPath));

                notifiactionGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                        .addAllUserId(userIds)
                        .setContent(message)
                        .setTargetType(NotificationProto.TargetType.Project)
                        .setTargetId(tweet.getId().toString())
                        .setSetting(NotificationProto.Setting.AtSetting)
                        .setSkipValidate(false)
                        .setSkipEmail(false)
                        .setSkipSystem(false)
                        .setSkipWechatWorkMessage(true)
                        .setForce(false)
                        .build());
            }
        }
    }

    public String getTweetHtmlLink(ProjectTweet tweet, Project project, String projectPath) {
        String host = teamGrpcClient.getTeamHostWithProtocolByTeamId(project.getTeamOwnerId());
        String path = tweetPath(tweet, project, projectPath);
        String tweetString = TextUtil.abstractHtml(tweet.getContent(), 20);
        String url = host + path;
        return ResourceUtil.ui("resource_link", url, tweetString);
    }

    public String tweetPath(ProjectTweet projectTweet, Project project, String projectPath) {
        if (userGrpcClient.getUserById(projectTweet.getOwnerId()) == null) {
            return StringUtils.EMPTY;
        }

        if (project == null) {
            return StringUtils.EMPTY;
        }

        return projectPath + "/setting/notice/" + projectTweet.getId();
    }

    public boolean isMember(UserProto.User user, Integer projectId) {
        if (Objects.isNull(user) || Objects.isNull(projectId)) {
            return false;
        }
        if(projectGrpcClient.isProjectRobotUser(user.getGlobalKey())) {
            return true;
        }
        return projectMemberService.getByProjectIdAndUserId(projectId, user.getId()) != null;
    }

    public List<Integer> filterNotifyUserIds(
            Integer userId,
            String content,
            Project project,
            ProjectTweet tweet) {
        // 获取被 @ 的用户编号
        Set<Integer> atUserIds = parseAtUser(userId, project, content, tweet.getOwnerId());
        // 获取用户列表
        List<ProjectMember> members = projectMemberService.findListByProjectId(project.getId());
        // 过滤创建者自身及被 @ 的用户
        List<Integer> userIds = members.stream()
                .filter(m ->
                        !Objects.equals(m.getUserId(), userId)
                                && !atUserIds.contains(m.getUserId()))
                .map(ProjectMember::getUserId)
                .collect(toList());
        return userIds;
    }

    /**
     * 处理content中的@通知 user: 发送者
     */
    public void notifyAtMembers(
            Integer userId,
            String content,
            ProjectTweet tweet,
            Project project,
            Boolean appendContent) {
        Set<Integer> userIds = parseAtUser(userId, project, content, tweet.getOwnerId());

        String notification;
        String userLink = userGrpcClient.getUserHtmlLinkById(userId);
        String projectPath = projectGrpcClient.getProjectPath(project.getId());

        if (appendContent) {
            // 附加信息
            Document document = TextUtils.replaceImagesTag(content);
            String abstractMessage = ":" + TextUtils.getFixedLengthString(TextUtils.htmlEscape(
                    TextUtils.getPlainText(document.body().html())), 40, true);

            notification = ResourceUtil.ui(
                    "notification_project_tweet_refer_user", userLink,
                    this.getTweetHtmlLink(tweet, project, projectPath),
                    abstractMessage);
        } else {
            // 不附加信息
            notification = ResourceUtil.ui(
                    "notification_project_tweet_refer_user", userLink,
                    this.getTweetHtmlLink(tweet, project, projectPath),
                    StringUtils.EMPTY);
        }

        // 站内通知
        asyncEventBus.post(NotificationEvent.builder()
                .userIds(userIds)
                .content(notification)
                .targetType(tweet.getClass().getSimpleName())
                .targetId(tweet.getId().toString())
                .baseSettingClass(AtSetting.class)
                .build());

        // TODO: 等移动端做了项目内冒泡后加上，且推送格式要修改
        // task: https://coding.net/u/wzw/p/coding/task/74923
    }

    public void postProjectTweetCreateActivity(Project project, ProjectTweet tweet, Integer userId, ActivityEnums activityEnums, Short action, String actionStr) {
        try {
            Map<String, String> mapInfo = new HashMap<>(1 << 2);
            mapInfo.put("raw", tweet.getRaw());
            mapInfo.put("content", tweet.getContent());
            // 站内通知
            asyncEventBus.post(ActivityEvent.builder()
                    .creatorId(userId)
                    .type(net.coding.common.base.bean.ProjectTweet.class)
                    .targetId(tweet.getId())
                    .projectId(tweet.getProjectId())
                    .action(action)
                    .content(JSON.toJson(mapInfo))
                    .build());

            Map<String, String> map = new HashMap<>(1 << 2);
            map.put("content", tweet.getContent());
            map.put("notice_id", tweet.getId().toString());
            map.put("action", actionStr);
            ActivitiesProto.SendActivitiesRequest request = ActivitiesProto.SendActivitiesRequest.newBuilder()
                    .setProjectId(project.getId())
                    .setOwnerId(userId)
                    .setTargetId(tweet.getId())
                    .setTargetType(ProjectTweet.class.getSimpleName())
                    .setContent(JSON.toJson(map))
                    .setAction(activityEnums.getAction())
                    .setCreatedAt(DateUtil.getCurrentDate().getTime())
                    .build();
            activityGrpcClient.sendActivity(request);
        } catch (Exception ex) {
            log.error("发送动态失败！ex={}", ex);
        }
    }
}
