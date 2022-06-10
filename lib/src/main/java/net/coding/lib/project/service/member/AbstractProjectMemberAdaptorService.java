package net.coding.lib.project.service.member;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.event.ProjectMemberCreateEvent;
import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.base.event.ProjectMemberRoleChangeEvent;
import net.coding.common.eventbus.AsyncExternalEventBus;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.events.all.platform.CommonProto.Operator;
import net.coding.events.all.platform.CommonProto.Program;
import net.coding.events.all.platform.CommonProto.ProgramMember;
import net.coding.events.all.platform.CommonProto.Team;
import net.coding.events.all.platform.ProgramMemberProto.ProgramMemberDeletedEvent;
import net.coding.events.all.platform.ProjectMemberProto.ProjectMemberDeletedEvent;
import net.coding.events.all.platform.ProjectMemberProto.ProjectMemberQuitEvent;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.hook.trigger.CreateMemberEventTriggerTrigger;
import net.coding.lib.project.hook.trigger.DeleteMemberEventTriggerTrigger;
import net.coding.lib.project.utils.ResourceUtil;
import net.coding.platform.permission.proto.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.notification.NotificationProto;
import proto.platform.team.TeamProto;

import static org.apache.commons.lang3.StringUtils.EMPTY;


@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractProjectMemberAdaptorService {

    protected final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    protected final ProjectMemberInspectService projectMemberInspectService;

    protected final UserGrpcClient userGrpcClient;

    protected final TeamGrpcClient teamGrpcClient;

    protected final NotificationGrpcClient notificationGrpcClient;

    protected final CreateMemberEventTriggerTrigger createMemberEventTrigger;

    protected final DeleteMemberEventTriggerTrigger deleteMemberEventTrigger;

    protected final AsyncEventBus asyncEventBus;

    protected AsyncExternalEventBus asyncExternalEventBus;
    protected LocaleMessageSource localeMessageSource;

    public abstract Integer pmType();


    protected abstract String notificationInviteMember();


    protected abstract void postProjectMemberPrincipalCreateEvent(Project project,
                                                                  Integer operationUserId,
                                                                  List<ProjectMember> members);

    protected abstract void postProjectMemberPrincipalDeleteEvent(Project project,
                                                                  Integer operationUserId,
                                                                  List<ProjectMember> members);

    public abstract void checkAddProjectMemberType(ProjectMember member) throws CoreException;

    public abstract void checkDelProjectMemberType(ProjectMember member, Short currentMemberType) throws CoreException;

    public abstract void checkExistProjectMember(Set<Integer> memberUserIds, List<Integer> targetUserIds,
                                                 Project project, short type) throws Exception;

    public abstract void checkProjectMemberRoleType(Integer teamId, Integer programId, Integer targetUserId) throws CoreException;

    public abstract List<ProjectMember> filterProjectMemberRoleType(Integer currentUserId, Project project, List<ProjectMember> members);

    public abstract AclProto.Role assignUsersToRoleByRoleType(Project project,
                                                              Set<Integer> targetUserIds,
                                                              short type) throws Exception;

    @Autowired
    public void setAsyncExternalEventBus(AsyncExternalEventBus asyncExternalEventBus) {
        this.asyncExternalEventBus = asyncExternalEventBus;
    }
    @Autowired
    public void setLocaleMessageSource(LocaleMessageSource localeMessageSource) {
        this.localeMessageSource = localeMessageSource;
    }
    @Async
    public void postAddMembersEvent(AtomicInteger insertRole, Integer operationUserId,
                                    Project project, ProjectMember projectMember,
                                    Integer userId, boolean isInvite) {
        postProjectMemberPrincipalCreateEvent(
                project,
                operationUserId,
                StreamEx.of(projectMember)
                        .peek(member -> {
                            member.setPrincipalType(ProjectMemberPrincipalTypeEnum.USER.name());
                            member.setPrincipalId(String.valueOf(member.getUserId()));
                        }).toList()
        );

        postProjectMemberCreateEvent(project.getId(), projectMember.getUserId());

        postActivityEvent(
                operationUserId,
                project.getId(),
                projectMember.getUserId(),
                ProjectMember.ACTION_ADD_MEMBER
        );

        if (insertRole.intValue() > 0) {
            postProjectMemberRoleChangeEvent(project.getId(), insertRole.intValue(), projectMember.getUserId());
        }
        String userLink = userGrpcClient.getUserHtmlLinkById(operationUserId);
        String projectHtmlUrl = projectHtmlLink(project);
        String inviteMessage = ResourceUtil.ui(notificationInviteMember(), userLink, projectHtmlUrl);
        if (!userId.equals(operationUserId)) {
            // 站内通知
            List<Integer> userIds = new ArrayList<>();
            userIds.add(userId);
            if (isInvite) {
                sentProjectMemberNotification(userIds, inviteMessage, project.getId());
            }
        }
    }

    @Async
    public void postAddMembersEvent(Project project,
                                    Integer operationUserId,
                                    List<ProjectMember> addMembers,
                                    List<ProjectMember> members) {
        postProjectMemberPrincipalCreateEvent(project, operationUserId, addMembers);
        Set<Integer> existUserIds = projectMemberInspectService.getPrincipalMemberUserIds(members);
        Set<Integer> userIds = projectMemberInspectService.getPrincipalMemberUserIds(addMembers);
        List<Integer> memberUserIds = StreamEx.of(userIds)
                .filter(userId -> !existUserIds.contains(userId))
                .distinct()
                .toList();
        //用户在项目内是否存在
        if (CollectionUtils.isEmpty(memberUserIds)) {
            return;
        }
        postAddMembersUserEvent(project, operationUserId, memberUserIds);
    }

    /**
     * 部门/用户组 成员添加事件时调用
     */
    @Async
    public void postAddMembersUserEvent(Project project,
                                        Integer operationUserId,
                                        List<Integer> userIds) {
        String userLink = userGrpcClient.getUserHtmlLinkById(operationUserId);
        String projectHtmlUrl = projectHtmlLink(project);
        StreamEx.of(userIds)
                .forEach(userId -> {
                    Set<Integer> roleIds = advancedRoleServiceGrpcClient.findUserRolesInProject(
                            userId,
                            project.getTeamOwnerId(),
                            project.getId()
                    )
                            .stream()
                            .map(AclProto.Role::getId)
                            .collect(Collectors.toSet());
                    if (CollectionUtils.isEmpty(roleIds)) {
                        roleIds = new HashSet<>(0);
                    }
                    postActivityEvent(
                            operationUserId,
                            project.getId(),
                            userId,
                            ProjectMember.ACTION_ADD_MEMBER
                    );
                    postProjectMemberCreateEvent(project.getId(), userId);
                    StreamEx.of(roleIds)
                            .forEach(roleId -> postProjectMemberRoleChangeEvent(project.getId(), roleId, userId));
                    createMemberEventTrigger.trigger(
                            StreamEx.of(roleIds).map(String::valueOf).toList(),
                            ProjectMember.builder().userId(userId).build(),
                            project,
                            operationUserId
                    );
                });
    }

    @Async
    public void postDeleteMemberEvent(Integer currentUserId, Project project, ProjectMember projectMember) {
        postProjectMemberPrincipalDeleteEvent(
                project,
                currentUserId,
                StreamEx.of(projectMember)
                        .peek(member -> {
                            member.setPrincipalType(ProjectMemberPrincipalTypeEnum.USER.name());
                            member.setPrincipalId(String.valueOf(member.getUserId()));
                        }).toList()
        );
        postProjectMemberDeleteEvent(project.getId(), projectMember.getUserId());
        postActivityEvent(
                currentUserId,
                project.getId(),
                projectMember.getUserId(),
                ProjectMember.ACTION_REMOVE_MEMBER
        );
    }

    @Async
    public void postDeleteMemberEvent(Project project,
                                      Integer operationUserId,
                                      List<ProjectMember> delMembers) {
        postProjectMemberPrincipalDeleteEvent(project, operationUserId, delMembers);
        Set<Integer> delUserIds = projectMemberInspectService.getPrincipalMemberUserIds(delMembers);
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        Set<Integer> existUserIds = projectMemberInspectService.getPrincipalMemberUserIds(members);
        List<Integer> memberUserIds = StreamEx.of(delUserIds)
                .filter(userId -> !existUserIds.contains(userId))
                .distinct()
                .toList();
        //用户在项目内是否存在
        if (CollectionUtils.isEmpty(memberUserIds)) {
            return;
        }
        postDeleteMemberUserEvent(project, operationUserId, memberUserIds);
    }

    /**
     * 部门/用户组 删除事件时调用
     */
    @Async
    public void postDeleteMemberUserEvent(Project project,
                                          Integer operationUserId,
                                          List<Integer> userIds) {
        StreamEx.of(userIds)
                .forEach(userId -> {
                    postProjectMemberDeleteEvent(project.getId(), userId);
                    postActivityEvent(
                            operationUserId,
                            project.getId(),
                            userId,
                            ProjectMember.ACTION_REMOVE_MEMBER
                    );
                    deleteMemberEventTrigger.trigger(
                            ImmutableList.of(String.valueOf(0)),
                            ProjectMember.builder().userId(userId).build(),
                            project,
                            operationUserId);
                });
    }

    @Async
    public void postMemberQuitEvent(Integer currentUserId, Project project, ProjectMember projectMember) {
        postProjectMemberPrincipalDeleteEvent(
                project,
                currentUserId,
                StreamEx.of(projectMember)
                        .peek(member -> {
                            member.setPrincipalType(ProjectMemberPrincipalTypeEnum.USER.name());
                            member.setPrincipalId(String.valueOf(member.getUserId()));
                        }).toList()
        );
        postProjectMemberDeleteEvent(project.getId(), projectMember.getUserId());

        postActivityEvent(
                projectMember.getUserId(),
                project.getId(),
                projectMember.getUserId(),
                ProjectMember.ACTION_QUIT
        );
    }

    @Async
    public void postMemberQuitEvent(Integer currentUserId, Project project, List<ProjectMember> delMembers) {
        postProjectMemberPrincipalDeleteEvent(
                project,
                currentUserId,
                StreamEx.of(delMembers)
                        .peek(m -> {
                            m.setPrincipalType(ProjectMemberPrincipalTypeEnum.USER.name());
                            m.setPrincipalId(String.valueOf(m.getUserId()));
                        }).toList()
        );
        ProjectMember member = StreamEx.of(delMembers)
                .findFirst()
                .orElse(null);
        if (Objects.isNull(member)) {
            return;
        }
        TeamProto.Team team = teamGrpcClient.getTeam(project.getId()).getData();
        List<Integer> userIds = new ArrayList<>();
        userIds.add(team.getOwner().getId());
        StreamEx.of(userIds)
                .forEach(userId -> {
                    postProjectMemberDeleteEvent(project.getId(), userId);
                    deleteMemberEventTrigger.trigger(
                            ImmutableList.of(String.valueOf(0)),
                            ProjectMember.builder().userId(userId).build(),
                            project,
                            member.getUserId());
                });
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
                                   Integer targetUserId, Short action) {
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.e.lib.core.bean.ProjectMember.class)
                        .targetId(targetUserId)
                        .projectId(projectId)
                        .action(action)
                        .content(EMPTY)
                        .build()
        );
    }

    private void postProjectMemberCreateEvent(Integer projectId, Integer userId) {
        asyncEventBus.post(
                ProjectMemberCreateEvent.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .build()
        );
    }

    private void postProjectMemberRoleChangeEvent(Integer projectId, Integer roleId, Integer userId) {
        asyncEventBus.post(
                ProjectMemberRoleChangeEvent.builder()
                        .projectId(projectId)
                        .roleId(roleId)
                        .targetUserId(userId)
                        .operate(1)
                        .build()
        );
    }

    private void postProjectMemberDeleteEvent(Integer projectId, Integer userId) {
        asyncEventBus.post(
                ProjectMemberDeleteEvent.builder()
                        .projectId(projectId)
                        .userId(userId)
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
