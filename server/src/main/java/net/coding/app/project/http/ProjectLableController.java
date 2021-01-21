package net.coding.app.project.http;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.dao.MergeRequestLabelDao;
import net.coding.lib.project.dto.ProjectLabelDTO;
import net.coding.lib.project.form.ProjectLabelForm;
import net.coding.lib.project.service.ProjectLabelService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "项目标签", tags = "项目标签")
@AllArgsConstructor
@RequestMapping("/api/platform/project/{projectId}/label")
public class ProjectLableController {

    private final ProjectLabelService projectLabelService;
    private final MergeRequestLabelDao mrLabelDao;

    @ApiOperation(value = "列表", tags = "列表")
    @ProtectedAPI
    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<ProjectLabelDTO> getAllLabelByProject(
            @PathVariable("projectId") Integer projectId
    ) {
        return projectLabelService.getAllLabelByProject(projectId)
                .stream()
                .map(item ->
                        ProjectLabelDTO.builder()
                                .id(item.getId())
                                .project_id(item.getProjectId())
                                .name(TextUtils.htmlEscape(item.getName()))
                                .color(item.getColor())
                                .owner_id(item.getOwnerId())
                                .merge_request_count(mrLabelDao.countByLabel(item.getId()))
                                .build()
                )
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "创建标签", tags = "创建标签")
    @ProtectedAPI
    @RequestMapping(value = "", method = RequestMethod.POST)
    public int createLabel(
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @PathVariable("projectId") Integer projectId,
            @Valid ProjectLabelForm form
    ) {
        form.setProjectId(projectId);
        return projectLabelService.createLabel(userId, form);
    }

    @ApiOperation(value = "编辑标签", tags = "编辑标签")
    @ProjectApiProtector(function = Function.ProjectLabel, action = Action.Update)
    @ProtectedAPI
    @RequestMapping(value = "{labelId}", method = RequestMethod.PUT)
    public Boolean updateLabel(
            @PathVariable("projectId") Integer projectId,
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @PathVariable("labelId") Integer labelId,
            @Valid ProjectLabelForm form
    ) {
        form.setProjectId(projectId);
        return projectLabelService.updateLabel(userId, labelId, form);
    }

    @ApiOperation(value = "删除标签", tags = "删除标签")
    @ProjectApiProtector(function = Function.ProjectLabel, action = Action.Delete)
    @ProtectedAPI
    @RequestMapping(value = "{labelId}", method = RequestMethod.DELETE)
    public Boolean deleteLabel(
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @PathVariable("projectId") Integer projectId,
            @PathVariable("labelId") Integer labelId
    ) {
        return projectLabelService.deleteLabel(userId, labelId, projectId);
    }

}
