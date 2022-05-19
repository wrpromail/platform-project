package net.coding.app.project.http;

import com.github.pagehelper.PageInfo;

import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.util.Result;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

@RestController
@Slf4j
@ProtectedAPI
@RequestMapping("/api/platform/project/resources")
@Api(value = "项目资源", tags = "项目资源")
public class ProjectResourcesController {

    @Autowired
    private ProjectResourceService projectResourceService;

    @Autowired
    private ProjectResourceLinkService projectResourceLinkService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @ApiOperation(value = "查询项目资源列表", notes = "查询项目资源列表")
    @GetMapping("/findProjectResourceList")
    public ResultPage<ProjectResource> findProjectResourceList(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @RequestParam Integer projectId,
            @ApiParam(value = "页码", required = true)
            @RequestParam Integer page,
            @ApiParam(value = "每页条数", required = true)
            @RequestParam Integer pageSize
    ) throws CoreException {
        if (projectId <= 0) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        if (page == null || page <= 0) {
            page = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        PageInfo<ProjectResource> pageInfo = projectResourceService.findProjectResourceList(projectId, null, null, page, pageSize);
        List<ProjectResource> projectResourceList = pageInfo.getList();
        String projectPath = projectGrpcClient.getProjectPath(projectId);
        projectResourceList.forEach(projectResource -> {
            projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
        });
        return new ResultPage(pageInfo.getList(), page, pageSize, pageInfo.getTotal());
    }

    @ApiOperation(value = "查询项目资源信息", notes = "查询项目资源信息")
    @GetMapping("/findProjectResourceInfo")
    public Result findProjectResourceInfo(
            @ApiParam(value = "项目资源 ID（必填）", required = true)
            @RequestParam Integer projectResourceId
    ) {
        if (projectResourceId == null || projectResourceId <= 0) {
            return Result.failed();
        }
        ProjectResource projectResource = projectResourceService.getById(projectResourceId);
        if (Objects.nonNull(projectResource)) {
            String projectPath = projectGrpcClient.getProjectPath(projectResource.getProjectId());
            projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
            return Result.success(projectResource);
        }
        return Result.failed();
    }

    @ApiOperation(value = "批量查询项目资源列表", notes = "批量查询项目资源列表")
    @GetMapping("/batchProjectResourceList")
    public Result batchProjectResourceList(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @RequestParam Integer projectId,
            @ApiParam(value = "资源序号（必填）", required = true)
            @RequestParam List<Integer> codes
    ) {
        if (projectId == null || projectId <= 0) {
            return Result.failed();
        }
        if (CollectionUtils.isEmpty(codes)) {
            return Result.failed();
        }
        List<ProjectResource> projectResourceList = projectResourceService.batchProjectResourceList(projectId, codes);
        String projectPath = projectGrpcClient.getProjectPath(projectId);
        projectResourceList.forEach(projectResource -> {
            projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
        });
        return Result.success(projectResourceList);
    }
}
