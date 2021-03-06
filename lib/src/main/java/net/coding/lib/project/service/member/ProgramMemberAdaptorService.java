package net.coding.lib.project.service.member;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.eventbus.AsyncExternalEventBus;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.events.all.platform.CommonProto.Operator;
import net.coding.events.all.platform.CommonProto.Program;
import net.coding.events.all.platform.CommonProto.ProgramMember;
import net.coding.events.all.platform.CommonProto.Team;
import net.coding.events.all.platform.ProgramMemberProto.ProgramMemberCreatedEvent;
import net.coding.events.all.platform.ProgramMemberProto.ProgramMemberDeletedEvent;
import net.coding.events.all.platform.ProgramMemberProto.ProgramMemberQuitEvent;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.hook.trigger.CreateMemberEventTriggerTrigger;
import net.coding.lib.project.hook.trigger.DeleteMemberEventTriggerTrigger;
import net.coding.platform.permission.proto.CommonProto;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;

import org.apache.commons.lang3.StringUtils;
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

    private final ProjectMemberInspectService projectMemberInspectService;
    public ProgramMemberAdaptorService(AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient, ProjectMemberInspectService projectMemberInspectService, UserGrpcClient userGrpcClient, TeamGrpcClient teamGrpcClient, NotificationGrpcClient notificationGrpcClient, CreateMemberEventTriggerTrigger createMemberEventTrigger, DeleteMemberEventTriggerTrigger deleteMemberEventTrigger, AsyncEventBus asyncEventBus, ProjectMemberInspectService projectMemberInspectService1) {
        super(advancedRoleServiceGrpcClient, projectMemberInspectService, userGrpcClient, teamGrpcClient, notificationGrpcClient, createMemberEventTrigger, deleteMemberEventTrigger, asyncEventBus);
        this.projectMemberInspectService = projectMemberInspectService1;
    }


    @Override
    public Integer pmType() {
        return PmTypeEnums.PROGRAM.getType();
    }


    @Override
    protected String notificationInviteMember() {
        return "program_notification_invite_member";
    }



    @Override
    protected void postProjectMemberPrincipalCreateEvent(Project project, Integer operationUserId, List<ProjectMember> members) {
        StreamEx.of(members)
                .distinct(ProjectMember::getUserId)
                .forEach(member->{
                    asyncExternalEventBus.post(ProgramMemberCreatedEvent.newBuilder()
                            .setOperator(Operator.newBuilder()
                                    .setId(operationUserId)
                                    .setLocale(localeMessageSource.getLocale().toString())
                                    .build())
                            .setTeam(Team.newBuilder()
                                    .setId(project.getTeamOwnerId())
                                    .build())
                            .setProgram(Program.newBuilder()
                                    .setId(project.getId())
                                    .build())
                            .setMember(ProgramMember.newBuilder()
                                    .setId(member.getUserId())
                                    .build())
                            .build());
                });
    }

    @Override
    protected void postProjectMemberPrincipalDeleteEvent(Project project, Integer operationUserId, List<ProjectMember> members) {
        members.forEach(member->{
                    if (member.getUserId().equals(operationUserId)) {
                        asyncExternalEventBus.post(ProgramMemberQuitEvent.newBuilder()
                                .setOperator(Operator.newBuilder()
                                        .setId(operationUserId)
                                        .setLocale(localeMessageSource.getLocale().toString())
                                        .build())
                                .setTeam(Team.newBuilder()
                                        .setId(project.getTeamOwnerId())
                                        .build())
                                .setProgram(Program.newBuilder()
                                        .setId(project.getId())
                                        .build())
                                .build());
                    } else {
                        asyncExternalEventBus.post(ProgramMemberDeletedEvent.newBuilder()
                                .setOperator(Operator.newBuilder()
                                        .setId(operationUserId)
                                        .setLocale(localeMessageSource.getLocale().toString())
                                        .build())
                                .setTeam(Team.newBuilder()
                                        .setId(project.getTeamOwnerId())
                                        .build())
                                .setProgram(Program.newBuilder()
                                        .setId(project.getId())
                                        .build())
                                .setMember(ProgramMember.newBuilder()
                                        .setId(member.getUserId())
                                        .build())
                                .build());
                    }

        });

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
        //?????????????????????
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
            //??????????????????????????????????????????????????????
            boolean isExist = StreamEx.of(roles)
                    .anyMatch(role -> role.getType().equals(ProgramRoleTypeEnum.ProgramOwner.name())
                            || role.getType().equals(ProgramRoleTypeEnum.ProgramProjectMember.name()));
            if (isExist) {
                throw CoreException.of(PERMISSION_DENIED);
            }
        }
    }

    @Override
    public List<ProjectMember> filterProjectMemberRoleType(Integer operatorId, Project program, List<ProjectMember> members) {
        PolicyResponseDTO policyOwner = projectMemberInspectService.getPolicyByName(operatorId, ProgramRoleTypeEnum.ProgramOwner.name());
        PolicyResponseDTO policyMember = projectMemberInspectService.getPolicyByName(operatorId, ProgramRoleTypeEnum.ProgramProjectMember.name());
        Set<Long> policyIdScope = StreamEx.of(policyOwner.getPolicyId(), policyMember.getPolicyId()).toSet();
        Set<String> grants = projectMemberInspectService.getResourceGrantPolicies(operatorId, program, members, policyIdScope)
                .keySet()
                .stream()
                .map(policyResponseDTOS -> StringUtils.join(policyResponseDTOS.getGrantObjectId(), policyResponseDTOS.getGrantScope()))
                .collect(Collectors.toSet());
        return StreamEx.of(members)
                .filter(member -> !grants.contains(StringUtils.join(member.getPrincipalId(), member.getPrincipalType())))
                .nonNull()
                .toList();
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
