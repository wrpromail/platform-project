package net.coding.app.project.http;

import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.base.util.BeanUtil;
import net.coding.common.util.Result;
import net.coding.lib.project.dto.ProjectPreferenceDTO;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.service.ProjectPreferenceService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * 项目偏好设置 Controller.
 *
 * @author chenxinyu@coding.net
 */
@RestController
@RequestMapping("/api/platform/project")
@AllArgsConstructor
@Api(value = "项目偏好", tags = "项目偏好")
public class ProjectPreferenceController {

    private final ProjectPreferenceService projectPreferenceService;


    /**
     * 获取项目的偏好设置列表.
     *
     * @param projectId 项目编号
     * @return 项目偏好设置的列表
     */
    @ApiOperation(value = "项目偏好设置的列表", notes = "项目偏好设置的列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true)
    })
    @ProtectedAPI
    @RequestMapping(value = "/{projectId}/preference/get", method = GET)
    public List<ProjectPreferenceDTO> getProjectPreferences(
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "integer", required = true),
            @ApiImplicitParam(name = "status", value = "开关状态 0 ：关；1 ：开", paramType = "integer", required = true)
    })
    @ProtectedAPI
    @RequestMapping(value = "/{projectId}/preference/toggle", method = POST)
    public Result toggleProjectPreference(
            @PathVariable(value = "projectId") Integer projectId,
            @RequestParam Short type,
            @RequestParam Short status) {
        return Result.of(projectPreferenceService.toggleProjectPreference(projectId, type, status));
    }

}
