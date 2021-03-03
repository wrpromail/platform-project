package net.coding.lib.project.helper;


import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.bean.setting.AtSetting;
import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.event.CreateProjectUserEvent;
import net.coding.common.base.event.ProjectCreateEvent;
import net.coding.common.base.event.ProjectDeleteEvent;
import net.coding.common.base.event.ProjectMemberCreateEvent;
import net.coding.common.base.event.ProjectMemberRoleChangeEvent;
import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.base.event.ProjectNameChangeEvent;
import net.coding.common.base.gson.JSON;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.TextUtils;
import net.coding.e.proto.ActivitiesProto;
import net.coding.exchange.dto.user.User;
import net.coding.grpc.client.activity.ActivityGrpcClient;
import net.coding.grpc.client.pinyin.PinyinClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.entity.ProjectSetting;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.ActivityEnums;
import net.coding.lib.project.event.NotificationEvent;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.metrics.ProjectCreateMetrics;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProfanityWordService;
import net.coding.lib.project.service.ProjectPreferenceService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.lib.project.utils.ResourceUtil;
import net.coding.lib.project.utils.TextUtil;
import net.coding.proto.CredentialProto;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.notification.NotificationProto;
import proto.platform.logging.loggingProto;
import proto.platform.project.ProjectProto;

