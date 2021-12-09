package net.coding.lib.project.service.member;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.StringUtils;
import net.coding.e.grpcClient.collaboration.ProgramIssueRelationGrpcClient;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.dao.ProgramProjectDao;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.request.ProjectMemberReqDTO;
import net.coding.lib.project.entity.ProgramProject;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProgramProjectRoleTypeEnum;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.platform.ram.pojo.dto.GrantDTO;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Service
@Slf4j
@AllArgsConstructor
public class ProgramMemberPrincipalService {

    private final ProjectDao projectDao;

    private final ProgramDao programDao;

    private final ProjectMemberDao projectMemberDao;

    private final ProgramProjectDao programProjectDao;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProgramIssueRelationGrpcClient programIssueRelationGrpcClient;

    /**
     * 项目删除
     * <p>
     * 成员要求 项目集项目普通成员且成员只在要删除的项目下存在，其他项目不存在时则可删
     */
    @Transactional
    public void removeProgramProjects(Integer teamId, Integer userId, Project project) {
        List<ProjectMember> projectMembers = projectMemberDao.findListByProjectId(project.getId(), BeanUtils.getDefaultDeletedAt());
        programDao.selectPrograms(ProgramQueryParameter.builder()
                .teamId(teamId)
                .projectId(project.getId())
                .build()
        ).forEach(program -> {
            try {
                removeProgramProjectMember(teamId, userId, program, project, projectMembers);
            } catch (CoreException e) {
                log.info("removeProgramProjects Error, programId {} ", program.getId());
            }
        });
    }

    /**
     * 移除项目集中项目时
     */
    @Transactional
    public void removeProgramProject(Integer teamId, Integer operatorId, Integer programId, Integer projectId) throws CoreException {
        Project program = programDao.selectByIdAndTeamId(programId, teamId);
        if (Objects.isNull(program)) {
            log.info("RemoveProgramProject Error, program is null programId  = {}", programId);
            return;
        }
        Project project = projectDao.getProjectNotDeleteByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            log.info("RemoveProgramProject Error, project is null projectId  = {}", projectId);
            return;
        }
        ProgramProject programProject = programProjectDao.selectOne(ProgramProject.builder()
                .programId(program.getId())
                .projectId(project.getId())
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build());
        if (Objects.isNull(programProject)) {
            log.info("RemoveProgramProject Error, programProject is null program  = {} , projectId  = {}",
                    program.getId(), project.getId());
            return;
        }
        List<ProjectMember> projectMembers = projectMemberDao.findListByProjectId(project.getId(),
                new Timestamp(project.getDeletedAt().getTime()));

        removeProgramProjectMember(teamId, operatorId, program, project, projectMembers);
        removeProgramIssueRelation(program.getId(), project.getId());
    }

    public void removeProgramProjectMember(Integer teamId,
                                           Integer operatorId,
                                           Project program,
                                           Project project,
                                           List<ProjectMember> members) throws CoreException {
        delMember(teamId, operatorId, program, project, members);
        Optional.ofNullable(
                programProjectDao.selectOne(ProgramProject.builder()
                        .programId(program.getId())
                        .projectId(project.getId())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build()))
                .ifPresent(pp -> {
                    pp.setDeletedAt(new Timestamp(System.currentTimeMillis()));
                    programProjectDao.updateByPrimaryKeySelective(pp);
                });
    }

    @Transactional
    public void delMember(Integer teamId,
                          Integer operatorId,
                          Project program,
                          Project project,
                          List<ProjectMember> members) throws CoreException {
        //项目集下其他项目的成员
        Set<String> otherMembers = StreamEx.of(
                programDao.selectProgramProjects(ProgramProjectQueryParameter.builder()
                        .teamId(teamId)
                        .programId(program.getId())
                        .build()))
                .filter(p -> !p.getId().equals(project.getId()))
                .flatMap(p -> projectMemberDao.findListByProjectId(p.getId(),
                        new Timestamp(p.getDeletedAt().getTime())).stream())
                .map(member -> StringUtils.join(member.getPrincipalId(), member.getPrincipalType()))
                .toSet();

        // 可删的项目成员
        Set<String> deleteMembers = StreamEx.of(members)
                .map(member -> StringUtils.join(member.getPrincipalId(), member.getPrincipalType()))
                .filter(grantMember -> !otherMembers.contains(grantMember))
                .toSet();

        // 项目集下可删成员
        List<ProjectMember> programMembers = projectMemberDao.findListByProjectId(program.getId(), BeanUtils.getDefaultDeletedAt())
                .stream()
                .filter(member -> deleteMembers.contains(StringUtils.join(member.getPrincipalId(), member.getPrincipalType())))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(programMembers)) {
            return;
        }
        PolicyResponseDTO policy = projectMemberInspectService.getPolicyByName(operatorId, ProgramProjectRoleTypeEnum.ProgramRoleTypeEnum.ProgramProjectMember.name());
        projectMemberInspectService.getResourceGrantPolicies(operatorId, program, programMembers, new HashSet<>())
                .forEach((key, value) -> {
                    StreamEx.of(value)
                            .filter(dto -> dto.getPolicyId().equals(policy.getPolicyId()))
                            .findFirst()
                            .map(dto -> new GrantDTO()
                                    .setGrantScope(key.getGrantScope())
                                    .setGrantObjectId(key.getGrantObjectId())
                                    .setPolicyId(dto.getPolicyId())
                                    .setResourceType(key.getResourceType())
                                    .setResourceId(key.getResourceId())
                            )
                            .ifPresent(dto -> projectMemberInspectService.detachGrant(operatorId, StreamEx.of(dto).toSet()));
                    boolean exist = StreamEx.of(value)
                            .anyMatch(dto -> !dto.getPolicyId().equals(policy.getPolicyId()));
                    if (!exist) {
                        try {
                            //删除成员
                            projectMemberPrincipalWriteService.delMember(
                                    teamId,
                                    operatorId,
                                    program.getId(),
                                    StreamEx.of(ProjectMemberReqDTO.builder()
                                            .principalId(key.getGrantObjectId())
                                            .principalType(ProjectMemberPrincipalTypeEnum.valueOf(key.getGrantScope()))
                                            .build()
                                    )
                                            .toList()
                            );
                        } catch (CoreException e) {
                            log.error("program delMember error");
                        }
                    }
                });
    }

    public void removeProgramIssueRelation(Integer programId, Integer projectId) {
        try {
            programIssueRelationGrpcClient.removeProgramIssueRelation(programId, projectId);
        } catch (Exception ex) {
            log.error("removeProgramIssueRelation Error , programId = {}, projectId = {}", programId, projectId, ex);
        }
    }
}
