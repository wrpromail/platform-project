package net.coding.app.project.http;

import com.github.pagehelper.PageRowBounds;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.util.Result;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.ProjectTeamMemberDTO;
import net.coding.lib.project.dto.RoleDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddMemberForm;
import net.coding.lib.project.pager.PagerResolve;
import net.coding.lib.project.service.ProjectMemberService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/platform/project/{projectId}/members")
@AllArgsConstructor
@Api(value = "项目成员", tags = "项目成员")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @ApiOperation(value = "项目成员列表", notes = "项目成员列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "keyWord", value = "查询关键字", paramType = "string")
    })
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = ProjectMemberDTO.class)})
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Result members(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @PathVariable(value = "projectId") Integer projectId,
            @RequestParam(required = false) String keyWord,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {

        return Result.success(projectMemberService.getProjectMembers(teamId,projectId, keyWord, null, pager));
    }


    @ApiOperation(value = "项目角色列表", notes = "项目角色列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true)
    })
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = RoleDTO.class)})
    @RequestMapping(value = "/roles", method = RequestMethod.GET)
    public Result listRoles(@PathVariable(value = "projectId") Integer projectId) throws CoreException {
        return Result.success(projectMemberService.findMemberCountByProjectId(projectId));
    }

    /**
     * 成员添加接口
     */
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT_MEMBERS)
    @ProjectApiProtector(function = Function.ProjectMember, action = Action.Create)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Result addMemberForGK(
            @PathVariable(value = "projectId") Integer projectId,
            @Valid AddMemberForm form
    ) throws CoreException {
        projectMemberService.doAddMember(form, projectId);
        return Result.success();
    }

    @ApiOperation(value = "删除某个项目成员", notes = "删除某个项目成员")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT_MEMBERS)
    @ProjectApiProtector(function = Function.ProjectMember, action = Action.Delete)
    @RequestMapping(value = "/{targetUserId}", method = RequestMethod.DELETE)
    public Result delMember(
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable("targetUserId") int targetUserId,
            @PathVariable("projectId") int projectId
    ) throws CoreException {
        projectMemberService.delMember(userId, projectId, targetUserId);
        return Result.success();

    }

    @ApiOperation(value = "包含团队成员的项目成员列表", notes = "包含团队成员的项目成员列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "keyWord", value = "查询关键字", paramType = "string")
    })
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = ProjectTeamMemberDTO.class)})
    @ProtectedAPI
    @RequestMapping(value = "/with/team-member", method = RequestMethod.GET)
    public Result memberList(
            @PathVariable(value = "projectId") Integer projectId,
            @RequestParam(required = false) String keyWord,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {
        return Result.success(projectMemberService.getMemberWithProjectAndTeam(projectId, keyWord, pager));
    }

    @ApiOperation(value = "quit", notes = "退出当前项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true)
    })
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = boolean.class)})
    @RequestMapping(value = {"/quit"}, method = RequestMethod.POST)
    public Result quit(@PathVariable("projectId") int projectId) throws CoreException {
        return Result.of(projectMemberService.quit(projectId) == 1);
    }
}
