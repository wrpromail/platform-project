package net.coding.lib.project.helper;


import com.google.common.eventbus.AsyncEventBus;

import java.util.stream.Collectors;
import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.event.CreateProjectUserEvent;
import net.coding.common.base.event.ProjectCreateEvent;
import net.coding.common.base.gson.JSON;
import net.coding.common.eventbus.AsyncExternalEventBus;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.TextUtils;
import net.coding.e.proto.ActivitiesProto;
import net.coding.events.all.platform.CommonProto;
import net.coding.events.all.platform.CommonProto.AtUser;
import net.coding.events.all.platform.CommonProto.Operator;
import net.coding.events.all.platform.CommonProto.Program;
import net.coding.events.all.platform.CommonProto.Team;
import net.coding.events.all.platform.ProgramProto.ProgramCreatedEvent;
import net.coding.events.all.platform.ProjectEventProto.ProjectCreatedEvent;
import net.coding.events.all.platform.ProjectNoticeEventProto.ProjectNoticeCreatedEvent;
import net.coding.events.all.platform.ProjectNoticeEventProto.ProjectNoticeUpdatedEvent;
import net.coding.grpc.client.activity.ActivityGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.ActivityEnums;
import net.coding.lib.project.enums.ProgramProjectEventEnums;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.metrics.ProjectCreateMetrics;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectPreferenceService;
import net.coding.lib.project.setting.ProjectSetting;
import net.coding.lib.project.utils.DateUtil;
import net.coding.lib.project.utils.ResourceUtil;
import net.coding.lib.project.utils.TextUtil;
import net.coding.platform.degradation.annotation.Degradation;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.notification.NotificationProto;
import proto.notification.NotificationProto.TargetType;
import proto.platform.logging.loggingProto;
import proto.platform.project.ProjectProto;

