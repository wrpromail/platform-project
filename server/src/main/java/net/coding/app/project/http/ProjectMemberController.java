package net.coding.app.project.http;

import com.github.pagehelper.PageRowBounds;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.util.ResultPage;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.ProjectTeamMemberDTO;
import net.coding.lib.project.dto.RoleDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddMemberForm;
import net.coding.lib.project.pager.PagerResolve;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/platform/project/{projectId}/members")
@AllArgsConstructor
@Api(value = "项目成员", tags = "项目成员")
@RestfulApi
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    private final ProjectMemberPrincipalService projectMemberPrincipalService;

    @ApiOperation(value = "项目成员列表", notes = "项目成员列表")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = ProjectMemberDTO.class)})
    @GetMapping
    public ResultPage<ProjectMemberDTO> members(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "查询关键字")
            @RequestParam(required = false) String keyWord,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {
        return projectMemberPrincipalService.getProjectMembers(teamId, projectId, keyWord, null, pager);
    }

    @ApiOperation(value = "项目角色列表", notes = "项目角色列表")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = RoleDTO.class)})
    @GetMapping("/roles")
    public List<RoleDTO> listRoles(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId
    ) throws CoreException {
        return projectMemberService.findMemberCountByProjectId(projectId);
    }

    /**
     * 成员添加接口
     */
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT_MEMBERS)
    @ProjectApiProtector(function = Function.ProjectMember, action = Action.Create)
    @PostMapping
    public void addMemberForGK(
            @PathVariable(value = "projectId") Integer projectId,
            @Valid AddMemberForm form
    ) throws CoreException {
        projectMemberService.doAddMember(form, projectId);
    }

    @ApiOperation(value = "删除某个项目成员", notes = "删除某个项目成员")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT_MEMBERS)
    @ProjectApiProtector(function = Function.ProjectMember, action = Action.Delete)
    @DeleteMapping("/{targetUserId}")
    public void delMember(
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable("targetUserId") int targetUserId,
            @PathVariable("projectId") int projectId
    ) throws CoreException {
        projectMemberService.delMember(userId, projectId, targetUserId);
    }

    @ApiOperation(value = "包含团队成员的项目成员列表", notes = "包含团队成员的项目成员列表")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = ProjectTeamMemberDTO.class)})
    @ProtectedAPI
    @GetMapping("/with/team-member")
    public ResultPage<ProjectTeamMemberDTO> memberList(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "查询关键字")
            @RequestParam(required = false) String keyWord,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {
        return projectMemberPrincipalService.getMemberWithProjectAndTeam(teamId, projectId, keyWord, pager);
    }

    @ApiOperation(value = "quit", notes = "退出当前项目")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = boolean.class)})
    @PostMapping("/quit")
    public void quit(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable("projectId") int projectId
    ) throws CoreException {
        projectMemberService.quit(projectId);
    }
}
