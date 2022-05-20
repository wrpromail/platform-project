package net.coding.app.project.http;

import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.base.util.BeanUtil;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.ProjectPreferenceDTO;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.service.ProjectPreferenceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;

/**
 * 项目偏好设置 Controller.
 *
 * @author chenxinyu@coding.net
 */
@RestController
@RequestMapping("/api/platform/project")
@AllArgsConstructor
@Api(value = "项目偏好", tags = "项目偏好")
@RestfulApi
public class ProjectPreferenceController {

    private final ProjectPreferenceService projectPreferenceService;


    /**
     * 获取项目的偏好设置列表.
     *
     * @param projectId 项目编号
     * @return 项目偏好设置的列表
     */
    @ApiOperation(value = "项目偏好设置的列表", notes = "项目偏好设置的列表")
    @ProtectedAPI
    @GetMapping("/{projectId}/preference/get")
    public List<ProjectPreferenceDTO> getProjectPreferences(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId) {
        List<ProjectPreference> projectPreferences =
                projectPreferenceService.getProjectPreferences(projectId);
        return BeanUtil.copyPropertiesOfList(
                projectPreferences, ProjectPreferenceDTO.class);
    }

    /**
     * 切换项目偏好设置开关.
     *
     * @param projectId 项目编号
     * @param type      偏好设置类型
     * @param status    偏好设置状态
     */
    @ApiOperation(value = "项目偏好设置", notes = "项目偏好设置")
    @ProtectedAPI
    @PostMapping("/{projectId}/preference/toggle")
    public void toggleProjectPreference(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "类型", required = true)
            @RequestParam Short type,
            @ApiParam(value = "开关状态 0 ：关；1 ：开", required = true)
            @RequestParam Short status) {
        projectPreferenceService.toggleProjectPreference(projectId, type, status);
    }

}