import static net.coding.common.constants.ProjectConstants.ACTION_CREATE;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_STATUS_TRUE;
import static net.coding.lib.project.entity.ProjectPreference.PREFERENCE_TYPE_PROJECT_TWEET;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.createProgram;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.createProject;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectServiceHelper {

    private final UserGrpcClient userGrpcClient;

    private final ProjectPreferenceService projectPreferenceService;

    private final ProjectGrpcClient projectGrpcClient;

    private final AsyncEventBus asyncEventBus;

    private final TeamGrpcClient teamGrpcClient;

    private final ActivityGrpcClient activityGrpcClient;

    private final NotificationGrpcClient notificationGrpcClient;

    private final LoggingGrpcClient loggingGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final AsyncExternalEventBus asyncExternalEventBus;
    private final ProjectMemberService projectMemberService;
    /**
     * 通知项目内所有人创建了一条冒泡，创建者自身及被 @ 的人除外
     */
    @Degradation
    public void notifyMembers(List<Integer> userIds, Integer userId, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String projectHtmlUrl = projectHtmlLink(project.getId());
                String message = ResourceUtil.ui("notification_create_project_tweet",
                        userLink, projectHtmlUrl, this.getTweetHtmlLink(tweet, project, projectPath));

                // 站内通知
                notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                        .addAllUserId(userIds)
                        .setContent(StringUtils.defaultIfBlank(message, EMPTY))
                        .setTargetType(TargetType.ProjectTweet)
                        .setTargetId(tweet.getId().toString())
                        .setSetting(NotificationProto.Setting.AtSetting)
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
    @Degradation
    public void notifyUpdateMembers(List<Integer> userIds, Integer userId, Project project, ProjectTweet tweet) {
        // 检查偏好设置中的开关是否开启
        ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(project.getId(), PREFERENCE_TYPE_PROJECT_TWEET);
        Short preferenceStatus = projectPreference != null ? projectPreference.getStatus() : null;
        if (Objects.equals(preferenceStatus, PREFERENCE_STATUS_TRUE)) {
            if (userIds.size() > 0) {
                String userLink = userGrpcClient.getUserHtmlLinkById(userId);
                String projectPath = projectGrpcClient.getProjectPath(project.getId());
                String projectHtmlUrl = projectHtmlLink(project.getId());
                String message = ResourceUtil.ui("notification_update_project_notice",
                        userLink, projectHtmlUrl, this.getTweetHtmlLink(tweet, project, projectPath));

                notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                        .addAllUserId(userIds)
                        .setContent(StringUtils.defaultIfBlank(message, EMPTY))
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
            return EMPTY;
        }

        if (project == null) {
            return EMPTY;
        }

        return projectPath + "/setting/notice/" + projectTweet.getId();
    }

    /**
     * 处理content中的@通知 user: 发送者
     */
    @Degradation
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
                    EMPTY);
        }

        // 站内通知
        notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                .addAllUserId(userIds)
                .setContent(StringUtils.defaultIfBlank(notification, EMPTY))
                .setTargetType(NotificationProto.TargetType.ProjectTweet)
                .setTargetId(tweet.getId().toString())
                .setSetting(NotificationProto.Setting.AtSetting)
                .build());
        // TODO: 等移动端做了项目内冒泡后加上，且推送格式要修改
        // task: https://coding.net/u/wzw/p/coding/task/74923
    }

    @Degradation
    public void postProjectTweetCreateActivity(Project project, ProjectTweet tweet, Integer userId, ActivityEnums activityEnums, Short action, String actionStr) {
        try {
            Set<Integer> atUsers = projectMemberService.parseAtUser(userId, project, tweet.getContent(), tweet.getOwnerId());
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
            if (activityEnums == ActivityEnums.ACTION_TWEET_CREATE) {
                asyncExternalEventBus.post(ProjectNoticeCreatedEvent.newBuilder()
                                .setOperator(Operator.newBuilder()
                                        .setId(userId)
                                        .setLocale(localeMessageSource.getLocale().toString())
                                        .build())
                                .setContent(tweet.getContent())
                                .setProject(CommonProto.Project.newBuilder()
                                        .setId(project.getId())
                                        .setName(project.getName())
                                        .setDisplayName(project.getDisplayName())
                                        .build())
                                .setTeam(Team.newBuilder()
                                        .setId(project.getTeamOwnerId())
                                        .build())
                                .addAllAtUser(atUsers.stream()
                                        .map(uid->AtUser.newBuilder()
                                                .setId(uid)
                                                .build())
                                        .collect(Collectors.toSet()))
                        .build());
            }
            if (activityEnums == ActivityEnums.ACTION_TWEET_UPDATE) {
                asyncExternalEventBus.post(ProjectNoticeUpdatedEvent.newBuilder()
                        .setOperator(Operator.newBuilder()
                                .setId(userId)
                                .setLocale(localeMessageSource.getLocale().toString())
                                .build())
                        .setContent(tweet.getContent())
                        .setProject(CommonProto.Project.newBuilder()
                                .setId(project.getId())
                                .setName(project.getName())
                                .setDisplayName(project.getDisplayName())
                                .build())
                        .setTeam(Team.newBuilder()
                                .setId(project.getTeamOwnerId())
                                .build())
                        .addAllAtUser(atUsers.stream()
                                .map(uid->AtUser.newBuilder()
                                        .setId(uid)
                                        .build())
                                .collect(Collectors.toSet()))
                        .build());
            }
        } catch (Exception ex) {
            log.error("Send activity failed！ex={}", ex.getMessage());
        }
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

    public void postProjectCreateEvent(Project project,
                                       ProjectCreateParameter parameter,
                                       Credential credential) {
        Map<String, String> initMap = new HashMap<>();
        initMap.put("createSvnLayout", parameter.getCreateSvnLayout());
        initMap.put("credential", Optional.ofNullable(credential)
                .map(Credential::getCredentialId).orElse(""));
        initMap.put("credentialType", Optional.ofNullable(credential)
                .map(Credential::getType).orElse(""));
        Boolean gitEnabled = parameter.getGitEnabled();
        if (gitEnabled) {
            initMap.put("readme", parameter.getGitReadmeEnabled());
            initMap.put("gitignore", parameter.getGitIgnore());
            initMap.put("license", parameter.getGitLicense());
            initMap.put("template", Optional.ofNullable(parameter.getTemplate()).orElse(EMPTY));
        } else {
            initMap.put("readme", "no");
            initMap.put("gitignore", "no");
            initMap.put("license", "no");
        }

        // ProjectCreateEvent 的触发要放到添加完成员后，否则创建团队项目时用户会无权限添加 README
        long prevTime = System.currentTimeMillis();
        asyncEventBus.post(
                ProjectCreateEvent.builder()
                        .projectId(project.getId())
                        .fork(false)
                        .parentId(0)
                        .rootId(0)
                        .initMap(initMap)
                        .quota(100 * 1024)
                        .userId(parameter.getUserId())
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

            insertOperationLog(parameter.getUserId(), parameter.getTeamId(), project, createProject);
        }

        asyncEventBus.post(CreateProjectUserEvent.builder()
                .userId(parameter.getUserId())
                .teamId(parameter.getTeamId())
                .projectId(project.getId())
                .build()
        );
    }

    @Degradation
    public void sendCreateProjectNotification(Integer teamId, Integer ownerId, Integer userId, Project project,
                                              ProgramProjectEventEnums eventEnums) {
        if (eventEnums.equals(createProgram)) {
            asyncExternalEventBus.post(ProgramCreatedEvent.newBuilder()
                    .setTeam(Team.newBuilder()
                            .setId(teamId)
                            .build())
                    .setOperator(Operator.newBuilder()
                            .setId(userId)
                            .setLocale(localeMessageSource.getLocale().toString())
                            .build())
                    .setProgram(Program.newBuilder()
                            .setId(project.getId())
                            .build())
                    .build());
        }
        if (eventEnums.equals(createProject)) {
            asyncExternalEventBus.post(ProjectCreatedEvent.newBuilder()
                    .setTeam(Team.newBuilder()
                            .setId(teamId)
                            .build())
                    .setOperator(Operator.newBuilder()
                            .setId(userId)
                            .setLocale(localeMessageSource.getLocale().toString())
                            .build())
                    .setProject(CommonProto.Project.newBuilder()
                            .setId(project.getId())
                            .build())
                    .build());
        }

    }

    public void insertOperationLog(Integer userId, Integer teamId, Project project,
                                   ProgramProjectEventEnums eventEnums) {
        loggingGrpcClient.insertOperationLog(loggingProto.OperationLogInsertRequest.newBuilder()
                .setUserId(userId)
                .setTeamId(teamId)
                .setContentName(eventEnums.name())
                .setTargetId(project.getId())
                .setTargetType(project.getClass().getSimpleName())
                .setAdminAction(false)
                .setText(localeMessageSource.getMessage(eventEnums.getMessage(),
                        new Object[]{EMPTY
                                , htmlLink(project)}).trim())
                .build()
        );
    }
}
