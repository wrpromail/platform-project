package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProgramProjectRoleTypeEnum;
import net.coding.lib.project.enums.RoleTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.platform.permission.proto.CommonProto;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import proto.acl.AclProto;

import static net.coding.common.constants.RoleConstants.OWNER;


@Slf4j
@Service
public class ProjectMemberAdaptorService extends AbstractProjectMemberAdaptorService {

    public ProjectMemberAdaptorService(AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient, UserGrpcClient userGrpcClient, TeamGrpcClient teamGrpcClient, NotificationGrpcClient notificationGrpcClient, AsyncEventBus asyncEventBus) {
        super(advancedRoleServiceGrpcClient, userGrpcClient, teamGrpcClient, notificationGrpcClient, asyncEventBus);
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
    public void checkAddProjectMemberType(ProjectMember member) throws CoreException {
        if (Objects.isNull(member) || member.getType() <= RoleTypeEnum.PROJECT_MEMBER.getCode()) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
    }

    @Override
    public void checkDelProjectMemberType(ProjectMember member, Short currentMemberType) throws CoreException {
        /*
         如果当前操作用户的权限低于被操作用户
         或者被操作用户的权限是ONWER,则权限不足
         */
        if ((currentMemberType.compareTo(member.getType()) <= 0) || member.getType().equals(OWNER)) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
    }

    @Override
    public void checkExistProjectMember(Set<Integer> memberUserIds, List<Integer> targetUserIds,
                                        Project project, short type) {
    }

    @Override
    public void checkProjectMemberRoleType(Integer teamId, Integer programId, Integer targetUserId) {

    }

    @Override
    public AclProto.Role assignUsersToRoleByRoleType(Project project, Set<Integer> targetUserIds, short type) throws Exception {
        return assignUsersToRoleByRoleType(project,
                targetUserIds.stream().map(Integer::longValue).collect(Collectors.toSet()),
                CommonProto.TargetType.PROJECT,
                ProgramProjectRoleTypeEnum.ProjectRoleTypeEnum.of(type).getRoleType());
    }

}
