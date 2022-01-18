package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.QueryProjectForm;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.service.ProjectService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Api(value = "项目查询", tags = "项目查询")
@AllArgsConstructor
@RequestMapping("/api/platform/project")
public class LegacyProjectsController {
    private final ProjectService projectService;
    private final TeamServiceGrpcClient teamServiceGrpcClient;

    @ApiOperation("项目-分页查询")
    @PostMapping("/pages")
    public ResultPage<ProjectDTO> queryProjectPages(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestBody @Valid QueryProjectForm form
    ) throws CoreException {
        if (!teamServiceGrpcClient.isMember(teamId, userId)) {
            return new ResultPage<>();
        }
        return projectService.getProjectPages(ProjectPageQueryParameter.builder()
                .teamId(teamId)
                .userId(userId)
                .groupId(form.getGroupId())
                .keyword(form.getKeyword())
                .queryType(form.getQueryType().name())
                .sortKey(form.getSortBy().getSortKey().name())
                .sortValue(form.getSortBy().getSortValue().name())
                .deletedAt(form.getArchived() ? BeanUtils.getDefaultArchivedAt()
                        : BeanUtils.getDefaultDeletedAt())
                .page(form.getPage())
                .pageSize(form.getPageSize())
                .build());
    }
}
