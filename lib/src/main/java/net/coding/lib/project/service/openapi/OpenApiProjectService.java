package net.coding.lib.project.service.openapi;

import net.coding.exchange.dto.user.User;
import net.coding.grpc.client.platform.UnsafeUserServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.enums.RegisterSourceEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectValidateService;
import net.coding.proto.open.api.project.ProjectProto;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.team.TeamProto;

import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TEMPLATE_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;

/**
 * @Description: open api  项目逻辑
 */
@Service
@Slf4j
@AllArgsConstructor
public class OpenApiProjectService {
    private final UnsafeUserServiceGrpcClient unsafeUserServiceGrpcClient;
    private final TeamGrpcClient teamGrpcClient;
    private final ProjectValidateService projectValidateService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    public int createProject(ProjectProto.CreateProjectRequest request,
                             String registerSource) throws Exception {
        User currentUser = unsafeUserServiceGrpcClient.getUser(request.getUser().getId());
        if (Objects.isNull(currentUser)) {
            throw CoreException.of(USER_NOT_EXISTS);
        }
        TeamProto.GetTeamResponse currentTeam = teamGrpcClient.getTeam(request.getUser().getTeamId());
        if (Objects.isNull(currentTeam)) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (!projectValidateService.validateProjectTemplate(request.getProjectTemplate())) {
            throw CoreException.of(PROJECT_TEMPLATE_NOT_EXIST);
        }
        // 云 API 有 Label
        String label = "";
        boolean invisible = false;
        if (RegisterSourceEnum.QCLOUD_API.name().equals(registerSource)) {
            if (Objects.isNull(ProjectLabelEnums.resolve(request.getLabel()))) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            label = request.getLabel();
            invisible = request.getInvisible();
        }
        Project existProjectDisplayName = projectService.getByDisplayNameAndTeamId(
                request.getDisplayName(),
                currentUser.getTeam_id());
        if (existProjectDisplayName != null) {
            return existProjectDisplayName.getId();
        }
        Project existProjectName = projectService.getByNameAndTeamId(
                request.getName(),
                currentUser.getTeam_id());
        if (existProjectName != null) {
            return existProjectName.getId();
        }
        ProjectCreateParameter parameter = ProjectCreateParameter.builder()
                .name(request.getName())
                .displayName(StringUtils.defaultIfBlank(request.getDisplayName(), request.getName()))
                .description(request.getDescription())
                .icon(request.getIcon())
                .label(label)
                .invisible(invisible)
                .type("2")
                .gitEnabled(true)
                .gitIgnore(StringUtils.defaultString(request.getGitIgnore(), "no"))
                .gitReadmeEnabled(StringUtils.defaultString(String.valueOf(request.getGitReadmeEnabled()), "false"))
                .gitLicense("no")
                .createSvnLayout(StringUtils.defaultString(String.valueOf(request.getCreateSvnLayout()), "false"))
                .vcsType(request.getVcsType())
                .shared(request.getShared())
                .userId(currentUser.getId())
                .userGk(currentUser.getGlobal_key())
                .teamId(currentUser.getTeam_id())
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
}
