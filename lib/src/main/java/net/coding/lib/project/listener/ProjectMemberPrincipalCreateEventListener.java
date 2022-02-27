package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.gson.JSON;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProgramProjectRoleTypeEnum.ProgramRoleTypeEnum;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.enums.RoleType;
import net.coding.lib.project.event.Principal;
import net.coding.lib.project.event.ProjectMemberPrincipalCreateEvent;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.RamTransformTeamService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalWriteService;
import net.coding.platform.ram.pojo.dto.GrantDTO;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMemberPrincipalCreateEventListener {
    private final ProjectService projectService;

    private final ProgramDao programDao;

    private final ProjectMemberService projectMemberService;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    private final RamTransformTeamService ramTransformTeamService;

    @Subscribe
    public void handle(ProjectMemberPrincipalCreateEvent event) {
        try {
            log.info("ProjectMemberPrincipalCreateEvent , event :{}", JSON.toJson(event));
            Project project = projectService.getById(event.getProjectId());
            if (Objects.isNull(project)) {
                log.info("ProjectMemberPrincipalCreateEvent Project is null, projectId = {}", event.getProjectId());
                return;
            }
            if (ramTransformTeamService.ramOnline(event.getTeamId())) {
                addMemberTransferAfter(event.getOperatorId(), project, event.getPrincipals());
            } else {
                addMemberTransferBefore(event.getOperatorId(), project, event.getPrincipals());
            }
        } catch (Exception ex) {
            log.error("ProjectMemberPrincipalCreateEvent Error, projectId = {}, operatorId = {}",
                    event.getProjectId(),
                    event.getOperatorId());
        }
    }

    public void addMemberTransferAfter(Integer operatorId, Project project, List<Principal> principals) {
        PolicyResponseDTO policyDTO = projectMemberInspectService.getPolicyByName(operatorId, RoleType.ProgramProjectMember.name());
        List<GrantDTO> grantInfoDTOS = new ArrayList<>();
        List<Project> programs = programDao.selectPrograms(ProgramQueryParameter.builder()
                .teamId(project.getTeamOwnerId())
                .projectId(project.getId())
                .build());
        programs.forEach(program -> {
            try {
                List<ProjectMemberAddReqDTO> reqDTOS = StreamEx.of(principals)
                        .map(principal -> {
                            grantInfoDTOS.add(new GrantDTO()
                                    .setGrantScope(principal.getPrincipalType().toLowerCase())
                                    .setGrantObjectId(principal.getPrincipalId())
                                    .setPolicyId(policyDTO.getPolicyId())
                                    .setResourceType(PmTypeEnums.of(program.getPmType()).name().toLowerCase())
                                    .setResourceId(String.valueOf(program.getId())));
                            return ProjectMemberAddReqDTO.builder()
                                    .principalType(ProjectMemberPrincipalTypeEnum.valueOf(principal.getPrincipalType()))
                                    .principalId(principal.getPrincipalId())
                                    .policyIds(StreamEx.of(policyDTO)
                                            .map(PolicyResponseDTO::getPolicyId)
                                            .toSet())
                                    .build();
                        })
                        .toList();
                //1、不存在的授权体添加并设置权限
                projectMemberPrincipalWriteService.addMember(
                        project.getTeamOwnerId(),
                        operatorId,
                        program.getId(),
                        reqDTOS
                );
            } catch (CoreException e) {
                log.error("ProjectMemberPrincipalCreateEvent addManyMemberToProject Error, projectId = {}, operatorId = {}",
                        project.getId(),
                        operatorId);
            }
        });
        //2、给授权体设置权限包含已存在的
        projectMemberInspectService.attachGrant(operatorId, grantInfoDTOS);
    }

    public void addMemberTransferBefore(Integer operatorId, Project project, List<Principal> principals) {
        programDao.selectPrograms(ProgramQueryParameter.builder()
                .teamId(project.getTeamOwnerId())
                .projectId(project.getId())
                .build()
        ).forEach(program -> {
            try {
                List<Integer> targetUserIds = StreamEx.of(principals)
                        .filter(principal -> ProjectMemberPrincipalTypeEnum.USER.name().equals(principal.getPrincipalType()))
                        .map(Principal::getPrincipalId)
                        .map(Integer::valueOf)
                        .toList();
                projectMemberService.doAddMember(
                        operatorId,
                        targetUserIds,
                        ProgramRoleTypeEnum.ProgramProjectMember.getCode(),
                        program,
                        false);
            } catch (CoreException e) {
                log.info("ProjectMemberCreateEvent doAddMember Error, projectId = {}, userId = {}",
                        project,
                        operatorId);
            }
        });
    }
}
