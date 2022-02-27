package net.coding.lib.project.service.member;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProgramProjectRoleTypeEnum;
import net.coding.lib.project.event.Principal;
import net.coding.lib.project.event.ProjectMemberPrincipalCreateEvent;
import net.coding.lib.project.event.ProjectMemberPrincipalDeleteEvent;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.hook.trigger.CreateMemberEventTriggerTrigger;
import net.coding.lib.project.hook.trigger.DeleteMemberEventTriggerTrigger;
import net.coding.platform.permission.proto.CommonProto;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;


@Slf4j
@Service
public class ProjectMemberAdaptorService extends AbstractProjectMemberAdaptorService {

    public ProjectMemberAdaptorService(AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient, ProjectMemberInspectService projectMemberInspectService, UserGrpcClient userGrpcClient, TeamGrpcClient teamGrpcClient, NotificationGrpcClient notificationGrpcClient, CreateMemberEventTriggerTrigger createMemberEventTrigger, DeleteMemberEventTriggerTrigger deleteMemberEventTrigger, AsyncEventBus asyncEventBus) {
        super(advancedRoleServiceGrpcClient, projectMemberInspectService, userGrpcClient, teamGrpcClient, notificationGrpcClient, createMemberEventTrigger, deleteMemberEventTrigger, asyncEventBus);
    }

    @Override
    public Integer pmType() {
        return PmTypeEnums.PROJECT.getType();
    }

    @Override
    protected String notificationAddMember() {
        return "notification_add_member";
    }

    @Override
    protected String notificationInviteMember() {
        return "notification_invite_member";
    }

    @Override
    protected String notificationMemberQuit() {
        return "notification_member_quit";
    }

    @Override
    protected String notificationDeleteMember() {
        return "notification_delete_member";
    }

    @Override
    protected void postProjectMemberPrincipalCreateEvent(Project project, Integer operationUserId, List<ProjectMember> members) {
        asyncEventBus.post(ProjectMemberPrincipalCreateEvent.builder()
                .teamId(project.getTeamOwnerId())
                .projectId(project.getId())
                .operatorId(operationUserId)
                .principals(StreamEx.of(members)
                        .map(member -> Principal.builder()
                                .principalId(member.getPrincipalId())
                                .principalType(member.getPrincipalType())
                                .build())
                        .toList()
                ).build()
        );
    }

    @Override
    protected void postProjectMemberPrincipalDeleteEvent(Project project, Integer operationUserId, List<ProjectMember> members) {
        asyncEventBus.post(ProjectMemberPrincipalDeleteEvent.builder()
                .teamId(project.getTeamOwnerId())
                .projectId(project.getId())
                .operatorId(operationUserId)
                .principals(StreamEx.of(members)
                        .map(member ->
                                Principal.builder()
                                        .principalId(member.getPrincipalId())
                                        .principalType(member.getPrincipalType())
                                        .build())
                        .toList()
                ).build()
        );
    }

    @Override
    public void checkAddProjectMemberType(ProjectMember member) throws CoreException {
        //todo henry: member type
/*        if (Objects.isNull(member) || member.getType() <= RoleTypeEnum.PROJECT_MEMBER.getCode()) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }*/
    }

    @Override
    public void checkDelProjectMemberType(ProjectMember member, Short currentMemberType) throws CoreException {
        /*
         如果当前操作用户的权限低于被操作用户
         或者被操作用户的权限是ONWER,则权限不足
         */
        //todo henry: member type
 /*       if ((currentMemberType.compareTo(member.getType()) <= 0) || member.getType().equals(OWNER)) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }*/
    }

    @Override
    public void checkExistProjectMember(Set<Integer> memberUserIds, List<Integer> targetUserIds,
                                        Project project, short type) {
    }

    @Override
    public void checkProjectMemberRoleType(Integer teamId, Integer programId, Integer targetUserId) {

    }

    @Override
    public List<ProjectMember> filterProjectMemberRoleType(Integer currentUserId, Project project, List<ProjectMember> members) {
        return members;
    }

    @Override
    public AclProto.Role assignUsersToRoleByRoleType(Project project, Set<Integer> targetUserIds, short type) throws Exception {
        return assignUsersToRoleByRoleType(project,
                targetUserIds.stream().map(Integer::longValue).collect(Collectors.toSet()),
                CommonProto.TargetType.PROJECT,
                ProgramProjectRoleTypeEnum.ProjectRoleTypeEnum.of(type).getRoleType());
    }

}
