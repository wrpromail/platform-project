package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.gson.JSON;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.event.Principal;
import net.coding.lib.project.event.ProjectMemberPrincipalDeleteEvent;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.ProgramMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.RamTransformTeamService;
import net.coding.lib.project.service.member.ProgramMemberPrincipalService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMemberPrincipalDeleteEventListener {

    private final ProgramDao programDao;

    private final ProjectService projectService;

    private final ProgramMemberService programMemberService;

    private final RamTransformTeamService ramTransformTeamService;

    private final ProgramMemberPrincipalService programMemberPrincipalService;

    private final ProjectMemberInspectService projectMemberInspectService;

    @Subscribe
    public void handle(ProjectMemberPrincipalDeleteEvent event) {
        try {
            log.info("ProjectMemberPrincipalDeleteEvent , event :{}", JSON.toJson(event));
            Project project = projectService.getById(event.getProjectId());
            if (Objects.isNull(project)) {
                log.info("ProjectMemberPrincipalDeleteEvent Project is null, projectId = {}", event.getProjectId());
                return;
            }

            if (ramTransformTeamService.ramOnline(event.getTeamId())) {
                delMemberTransferAfter(event.getOperatorId(), project, event.getPrincipals());
            } else {
                delMemberTransferBefore(event.getOperatorId(), project, event.getPrincipals());
            }
        } catch (Exception ex) {
            log.error("ProjectMemberPrincipalDeleteEvent Exception Error, projectId = {}, targetMembers = {}",
                    event.getProjectId(),
                    event.getPrincipals());
        }
    }

    public void delMemberTransferAfter(Integer operatorId, Project project, List<Principal> principals) {
        List<ProjectMember> members = StreamEx.of(principals)
                .map(grant -> ProjectMember.builder()
                        .principalId(grant.getPrincipalId())
                        .principalType(grant.getPrincipalType())
                        .build())
                .toList();
        programDao.selectPrograms(ProgramQueryParameter.builder()
                .teamId(project.getTeamOwnerId())
                .projectId(project.getId())
                .build()
        ).forEach(program -> {
            try {
                programMemberPrincipalService.delMember(program.getTeamOwnerId(), operatorId, program, project, members);
            } catch (Exception e) {
                log.error("ProjectMemberPrincipalDeleteEvent CoreException Error, programId = {}, targetMembers = {}",
                        program.getId(),
                        members);
            }
        });
    }

    public void delMemberTransferBefore(Integer operatorId, Project project, List<Principal> principals) {
        StreamEx.of(principals)
                .filter(principal -> ProjectMemberPrincipalTypeEnum.USER.name().equals(principal.getPrincipalType()))
                .map(Principal::getPrincipalId)
                .map(Integer::valueOf)
                .forEach(userId ->
                        programDao.selectPrograms(ProgramQueryParameter.builder()
                                .teamId(project.getTeamOwnerId())
                                .projectId(project.getId())
                                .joinedProjectIds(projectMemberInspectService.getJoinedProjectIds(
                                        ProjectMemberPrincipalQueryParameter.builder()
                                                .teamId(project.getTeamOwnerId())
                                                .userId(userId)
                                                .pmType(PmTypeEnums.PROGRAM.getType())
                                                .deletedAt(BeanUtils.getDefaultDeletedAt())
                                                .build()))
                                .build()
                        ).forEach(program -> {
                            Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                                    ProjectMemberPrincipalQueryParameter.builder()
                                            .teamId(project.getTeamOwnerId())
                                            .userId(userId)
                                            .pmType(PmTypeEnums.PROJECT.getType())
                                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                                            .build());
                            if (CollectionUtils.isNotEmpty(joinedProjectIds)) {
                                //项目集下其他项目成员中所在项目
                                boolean isExist = StreamEx.of(programDao.selectProgramProjects(
                                        ProgramProjectQueryParameter.builder()
                                                .teamId(program.getTeamOwnerId())
                                                .programId(program.getId())
                                                .userId(userId)
                                                .joinedProjectIds(joinedProjectIds)
                                                .build()))
                                        .anyMatch(p -> !p.getId().equals(project.getId()));
                                if (isExist) {
                                    return;
                                }
                            }
                            programMemberService.delMember(program.getTeamOwnerId(), operatorId, program, userId);
                        }));
    }
}
