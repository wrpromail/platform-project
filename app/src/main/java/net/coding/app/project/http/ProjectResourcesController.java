package net.coding.app.project.http;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.utils.ResponseUtil;
import net.coding.app.project.utils.ResultModel;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.util.Result;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

@RestController
@Slf4j
@ProtectedAPI
@RequestMapping("/api/platform/project/resources")
public class ProjectResourcesController {

    @Autowired
    private ProjectResourceService projectResourceService;

    @GetMapping("/findProjectResourceList")
    public ResultPage<ProjectResource> findProjectResourceList(Integer projectId, Integer page, Integer pageSize) throws CoreException {
        if(projectId <= 0) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        if(page == null || page <= 0) {
            page = 1;
        }
        if(pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        PageInfo<ProjectResource> pageInfo = projectResourceService.findProjectResourceList(projectId, null, null, page, pageSize);
        return new ResultPage(pageInfo.getList(), page, pageSize, pageInfo.getTotal());
    }

    @GetMapping("/findProjectResourceInfo")
    public Result findProjectResourceInfo(Integer projectResourceId) {
        if(projectResourceId == null || projectResourceId <= 0) {
            return Result.failed();
        }
        return Result.success(projectResourceService.selectById(projectResourceId));
    }

    @GetMapping("/batchProjectResourceList")
    public Result batchProjectResourceList(Integer projectId, List<Integer> codes) {
        if(projectId == null || projectId <= 0) {
            return Result.failed();
        }
        if(CollectionUtils.isEmpty(codes)) {
            return Result.failed();
        }
        return Result.success(projectResourceService.batchProjectResourceList(projectId, codes));
    }
}
