package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.constants.TwoFactorAuthConstants;
import net.coding.common.storage.support.internal.StorageStream;
import net.coding.common.storage.support.internal.StorageUploadStream;
import net.coding.common.util.Result;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.CreateProjectForm;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectValidateService;

import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import proto.platform.user.UserProto;

@RestController
@Api(value = "项目", tags = "项目")
@AllArgsConstructor
@RequestMapping("/api/platform/project")
public class ProjectController {

    private final ProjectService projectService;

    private final ProjectValidateService projectValidateService;

    @ApiOperation(value = "create_project", notes = "创建项目")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Create)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
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

    @ApiOperation(value = "update_project_icon", notes = "更新项目图标")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Update)
    @RequestMapping(value = "/icon", method = RequestMethod.POST)
    public ProjectDTO updateProjectIcon(@RequestParam(required = false) Integer projectId,
                                        @StorageStream(bucket = "coding-net-project-icon") StorageUploadStream form) throws CoreException {

        return projectService.updateProjectIcon(projectId, form);
    }

    @ApiOperation(value = "delete_project", notes = "删除项目")
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_DEFAULT)
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Delete)
    @RequestMapping(value = "{projectId}", method = RequestMethod.DELETE)
    public Result deleteProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                @PathVariable Integer projectId) throws CoreException {
        Integer userId = 0;
        if (Objects.isNull(SystemContextHolder.get())) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        userId = SystemContextHolder.get().getId();
        projectService.delete(userId, teamId, projectId);
        return Result.success();
    }

    @ApiOperation(value = "update_project", notes = "更新项目")
    @ProtectedAPI
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Update)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ProjectDTO updateProject(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                    @Valid UpdateProjectForm form,
                                    Errors errors) throws
            CoreException {

        projectService.validateUpDate(form, errors);
        if (errors.hasErrors()) {
            throw CoreException.of(errors);
        }
        Project project = projectService.getById(projectValidateService.getId(form.getId()));

        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        if (!project.getTeamOwnerId().equals(teamId)) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }

        int result = projectService.update(form);
        if (result == 0) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }

        return projectService.getProjectDtoById(project.getId());
    }

    @ApiOperation(value = "visit_project", notes = "更新项目阅读时间")
    @ProtectedAPI
    @RequestMapping(value = {"/{projectId}/update-visit"}, method = RequestMethod.POST)
    public boolean visitProject(@PathVariable("projectId") int projectId) throws
            CoreException {
        return projectService.updateVisitProject(projectId);
    }

    @ApiOperation(value = "query_project_by_name", notes = "项目名称查询项目信息")
    @ProtectedAPI
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.View)
    @ProjectApiProtector(function = Function.ProjectMember, action = Action.View)
    @RequestMapping(value = {"/{projectName}"}, method = RequestMethod.GET)
    public ProjectDTO queryProjectByName(@RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
                                         @PathVariable("projectName") String projectName) {
        return projectService.getProjectByNameAndTeamId(projectName, teamId);
    }

    private String getProjectPath(Project project) {
        return "/p/" + project.getName();
    }
}
