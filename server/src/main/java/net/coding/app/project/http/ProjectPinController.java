package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.Result;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectPinService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Api(value = "星标项目查询", tags = "星标项目查询")
@AllArgsConstructor
@RequestMapping("/api/platform/project")
public class ProjectPinController {

    private final ProjectPinService projectPinService;

    @Deprecated
    @ProtectedAPI
    @ApiOperation("星标项目-分页查询")
    @PostMapping("/pin/pages")
    public ResultPage<ProjectDTO> queryProjectPinPages(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(required = false) String keyword,
            LimitedPager pager
    ) {
        return projectPinService.getProjectPinPages(
                teamId,
                userId,
                keyword,
                pager
        );
    }

    @ProtectedAPI
    @ApiOperation("星标项目-分页查询")
    @GetMapping("/pin/projects")
    public ResultPage<ProjectDTO> pagePinProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(required = false) String keyword,
            LimitedPager pager
    ) {
        return projectPinService.getProjectPinPages(
                teamId,
                userId,
                keyword,
                pager
        );
    }

    @ProtectedAPI
    @ApiOperation("星标项目-标记")
    @PutMapping(value = {"/{projectId}/pin"})
    public boolean pinProject(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId
    ) throws CoreException {
        return projectPinService.pinProject(teamId, userId, projectId);
    }

    @ProtectedAPI
    @ApiOperation("星标项目-取消标记")
    @DeleteMapping("/{projectId}/pin")
    public boolean cancelPinProject(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId
    )
            throws CoreException {
        return projectPinService.cancelPinProject(teamId, userId, projectId);
    }

    @ProtectedAPI
    @ApiOperation("星标项目-移动排序")
    @PatchMapping("/{projectId}/pin/sort")
    public Result sortPinProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId,
            @RequestParam(required = false, defaultValue = "0") Integer targetId
    ) throws CoreException {
        projectPinService.sortPinProject(teamId, userId, projectId, targetId);
        return Result.success();
    }
}
