package net.coding.lib.project.service.openapi;

import net.coding.lib.project.dao.ProjectInvisibleDao;
import net.coding.lib.project.dao.ProjectMemberInvisibleDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectValidateService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.proto.open.api.project.ProjectProto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TEMPLATE_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;

/**
 * 针对涉及 TCB 等合作业务伙伴的特殊云 API 单独提出, 与常规业务隔离
 */
@Service
@Slf4j
@AllArgsConstructor
public class OpenApiProjectInvisibleService {

    private final UserGrpcClient userGrpcClient;

    private final TeamGrpcClient teamGrpcClient;

    private final ProjectValidateService projectValidateService;

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final ProjectInvisibleDao projectInvisibleDao;

    private final ProjectMemberInvisibleDao projectMemberInvisibleDao;

    private final ProjectMemberInspectService projectMemberInspectService;

    public List<Project> getJoinedProjectsByLabel(Integer teamId, Integer userId, String label) {
        Set<Integer> joinedProjectIds = getJoinedProjectIds(teamId, userId);
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        return projectInvisibleDao.findJoinedProjectsByLabel(teamId, joinedProjectIds, label);
    }

    public int createProject(ProjectProto.CreateProjectRequest request) throws Exception {
        UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
        if (Objects.isNull(currentUser)) {
            throw CoreException.of(USER_NOT_EXISTS);
        }
        TeamProto.GetTeamResponse currentTeam = teamGrpcClient.getTeam(currentUser.getTeamId());
        if (Objects.isNull(currentTeam)) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (!projectValidateService.validateProjectTemplate(request.getProjectTemplate())) {
            throw CoreException.of(PROJECT_TEMPLATE_NOT_EXIST);
        }
        if (Objects.isNull(ProjectLabelEnums.resolve(request.getLabel()))) {
            throw CoreException.of(PARAMETER_INVALID);
        }
        Project existProjectDisplayName = projectService.getByDisplayNameAndTeamId(
                request.getDisplayName(),
                currentUser.getTeamId());
        if (existProjectDisplayName != null) {
            return existProjectDisplayName.getId();
        }
        Project existProjectName = projectService.getByNameAndTeamId(
                request.getName(),
                currentUser.getTeamId());
        if (existProjectName != null) {
            return existProjectName.getId();
        }
        ProjectCreateParameter parameter = ProjectCreateParameter.builder()
                .name(request.getName())
                .displayName(StringUtils.defaultIfBlank(request.getDisplayName(), request.getName()))
                .description(request.getDescription())
                .icon(request.getIcon())
                .label(request.getLabel())
                .invisible(request.getInvisible())
                .type("2")
                .gitEnabled(true)
                .gitIgnore(StringUtils.defaultString(request.getGitIgnore(), "no"))
                .gitReadmeEnabled(StringUtils.defaultString(String.valueOf(request.getGitReadmeEnabled()), "false"))
                .gitLicense("no")
                .createSvnLayout(StringUtils.defaultString(String.valueOf(request.getCreateSvnLayout()), "false"))
                .vcsType(request.getVcsType())
                .shared(request.getShared())
                .userId(currentUser.getId())
                .userGk(currentUser.getGlobalKey())
                .teamId(currentUser.getTeamId())
                .projectTemplate(request.getProjectTemplate())
                .build();
        projectValidateService.validateCreateProject(parameter);
        Project project = projectService.createProject(parameter);
        //Serverless 项目 创建项目后自动把主账号拉到项目中
        if (ProjectLabelEnums.SLS.name().equals(request.getLabel())
                && request.getUser().getId() != currentTeam.getData().getOwner().getId()) {
            projectMemberService.doAddMember(parameter.getUserId(),
                    Collections.singletonList(currentTeam.getData().getOwner().getId()),
                    ADMIN, project, false);
        }
        return project.getId();
    }

    public Set<Integer> getJoinedProjectIds(Integer teamId, Integer userId) {
        List<ProjectMember> userGroupMembers = Optional.ofNullable(
                projectMemberInspectService.getGroupIdsForUser(userId)
        )
                .map(principalParameter ->
                        projectMemberInvisibleDao.findPrincipalMembers(
                                teamId,
                                principalParameter.getPrincipalType(),
                                principalParameter.getPrincipalIds()
                        )
                ).orElse(new ArrayList<>());

        List<ProjectMember> departmentMembers = Optional.ofNullable(
                projectMemberInspectService.getDepartmentIdsForUser(teamId, userId)
        )
                .map(principalParameter ->
                        projectMemberInvisibleDao.findPrincipalMembers(
                                teamId,
                                principalParameter.getPrincipalType(),
                                principalParameter.getPrincipalIds()
                        )
                ).orElse(new ArrayList<>());

        List<ProjectMember> userMembers = Optional.ofNullable(
                projectMemberInspectService.getUserIdsForUser(userId)
        )
                .map(principalParameter ->
                        projectMemberInvisibleDao.findPrincipalMembers(
                                teamId,
                                principalParameter.getPrincipalType(),
                                principalParameter.getPrincipalIds()
                        )
                ).orElse(new ArrayList<>());
        // 兼容迁移前 principal 为空的情况
        List<ProjectMember> userJoinedMembers =
                projectMemberInvisibleDao.findJoinPrincipalMembers(teamId, userId);
        return StreamEx.of(userGroupMembers, departmentMembers, userMembers, userJoinedMembers)
                .flatMap(Collection::stream)
                .nonNull()
                .map(ProjectMember::getProjectId)
                .toSet();
    }
}
