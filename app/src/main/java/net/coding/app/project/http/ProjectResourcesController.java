package net.coding.app.project.http;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.utils.ResponseUtil;
import net.coding.app.project.utils.ResultModel;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/project/resources")
public class ProjectResourcesController {

    @Autowired
    private ProjectResourceService projectResourceService;

    @Autowired
    private ProjectResourceSequenceService projectResourceSequenceService;

    @GetMapping("/findProjectResourceList")
    public ResultModel<String> findProjectResourceList(Integer projectId, Integer page, Integer pageSize) {
        if(projectId <= 0) {
            return ResponseUtil.buildFaildResponse("-1", "param projectId error");
        }
        if(page == null || page <= 0) {
            page = 1;
        }
        if(pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        PageInfo<ProjectResource> pageInfo = projectResourceService.findProjectResourceList(projectId, null, null, page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageInfo.getList());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPage", pageInfo.getPages());
        result.put("totalRow", pageInfo.getTotal());
        return ResponseUtil.buildSuccessResponse(result);
    }

    @GetMapping("/findProjectResourceInfo")
    public ResultModel<String> findProjectResourceInfo(Integer projectResourceId) {
        if(projectResourceId == null || projectResourceId <= 0) {
            return ResponseUtil.buildFaildResponse("-1", "param projectResourceId error");
        }
        return ResponseUtil.buildSuccessResponse(projectResourceService.selectById(projectResourceId));
    }

    @GetMapping("/batchProjectResourceList")
    public ResultModel<String> batchProjectResourceList(Integer projectId, List<Integer> codes) {
        if(projectId == null || projectId <= 0) {
            return ResponseUtil.buildFaildResponse("-1", "param projectId error");
        }
        if(codes == null || codes.size() <= 0) {
            return ResponseUtil.buildFaildResponse("-1", "param codes error");
        }
        return ResponseUtil.buildSuccessResponse(projectResourceService.batchProjectResourceList(projectId, codes));
    }
}
