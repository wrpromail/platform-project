package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.Result;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.ProjectJoinProjectMemberDTO;
import net.coding.lib.project.dto.request.ProjectMemberBatchAddReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberBatchDelReqDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.member.ProjectMemberPrincipalService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalWriteService;
import net.coding.platform.ram.annotation.Action;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/platform/project/members/principal")
@AllArgsConstructor
@Api(value = "项目成员主体", tags = "项目成员主体")
@RestfulApi
public class ProjectMemberPrincipalBatchController {

    private final ProjectMemberPrincipalService projectMemberPrincipalService;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    @ApiOperation(value = "查询加入项目的项目成员", notes = "查询加入项目的项目成员")
    @GetMapping("/joined/members")
    public List<ProjectJoinProjectMemberDTO> queryJoinProjectMembers(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam("关键字搜索") @RequestParam(required = false) String keyword) {
        return projectMemberPrincipalService.findJoinProjectMembers(teamId, userId, keyword);
    }

    @Action(name = "addProjectOnMember", description = "批量添加-项目成员添加至多个项目", actionType = Action.ActionType.Write)
    @ApiOperation("批量添加-项目成员添加至多个项目")
    @PostMapping("/batch/add")
    public void batchAddMember(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestBody @Valid ProjectMemberBatchAddReqDTO reqDTO
    ) throws CoreException {
        projectMemberPrincipalWriteService.batchAddMember(teamId, userId, reqDTO);
    }

    @Action(name = "deleteProjectOnMember", description = "批量移出-项目成员至多个项目移出", actionType = Action.ActionType.Write)
    @ApiOperation("批量移出-项目成员至多个项目移出")
    @DeleteMapping("/batch/del")
    public void batchDelMember(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestBody @Valid ProjectMemberBatchDelReqDTO reqDTO
    ) throws CoreException {
        projectMemberPrincipalWriteService.batchDelMember(teamId, userId, reqDTO);
    }
}