import static net.coding.common.constants.ProjectConstants.ACTION_CREATE;
import static net.coding.common.constants.ProjectConstants.ACTION_DELETE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DATE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DESCRIPTION;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DISPLAY_NAME;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_NAME;
import static net.coding.grpc.client.pinyin.PinyinClient.DEFAULT_SEPARATOR;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_STATUS_TRUE;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_TYPE_PROJECT_TWEET;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectServiceHelper {

    private final UserGrpcClient userGrpcClient;

    private final ProfanityWordService profanityWordService;

    private final ProjectPreferenceService projectPreferenceService;

    private final ProjectGrpcClient projectGrpcClient;

    private final AsyncEventBus asyncEventBus;

    private final TeamGrpcClient teamGrpcClient;

    private final ActivityGrpcClient activityGrpcClient;

    private final NotificationGrpcClient notificationGrpcClient;

    private final PinyinClient pinyinClient;

    private final LoggingGrpcClient loggingGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    private final UserServiceGrpcClient userServiceGrpcClient;

    public String checkContent(String content) {
        // 包含限制词
        String profanity = profanityWordService.checkContent(content);
        return profanity;
    }

    public String getPinYin(String displayName, String name) {
        return pinyinClient.combined(
                StringUtils.defaultIfBlank(
                        displayName,
                        name
                ),
                DEFAULT_SEPARATOR
        );
    }

    /**
     * 通知项目内所有人创建了一条冒泡，创建者自身及被 @ 的人除外
     */
    public void notifyMembers(List<Integer> userIds, Integer userId, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus().shortValue() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String projectHtmlUrl = projectHtmlLink(project.getId());
                String message = ResourceUtil.ui("notification_create_project_tweet",
                        userLink, projectHtmlUrl, this.getTweetHtmlLink(tweet, project, projectPath));

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

    public String projectHtmlLink(Integer projectId) {
        ProjectProto.Project project = projectGrpcClient.getProjectById(projectId);
        StringBuilder sb = new StringBuilder();
        sb.append("<a href='");
        sb.append(project.getHtmlUrl());
        sb.append("' target='_blank'>");
        sb.append(StringUtils.isNotBlank(project.getDisplayName()) ? project.getDisplayName() : project.getName());
        sb.append("</a>");
        return sb.toString();
    }

    /**
     * 通知项目内所有人创建了一条冒泡，创建者自身及被 @ 的人除外
     */
    public void notifyUpdateMembers(List<Integer> userIds, Integer userId, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus().shortValue() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String projectHtmlUrl = projectHtmlLink(project.getId());
                String message = ResourceUtil.ui("notification_update_project_notice",
                        userLink, projectHtmlUrl, this.getTweetHtmlLink(tweet, project, projectPath));

                notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                        .addAllUserId(userIds)
                        .setContent(Optional.ofNullable(message).orElse(null))
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

    /**
     * 处理content中的@通知 user: 发送者
     */
    public void notifyAtMembers(
            Set<Integer> userIds,
            Integer userId,
            String content,
            ProjectTweet tweet,
            Project project,
            Boolean appendContent) {
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

            Map map = new HashMap<>(1 << 2);
            map.put("content", tweet.getContent());
            map.put("notice_id", tweet.getId());
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
            log.error("Send activity failed！ex={}", ex.getMessage());
        }
    }

    public void postProjectNameChangeEvent(Project project) {
        ProjectNameChangeEvent projectNameChangeEvent = ProjectNameChangeEvent.builder().projectId(project.getId()).newName(project.getName()).build();
        asyncEventBus.post(projectNameChangeEvent);
    }

    public void postNameActivityEvent(Integer userId, Project project) {
        ActivityEvent nameActivityEvent = ActivityEvent.builder().creatorId(userId).type(net.coding.e.lib.core.bean.Project.class)
                .targetId(project.getId()).projectId(project.getId()).action(ACTION_UPDATE_NAME).content(StringUtils.EMPTY).build();
        asyncEventBus.post(nameActivityEvent);
    }

    public void postDisplayNameActivityEvent(Integer userId, Project project) {
        ActivityEvent displayNameActivityEvent = ActivityEvent.builder().creatorId(userId).type(net.coding.e.lib.core.bean.Project.class)
                .targetId(project.getId()).projectId(project.getId()).action(ACTION_UPDATE_DISPLAY_NAME).content(StringUtils.EMPTY).build();
        asyncEventBus.post(displayNameActivityEvent);
    }

    public void postDescriptionActivityEvent(Integer userId, Project project) {
        ActivityEvent descriptionActivityEvent = ActivityEvent.builder().creatorId(userId).type(net.coding.e.lib.core.bean.Project.class)
                .targetId(project.getId()).projectId(project.getId()).action(ACTION_UPDATE_DESCRIPTION).content(StringUtils.EMPTY).build();
        asyncEventBus.post(descriptionActivityEvent);
    }

    public void postDateActivityEvent(Integer userId, Project project) {
        ActivityEvent dateActivityEvent = ActivityEvent.builder().creatorId(userId).type(net.coding.e.lib.core.bean.Project.class)
                .targetId(project.getId()).projectId(project.getId()).action(ACTION_UPDATE_DATE).content(StringUtils.EMPTY).build();
        asyncEventBus.post(dateActivityEvent);
    }

    public void postIconActivityEvent(Integer userId, Project project) {
        ActivityEvent iconActivityEvent = ActivityEvent.builder().creatorId(userId)
                .type(net.coding.e.lib.core.bean.Project.class)
                .targetId(project.getId())
                .projectId(project.getId())
                .action(ACTION_UPDATE)
                .content(StringUtils.EMPTY)
                .build();
        asyncEventBus.post(iconActivityEvent);
    }

    public void postFunctionActivity(Integer userId, ProjectSetting projectSetting, Short action) {
        ActivityEvent funActivityEvent = ActivityEvent.builder()
                .creatorId(userId)
                .type(net.coding.e.lib.core.bean.ProjectSetting.class)
                .targetId(projectSetting.getId())
                .projectId(projectSetting.getProjectId())
                .action(action)
                .content(projectSetting.getCode())
                .build();
        asyncEventBus.post(funActivityEvent);
    }

    public void postProjectDeleteEvent(Integer userId, Project project) {
        asyncEventBus.post(
                ProjectDeleteEvent.builder()
                        .teamId(project.getTeamOwnerId())
                        .userId(userId)
                        .projectId(project.getId())
                        .build()
        );

        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.e.lib.core.bean.Project.class)
                        .targetId(project.getId())
                        .projectId(project.getId())
                        .action(ACTION_DELETE)
                        .content("")
                        .build()
        );

        loggingGrpcClient.insertOperationLog(loggingProto.OperationLogInsertRequest.newBuilder()
                .setUserId(userId)
                .setTeamId(project.getTeamOwnerId())
                .setContentName("deleteProject")
                .setTargetId(project.getId())
                .setTargetType(project.getClass().getSimpleName())
                .setAdminAction(false)
                .setText(localeMessageSource.getMessage("project_deleted",
                        new Object[]{htmlLink(project)}))

                .build());
    }

    public String htmlLink(Project project) {
        String host = teamGrpcClient.getTeamHostWithProtocolByTeamId(project.getTeamOwnerId());
        StringBuilder sb = new StringBuilder();
        sb.append("<a href='");
        sb.append(host);
        sb.append("/p/" + project.getName());
        sb.append("' target='_blank'>");
        sb.append(StringUtils.defaultIfBlank(project.getDisplayName(), project.getName()));
        sb.append("</a>");
        return sb.toString();
    }

    /**
     * 添加成员推送消息
     *
     * @param operationUserId
     * @param projectId
     */
    public void postAddMembersEvent(AtomicInteger insertRole, Integer operationUserId, Integer projectId, ProjectMember projectMember, Integer userId, boolean isInvite) {
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(operationUserId)
                        .type(net.coding.e.lib.core.bean.ProjectMember.class)
                        .targetId(projectMember.getId())
                        .projectId(projectId)
                        .action(ProjectMember.ACTION_ADD_MEMBER)
                        .content("")
                        .build()

        );
        asyncEventBus.post(
                ProjectMemberCreateEvent.builder()
                        .projectId(projectId)
                        .userId(projectMember.getUserId())
                        .build()
        );
        if (insertRole.intValue() > 0) {
            asyncEventBus.post(ProjectMemberRoleChangeEvent.builder()
                    .projectId(projectId)
                    .roleId(insertRole.intValue())
                    .targetUserId(projectMember.getUserId())
                    .operate(1)
                    .build()
            );
        }
        String userLink = userGrpcClient.getUserHtmlLinkById(operationUserId);
        String projectHtmlUrl = projectHtmlLink(projectId);
        String message = ResourceUtil.ui("notification_add_member",
                userLink, projectHtmlUrl);
        String inviteMessage = ResourceUtil.ui("notification_invite_member",
                userLink, projectHtmlUrl);
        if (!userId.equals(operationUserId)) {
            // 站内通知
            List<Integer> userIds = new ArrayList<>();
            userIds.add(userId);
            sentProjectMemberNotification(userIds, message, projectId);
            if (isInvite) {
                sentProjectMemberNotification(userIds, inviteMessage, projectId);
            }
        }
    }


    public void postDeleteMemberEvent(Integer currentUserId, Integer projectId, Integer targetUserId){
        asyncEventBus.post(
                ProjectMemberDeleteEvent.builder()
                        .projectId(projectId)
                        .userId(targetUserId)
                        .build()
        );

        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(currentUserId)
                        .type(net.coding.e.lib.core.bean.ProjectMember.class)
                        .targetId(targetUserId)
                        .projectId(projectId)
                        .action(net.coding.e.lib.core.bean.ProjectMember.ACTION_REMOVE_MEMBER)
                        .content("")
                        .build()
        );
        List<Integer> userIds = new ArrayList<>();
        userIds.add(targetUserId);
        String userLink = userGrpcClient.getUserHtmlLinkById(currentUserId);
        String projectHtmlUrl = projectHtmlLink(projectId);
        String message = ResourceUtil.ui("notification_delete_member",
                userLink, projectHtmlUrl);
        sentProjectMemberNotification(userIds,message,projectId);
    }


    private void sentProjectMemberNotification(List<Integer> userIds,String message,Integer projectId){
        notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                .addAllUserId(userIds)
                .setContent(Optional.ofNullable(message).orElse(null))
                .setTargetType(NotificationProto.TargetType.ProjectMember)
                .setTargetId(projectId.toString())
                .setSetting(NotificationProto.Setting.ProjectMemberSetting)
                .setSkipValidate(false)
                .setSkipEmail(false)
                .setSkipSystem(false)
                .setSkipWechatWorkMessage(true)
                .setForce(false)
                .build());
    }


    public void postProjectCreateEvent(Project project,
                                       ProjectCreateParameter parameter,
                                       CredentialProto.Credential credential) {
        Map<String, String> initMap = new HashMap<>();
        initMap.put("createSvnLayout", parameter.getCreateSvnLayout());
        initMap.put("credential", Optional.ofNullable(credential)
                .map(CredentialProto.Credential::getCredentialId).orElse(""));
        initMap.put("credentialType", Optional.ofNullable(credential)
                .map(c -> c.getType().name()).orElse(""));
        Boolean gitEnabled = parameter.getGitEnabled();
        if (gitEnabled) {
            initMap.put("readme", parameter.getGitReadmeEnabled());
            initMap.put("gitignore", parameter.getGitIgnore());
            initMap.put("license", parameter.getGitLicense());
            initMap.put("template", parameter.getTemplate());
        } else {
            initMap.put("readme", "no");
            initMap.put("gitignore", "no");
            initMap.put("license", "no");
        }

        // ProjectCreateEvent 的触发要放到添加完成员后，否则创建团队项目时用户会无权限添加 README
        long prevTime = System.currentTimeMillis();
        User user = userServiceGrpcClient.getUser(parameter.getUserId());
        asyncEventBus.post(
                ProjectCreateEvent.builder()
                        .projectId(project.getId())
                        .fork(false)
                        .parentId(0)
                        .rootId(0)
                        .initMap(initMap)
                        .quota(100 * 1024)
                        .creator(user)
                        .vcsType(parameter.getVcsType())
                        .shared(parameter.getShared() == 1)
                        .initDepot(parameter.getShouldInitDepot())
                        .build() // 100G
        );

        ProjectCreateMetrics.setProjectCreateEvent(System.currentTimeMillis() - prevTime);

        if (!project.getInvisible()) {
            asyncEventBus.post(
                    ActivityEvent.builder()
                            .creatorId(parameter.getUserId())
                            .type(net.coding.e.lib.core.bean.Project.class)
                            .targetId(project.getId())
                            .projectId(project.getId())
                            .action(ACTION_CREATE)
                            .content("")
                            .build()
            );
            loggingGrpcClient.insertOperationLog(loggingProto.OperationLogInsertRequest.newBuilder()
                    .setUserId(parameter.getUserId())
                    .setTeamId(parameter.getTeamId())
                    .setContentName("createProject")
                    .setTargetId(project.getId())
                    .setTargetType(project.getClass().getSimpleName())
                    .setAdminAction(false)
                    .setText(localeMessageSource.getMessage("project_created",
                            new Object[]{""
                                    , htmlLink(project)}).trim())
                    .build()
            );
        }

        asyncEventBus.post(CreateProjectUserEvent.builder()
                .userId(parameter.getUserId())
                .teamId(parameter.getTeamId())
                .projectId(project.getId())
                .build()
        );
    }

    public void sendCreateProjectNotification(Integer ownerId, Integer userId, Project project) {
        notificationGrpcClient.send(
                NotificationProto
                        .NotificationSendRequest
                        .newBuilder()
                        .addUserId(ownerId)
                        .setContent(
                                localeMessageSource.getMessage("project_created",
                                        new Object[]{userGrpcClient.getUserHtmlLinkById(userId)
                                                , htmlLink(project)}))
                        .setTargetType(NotificationProto.TargetType.Project)
                        .setTargetId(String.valueOf(project.getId()))
                        .setSetting(NotificationProto.Setting.ProjectMemberSetting)
                        .build()
        );
    }
}
