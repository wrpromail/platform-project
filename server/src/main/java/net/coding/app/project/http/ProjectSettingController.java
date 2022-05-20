package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.setting.ProjectSettingDTO;
import net.coding.lib.project.setting.ProjectSettingFunctionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;

@Api(value = "项目设置", tags = "项目设置")
@ProtectedAPI
@RequestMapping(value = "/api/platform/project/{projectId}/settings")
@RestController
@AllArgsConstructor
@RestfulApi
public class ProjectSettingController {

    private final ProjectSettingFunctionService projectSettingFunctionService;

    @ApiOperation("获取所有的功能设置项")
    @GetMapping(value = "functions")
    public List<ProjectSettingDTO> getFunctions(
            @RequestHeader(GatewayHeader.PROJECT_ID) Integer projectId
    ) {
        return projectSettingFunctionService.getFunctions(projectId);
    }

    @ApiOperation("修改功能状态")
    @PutMapping(value = "/function")
    @ProjectApiProtector(function = Function.ProjectFunction, action = Action.Update)
    public boolean update(
            @RequestHeader(GatewayHeader.PROJECT_ID) Integer projectId,
            @RequestParam String function,
            @RequestParam Boolean status
    ) throws CoreException {
        return projectSettingFunctionService.update(
                projectId,
                function,
                status
        );
    }
}
