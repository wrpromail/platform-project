package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.constants.TwoFactorAuthConstants;
import net.coding.common.storage.support.internal.StorageStream;
import net.coding.common.storage.support.internal.StorageUploadStream;
import net.coding.common.util.Result;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.CreateProjectForm;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

@Slf4j
@RestController
@Api(value = "项目", tags = "项目")
@AllArgsConstructor
@RequestMapping("/api/platform/project")
public class ProjectController {
    private final ProjectService projectService;

    @ApiOperation(value = "create_project", notes = "创建项目")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Create)
    @PostMapping("/create")
    public String createProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                @Valid CreateProjectForm form) throws Exception {
        UserProto.User user = SystemContextHolder.get();
        if (Objects.isNull(user)) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        Project project = projectService.createProject(ProjectCreateParameter.builder()
                .userId(user.getId())
                .teamId(teamId)
                .name(form.getName().replace(" ", "-"))
                .displayName(form.getDisplayName())
                .description(form.getDescription())
                .icon(form.getIcon())
                .groupId(form.getGroupId())
                .projectTemplate(form.getProjectTemplate())
                .template(form.getTemplate())
                .functionModule(form.getFunctionModule())
                .build());
        return getProjectPath(project);
    }

    @ApiOperation(value = "delete_project", notes = "删除项目")
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_DEFAULT)
    @DeleteMapping("{projectId}")
    public Result deleteProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                @PathVariable Integer projectId) throws CoreException {
        projectService.delete(userId, teamId, projectId);
        return Result.success();
    }

    @ApiOperation(value = "update_project_icon", notes = "更新项目图标")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    @PostMapping("/icon")
    public ProjectDTO updateProjectIcon(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                        @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                        @RequestParam(required = false) Integer projectId,
                                        @StorageStream(bucket = "coding-net-project-icon") StorageUploadStream form) throws CoreException {

        return projectService.updateIcon(teamId, userId, projectId, form);
    }

    @ApiOperation(value = "update_project", notes = "更新项目")
    @ProtectedAPI
    @PostMapping("/update")
    public ProjectDTO updateProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                    @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                    @Valid UpdateProjectForm form) throws CoreException, MilestoneException {
        return projectService.update(teamId, userId, form);
    }

    @ApiOperation(value = "visit_project", notes = "更新项目阅读时间")
    @ProtectedAPI
    @PostMapping("/{projectId}/update-visit")
    public boolean visitProject(@PathVariable("projectId") int projectId) throws
            CoreException {
        return projectService.updateVisit(projectId);
    }

    @ApiOperation(value = "query_project_by_name", notes = "项目名称查询项目信息")
    @ProtectedAPI
    @GetMapping("/{projectName:.+}")
    public ProjectDTO queryProjectByName(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable("projectName") String projectName
    ) throws CoreException {
        return projectService.getProjectByNameAndTeamId(teamId, userId, projectName);
    }

    @ApiOperation(value = "archive_project", notes = "归档项目")
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_DEFAULT)
    @PostMapping("/{projectId}/archive")
    public Result archiveProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                 @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                 @PathVariable Integer projectId) throws CoreException {
        projectService.archive(teamId, userId, projectId);
        return Result.success();
    }

    @ApiOperation(value = "unarchive_project", notes = "取消归档项目")
    @ProtectedAPI
    @PostMapping("/{projectId}/unarchive")
    public Result unarchiveProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                   @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                   @PathVariable Integer projectId) throws CoreException {
        projectService.unarchive(teamId, userId, projectId);
        return Result.success();
    }

    @ApiOperation(value = "query_joined_projects", notes = "我参与的项目列表")
    @ProtectedAPI
    @GetMapping("/joined/projects")
    public List<ProjectDTO> queryJoinedProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId) throws CoreException {
        return projectService.getJoinedProjectDTOs(teamId, userId);
    }

    private String getProjectPath(Project project) {
        return "/p/" + project.getName();
    }


}
