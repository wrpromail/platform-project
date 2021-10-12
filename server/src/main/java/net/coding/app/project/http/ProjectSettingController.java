package net.coding.app.project.http;

import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.lib.project.dto.ProjectFunctionDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectSettingService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;

/**
 * @Author liuying
 * @Date 2021/1/5 10:47 上午
 * @Version 1.0
 */
@Api(value = "项目设置", tags = "项目设置")
@ProtectedAPI
@RequestMapping(value = "/api/platform/project/{projectId}/settings")
@RestController
@AllArgsConstructor
public class ProjectSettingController {

    private final ProjectSettingService projectSettingService;
    /**
     * 获取所有的功能设置项
     */
    @RequestMapping(value = "functions", method = RequestMethod.GET)
    public List<ProjectFunctionDTO> getAllFunctions(@PathVariable Integer projectId) {
        return projectSettingService.getAllFunction(projectId);
    }
    /**
     * 修改功能状态
     */
    @RequestMapping(value = "/function", method = RequestMethod.PUT)
    @ProjectApiProtector(function = Function.ProjectFunction, action = Action.Update)
    public boolean updateFunctionStatus(@PathVariable Integer projectId,
                                       @RequestParam String function,
                                       @RequestParam Boolean status
    ) throws CoreException {

        boolean result = projectSettingService.updateProjectFunction(
                projectId,
                function,
                status
        );
        return result;
    }
}
