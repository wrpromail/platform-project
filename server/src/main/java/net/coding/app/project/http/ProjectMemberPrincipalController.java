package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberQueryPageReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberReqDTO;
import net.coding.lib.project.dto.response.ProjectMemberQueryPageRespDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.member.ProjectMemberPrincipalService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalWriteService;
import net.coding.platform.ram.annotation.Action;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/platform/{project}/{projectId}/members/principal")
@AllArgsConstructor
@Api(value = "项目成员主体-项目内", tags = "项目成员主体-项目内")
@RestfulApi
public class ProjectMemberPrincipalController {

    private final ProjectMemberPrincipalService projectMemberPrincipalService;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    @Action(name = "viewProjectMember", description = "查询项目成员主体列表-(包含用户组、部门、成员)", actionType = Action.ActionType.List)
    @ApiOperation("查询项目成员主体列表-(包含用户组、部门、成员)")
    @GetMapping("/search")
    public ResultPage<ProjectMemberQueryPageRespDTO> queryProjectMemberPrincipalPages(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable(value = "project") String resourceType,
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam("权限组Id") @RequestParam(required = false) Long policyId,
            @ApiParam("关键字搜索") @RequestParam(required = false) String keyword,
            @ApiParam("分页") LimitedPager pager
    ) throws CoreException {
        return projectMemberPrincipalService.findProjectMemberPrincipalPages(
                ProjectMemberQueryPageReqDTO.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .projectId(projectId)
                        .policyId(policyId)
                        .keyword(keyword)
                        .build(),
                pager);
    }

    @Action(name = "createProjectMember", description = "添加项目成员主体-(包含用户组、部门、成员)", actionType = Action.ActionType.Write)
    @ApiOperation("添加项目成员主体-(包含用户组、部门、成员)")
    @PostMapping("/add")
    public void addMember(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable(value = "project") String resourceType,
            @PathVariable(value = "projectId") Integer projectId,
            @RequestBody @Valid List<ProjectMemberAddReqDTO> reqDTOs
    ) throws CoreException {
        projectMemberPrincipalWriteService.addMember(teamId, userId, projectId, reqDTOs);
    }

    @Action(name = "deleteProjectMember", description = "移出项目成员主体-(包含用户组、部门、成员)", actionType = Action.ActionType.Write)
    @ApiOperation("移出项目成员主体-(包含用户组、部门、成员)")
    @DeleteMapping("/del")
    public void delMember(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable(value = "project") String resourceType,
            @PathVariable(value = "projectId") Integer projectId,
            @RequestBody @Valid List<ProjectMemberReqDTO> reqDTOs
    ) throws CoreException {
        projectMemberPrincipalWriteService.delMember(teamId, userId, projectId, reqDTOs);
    }

    @ApiOperation("退出当前项目")
    @DeleteMapping("/quit")
    public void quit(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable(value = "project") String resourceType,
            @PathVariable(value = "projectId") Integer projectId
    ) throws CoreException {
        projectMemberPrincipalWriteService.quit(teamId, userId, projectId);
    }
}
