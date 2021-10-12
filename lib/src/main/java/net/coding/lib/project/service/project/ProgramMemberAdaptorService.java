package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.platform.permission.proto.CommonProto;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;

import static net.coding.lib.project.enums.ProgramProjectRoleTypeEnum.ProgramRoleTypeEnum;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;


@Slf4j
@Service
public class ProgramMemberAdaptorService extends AbstractProjectMemberAdaptorService {


    public ProgramMemberAdaptorService(AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient, UserGrpcClient userGrpcClient, TeamGrpcClient teamGrpcClient, NotificationGrpcClient notificationGrpcClient, AsyncEventBus asyncEventBus) {
        super(advancedRoleServiceGrpcClient, userGrpcClient, teamGrpcClient, notificationGrpcClient, asyncEventBus);
    }

    @Override
    public Integer pmType() {
        return PmTypeEnums.PROGRAM.getType();
    }

    @Override
    protected String notificationAddMember() {
        return "program_notification_add_member";
    }

    @Override
    protected String notificationInviteMember() {
        return "program_notification_invite_member";
    }

    @Override
    protected String notificationMemberQuit() {
        return "program_notification_member_quit";
    }

    @Override
    protected String notificationDeleteMember() {
        return "program_notification_delete_member";
    }

    @Override
    public void checkAddProjectMemberType(ProjectMember member) {
    }

    @Override
    public void checkDelProjectMemberType(ProjectMember member, Short currentMemberType) {
    }

    @Override
    public void checkExistProjectMember(Set<Integer> memberUserIds, List<Integer> targetUserIds,
                                        Project project, short type) throws Exception {
        //存在则设置权限
        Set<Integer> existUserId = StreamEx.of(targetUserIds)
                .filter(memberUserIds::contains)
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(existUserId)) {
            assignUsersToRoleByRoleType(project, existUserId, type);
        }
    }

    @Override
    public void checkProjectMemberRoleType(Integer teamId, Integer programId, Integer targetUserId) throws CoreException {
        List<AclProto.Role> roles =
                advancedRoleServiceGrpcClient.findUserRolesInProject(targetUserId, teamId, programId);
        if (!CollectionUtils.isEmpty(roles)) {
            //如果存在项目集负责人和项目集项目成员
            boolean isExist = StreamEx.of(roles)
                    .anyMatch(role -> role.getType().equals(ProgramRoleTypeEnum.ProgramOwner.name())
                            || role.getType().equals(ProgramRoleTypeEnum.ProgramProjectMember.name()));
            if (isExist) {
                throw CoreException.of(PERMISSION_DENIED);
            }
        }
    }

    @Override
    public AclProto.Role assignUsersToRoleByRoleType(Project project,
                                                     Set<Integer> targetUserIds,
                                                     short type) throws Exception {
        return assignUsersToRoleByRoleType(project,
                targetUserIds.stream().map(Integer::longValue).collect(Collectors.toSet()),
                CommonProto.TargetType.PROGRAM,
                ProgramRoleTypeEnum.of(type).getRoleType());
    }

}
