package net.coding.app.project.http;

import net.coding.lib.project.dto.ProjectFunctionModuleDTO;
import net.coding.lib.project.setting.ProjectFunctionModuleService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Api(value = "项目功能开关查询", tags = "项目功能开关")
@AllArgsConstructor
@RequestMapping("/api/platform/project")
public class ProjectFunctionController {
    private final ProjectFunctionModuleService projectFunctionModuleService;

    @ApiOperation("项目-创建项目功能模版选择")
    @GetMapping("/function/modules")
    public List<ProjectFunctionModuleDTO> queryProjectFunctionModules() {
        return projectFunctionModuleService.getProjectFunctionModules();
    }
}
