package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.TextUtils;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.ProjectLabelDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.CoreRuntimeException;
import net.coding.lib.project.form.ProjectLabelForm;
import net.coding.lib.project.service.ProjectLabelService;
import net.coding.lib.project.service.ProjectService;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.git.GitDepotGrpcClient;
import proto.git.GitDepotProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

@Slf4j
@RestController
@Api(value = "项目标签", tags = "项目标签")
@AllArgsConstructor
@RequestMapping("/api/platform/project/{projectId}/label")
@RestfulApi
public class ProjectLableController {

    private final ProjectLabelService projectLabelService;
    private final GitDepotGrpcClient gitDepotGrpcClient;
    private final ProjectService projectService;


    /**
     * 检测项目权限，团队权限 id : 项目label id
     */
    @ModelAttribute
    public void preCheckPermission(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @PathVariable("projectId") Integer projectId,
            @PathVariable(value = "labelId", required = false) Integer id,
            Model model
    ) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null || !teamId.equals(project.getTeamOwnerId())) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        if (Objects.nonNull(id)) {
            ProjectLabel projectLabel = projectLabelService.findById(id);
            if (projectLabel == null || !projectLabel.getProjectId().equals(projectId)) {
                throw new CoreRuntimeException(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            model.addAttribute("projectLabel", projectLabel);
        }
        model.addAttribute("project", project);
    }

    @ApiOperation(value = "项目标签列表", notes = "项目标签列表")
    @ProtectedAPI
    @GetMapping
    public List<ProjectLabelDTO> getAllLabelByProject(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @ModelAttribute("project") Project project
    ) throws CoreException {
        return projectLabelService.getAllLabelByProject(project.getId())
                .stream()
                .map(item ->
                        ProjectLabelDTO.builder()
                                .id(item.getId())
                                .project_id(item.getProjectId())
                                .name(TextUtils.htmlEscape(item.getName()))
                                .color(item.getColor())
                                .owner_id(item.getOwnerId())
                                .merge_request_count(mrCount(item))
                                .build()
                )
                .collect(Collectors.toList());
    }

    private Long mrCount(ProjectLabel projectLabel) {
        try {
            return gitDepotGrpcClient.getMergeRequestCountByLabelId(
                    GitDepotProto.GetMergeRequestCountByLabelIdRequest.newBuilder()
                            .setLabelId(projectLabel.getId())
                            .build()).getCount();
        } catch (Exception e) {
            log.warn("getMergeRequestCountByLabelId error:{} ", e.getMessage());
            return 0L;
        }
    }

    @ApiOperation(value = "创建标签", notes = "创建标签")
    @ProjectApiProtector(function = Function.ProjectLabel, action = Action.Create)
    @ProtectedAPI
    @PostMapping
    public int createLabel(
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @ApiParam(value = "项目 ID（必填）", required = true)
            @ModelAttribute("project") Project project,
            @Valid ProjectLabelForm form
    ) {
        form.setProjectId(project.getId());
        return projectLabelService.createLabel(userId, form);
    }

    @ApiOperation(value = "编辑标签", notes = "编辑标签")
    @ProjectApiProtector(function = Function.ProjectLabel, action = Action.Update)
    @ProtectedAPI
    @PutMapping("{labelId}")
    public Boolean updateLabel(
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @ApiParam(value = "项目 ID（必填）", required = true)
            @ModelAttribute("project") Project project,
            @ApiParam(value = "项目标签 ID（必填）", required = true)
            @PathVariable("labelId") Integer labelId,
            @Valid ProjectLabelForm form
    ) {
        form.setProjectId(project.getId());
        return projectLabelService.updateLabel(userId, labelId, form);
    }

    @ApiOperation(value = "删除标签", notes = "删除标签")
    @ProjectApiProtector(function = Function.ProjectLabel, action = Action.Delete)
    @ProtectedAPI
    @DeleteMapping("{labelId}")
    public Boolean deleteLabel(
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @ApiParam(value = "项目 ID（必填）", required = true)
            @ModelAttribute("project") Project project,
            @ApiParam(value = "项目标签 ID（必填）", required = true)
            @PathVariable("labelId") Integer labelId
    ) {
        return projectLabelService.deleteLabel(userId, labelId, project.getId());
    }

}
