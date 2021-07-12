package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.e.grpcClient.collaboration.ProgramIssueRelationGrpcClient;
import net.coding.e.grpcClient.collaboration.exception.ProgramIssueRelationException;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.dao.ProgramProjectDao;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProgramProject;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;

import static net.coding.lib.project.enums.ProgramProjectRoleTypeEnum.*;

@Service
@Slf4j
@AllArgsConstructor
public class ProgramMemberService {
    private final ProgramDao programDao;

    private final ProjectMemberService projectMemberService;

    private final ProjectMemberDao projectMemberDao;

    private final ProgramProjectDao programProjectDao;

    private final ProjectDao projectDao;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final ProgramIssueRelationGrpcClient programIssueRelationGrpcClient;

    /**
     * 项目删除
     * <p>
     * 成员要求 项目集项目普通成员且成员只在要删除的项目下存在，其他项目不存在时则可删
     */
    public void removeProgramProjects(Integer teamId, Project project) {
        List<ProjectMember> projectMembers = projectMemberService.findListByProjectId(project.getId());
        programDao.selectPrograms(ProgramQueryParameter.builder()
                .teamId(teamId)
                .projectId(project.getId())
                .build()
        ).forEach(program -> removeProgramProjectMember(teamId, program, project, projectMembers));
    }

    /**
     * 移除项目集中项目时
     */
    public void removeProgramProject(Integer teamId, Integer programId, Integer projectId) {
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

        removeProgramProjectMember(teamId, program, project, projectMembers);
        removeProgramIssueRelation(program.getId(), project.getId());
    }

    public void removeProgramProjectMember(Integer teamId,
                                           Project program,
                                           Project project,
                                           List<ProjectMember> projectMembers) {
        //项目集下其他项目成员
        Set<Integer> otherMembers = StreamEx.of(
                programDao.selectProgramProjects(ProgramProjectQueryParameter.builder()
                        .teamId(teamId)
                        .programId(program.getId())
                        .build()))
                .filter(p -> !p.getId().equals(project.getId()))
                .flatMap(p -> projectMemberDao.findListByProjectId(p.getId(),
                        new Timestamp(p.getDeletedAt().getTime())).stream())
                .map(ProjectMember::getUserId)
                .toSet();

        // 可删的项目成员
        Set<Integer> deleteMembers = StreamEx.of(projectMembers)
                .filter(m -> !otherMembers.contains(m.getUserId()))
                .map(ProjectMember::getUserId)
                .toSet();

        // 项目集下可删成员
        List<Integer> programUserIds = projectMemberService.findListByProjectId(program.getId())
                .stream()
                .filter(m -> deleteMembers.contains(m.getUserId()))
                .map(ProjectMember::getUserId)
                .collect(Collectors.toList());

        programUserIds.forEach(targetUserId -> delMember(teamId, program, targetUserId));

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


    public void delMember(Integer teamId, Project program, Integer targetUserId) {
        List<AclProto.Role> roles =
                advancedRoleServiceGrpcClient.findUserRolesInProject(targetUserId, teamId, program.getId());
        //如果用户有多重角色 则删除项目成员角色
        if (!CollectionUtils.isEmpty(roles) && roles.size() > 1) {
            StreamEx.of(roles)
                    .filter(role -> role.getType().equals(ProgramRoleTypeEnum.ProgramProjectMember.name()))
                    .findFirst()
                    .ifPresent(role -> {
                        try {
                            advancedRoleServiceGrpcClient.removeRoleFromUser(role, targetUserId);
                        } catch (Exception e) {
                            log.error("RemoveRoleFromUser Error, roleType = {}, targetUserId = {}",
                                    role.getType(),
                                    targetUserId,
                                    e);
                        }
                    });
            return;
        }
        StreamEx.of(projectMemberService.findListByProjectId(program.getId()))
                .filter(member -> member.getType().equals(ProgramRoleTypeEnum.ProgramOwner.getCode()))
                .map(ProjectMember::getUserId)
                .findFirst()
                .ifPresent(currentUserId -> {
                    try {
                        projectMemberService.delMember(currentUserId, program, targetUserId);
                    } catch (CoreException e) {
                        log.error("DelMember Error, programId = {}, targetUserId = {}", program.getId(), targetUserId);
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
