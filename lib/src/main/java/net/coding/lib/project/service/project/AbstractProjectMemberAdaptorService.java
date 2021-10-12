package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.event.ProjectMemberCreateEvent;
import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.base.event.ProjectMemberRoleChangeEvent;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.utils.ResourceUtil;
import net.coding.platform.permission.proto.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.acl.AclProto;
import proto.notification.NotificationProto;
import proto.platform.team.TeamProto;

import static org.apache.commons.lang3.StringUtils.EMPTY;


@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractProjectMemberAdaptorService {

    protected final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    protected final UserGrpcClient userGrpcClient;

    protected final TeamGrpcClient teamGrpcClient;

    protected final NotificationGrpcClient notificationGrpcClient;

    protected final AsyncEventBus asyncEventBus;

    public abstract Integer pmType();

    protected abstract String notificationAddMember();

    protected abstract String notificationInviteMember();

    protected abstract String notificationMemberQuit();

    protected abstract String notificationDeleteMember();

    public abstract void checkAddProjectMemberType(ProjectMember member) throws CoreException;

    public abstract void checkDelProjectMemberType(ProjectMember member, Short currentMemberType) throws CoreException;

    public abstract void checkExistProjectMember(Set<Integer> memberUserIds, List<Integer> targetUserIds,
                                                 Project project, short type) throws Exception;

    public abstract void checkProjectMemberRoleType(Integer teamId, Integer programId, Integer targetUserId) throws CoreException;

    public abstract AclProto.Role assignUsersToRoleByRoleType(Project project,
                                                              Set<Integer> targetUserIds,
                                                              short type) throws Exception;

    public void postAddMembersEvent(AtomicInteger insertRole, Integer operationUserId,
                                    Project project, ProjectMember projectMember,
                                    Integer userId, boolean isInvite) {
        postProjectMemberCreateEvent(project.getId(), projectMember);

        postActivityEvent(operationUserId, project.getId(),
                projectMember, ProjectMember.ACTION_ADD_MEMBER);

        if (insertRole.intValue() > 0) {
            postProjectMemberRoleChangeEvent(project.getId(), insertRole, projectMember);
        }
        String userLink = userGrpcClient.getUserHtmlLinkById(operationUserId);
        String projectHtmlUrl = projectHtmlLink(project);
        String message = ResourceUtil.ui(notificationAddMember(), userLink, projectHtmlUrl);
        String inviteMessage = ResourceUtil.ui(notificationInviteMember(), userLink, projectHtmlUrl);
        if (!userId.equals(operationUserId)) {
            // 站内通知
            List<Integer> userIds = new ArrayList<>();
            userIds.add(userId);
            sentProjectMemberNotification(userIds, message, project.getId());
            if (isInvite) {
                sentProjectMemberNotification(userIds, inviteMessage, project.getId());
            }
        }
    }

    public void postDeleteMemberEvent(Integer currentUserId, Project project, ProjectMember projectMember) {
        postProjectMemberDeleteEvent(project.getId(), projectMember);

        postActivityEvent(currentUserId, project.getId(),
                projectMember, ProjectMember.ACTION_REMOVE_MEMBER);

        List<Integer> userIds = new ArrayList<>();
        userIds.add(projectMember.getUserId());
        String userLink = userGrpcClient.getUserHtmlLinkById(currentUserId);
        String projectHtmlUrl = projectHtmlLink(project);
        String message = ResourceUtil.ui(notificationDeleteMember(),
                userLink, projectHtmlUrl);
        sentProjectMemberNotification(userIds, message, project.getId());
    }

    public void postMemberQuitEvent(Project project, ProjectMember projectMember) {
        postProjectMemberDeleteEvent(project.getId(), projectMember);

        postActivityEvent(projectMember.getUserId(), project.getId(),
                projectMember, ProjectMember.ACTION_QUIT);

        TeamProto.Team team = teamGrpcClient.getTeam(project.getId()).getData();
        List<Integer> userIds = new ArrayList<>();
        userIds.add(team.getOwner().getId());
        String userLink = userGrpcClient.getUserHtmlLinkById(projectMember.getUserId());
        String projectHtmlUrl = projectHtmlLink(project);
        String message = ResourceUtil.ui(notificationMemberQuit(), userLink, projectHtmlUrl);
        sentProjectMemberNotification(userIds, message, project.getId());
    }

    private void sentProjectMemberNotification(List<Integer> userIds, String message, Integer projectId) {
        notificationGrpcClient.send(NotificationProto.NotificationSendRequest.newBuilder()
                .addAllUserId(userIds)
                .setContent(message)
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

    private void postActivityEvent(Integer userId, Integer projectId,
                                   ProjectMember projectMember, Short action) {
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.e.lib.core.bean.ProjectMember.class)
                        .targetId(projectMember.getId())
                        .projectId(projectId)
                        .action(action)
                        .content(EMPTY)
                        .build()
        );
    }

    private void postProjectMemberCreateEvent(Integer projectId, ProjectMember projectMember) {
        asyncEventBus.post(
                ProjectMemberCreateEvent.builder()
                        .projectId(projectId)
                        .userId(projectMember.getUserId())
                        .build()
        );
    }

    private void postProjectMemberRoleChangeEvent(Integer projectId, AtomicInteger insertRole, ProjectMember projectMember) {
        asyncEventBus.post(
                ProjectMemberRoleChangeEvent.builder()
                        .projectId(projectId)
                        .roleId(insertRole.intValue())
                        .targetUserId(projectMember.getUserId())
                        .operate(1)
                        .build()
        );
    }

    private void postProjectMemberDeleteEvent(Integer projectId, ProjectMember projectMember) {
        asyncEventBus.post(
                ProjectMemberDeleteEvent.builder()
                        .projectId(projectId)
                        .userId(projectMember.getUserId())
                        .build()
        );
    }

    protected AclProto.Role assignUsersToRoleByRoleType(Project project, Set<Long> targetUserIds,
                                                        CommonProto.TargetType targetType,
                                                        CommonProto.RoleTypeEnum.RoleType roleType) throws Exception {
        return advancedRoleServiceGrpcClient.assignUsersToRoleByRoleType(
                project.getTeamOwnerId(), targetType, project.getId(), roleType, targetUserIds);
    }

    private String projectHtmlLink(Project project) {
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

}
