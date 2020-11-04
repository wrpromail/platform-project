package net.coding.lib.project.helper;

import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ResourceReferenceService;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProjectResourceServiceHelper {

    @Resource
    private ProjectResourceService projectResourceService;

    @Resource
    private ProjectResourceSequenceService projectResourceSequenceService;

    @Resource
    private ResourceReferenceService resourceReferenceService;

    @Resource
    private ProjectResourceLinkService projectResourceLinkService;

    @Transactional(rollbackFor = Exception.class)
    public ProjectResource addProjectResource(ProjectResource record, String projectPath) {
        ProjectResource projectResource = projectResourceService.getByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            return projectResource;
        }
        int code = projectResourceSequenceService.generateProjectResourceCode(record.getProjectId());
        record.setCode(code);
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedAt(record.getCreatedAt());
        record.setUpdatedBy(record.getCreatedBy());
        record.setDeletedBy(0);
        record.setResourceUrl(projectResourceLinkService.getResourceLink(record, projectPath));
        projectResourceService.insert(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectResource(Integer projectId, String targetType, List<Integer> targetIdList, Integer userId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("targetType", targetType);
        parameters.put("targetIds", targetIdList);
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("deletedBy", userId);
        int result = projectResourceService.batchDelete(parameters);

        if(result > 0) {
            Map<String, Object> map = new HashMap<>();
            map.put("targetType", targetType);
            map.put("targetIds", targetIdList);
            map.put("deletedAt", DateUtil.getCurrentDate());
            resourceReferenceService.batchDelete(map);

            Map<String, Object> selfMap = new HashMap<>();
            selfMap.put("selfType", targetType);
            selfMap.put("selfIds", targetIdList);
            selfMap.put("deletedAt", DateUtil.getCurrentDate());
            resourceReferenceService.batchDelete(selfMap);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer generateCodes(Integer projectId, Integer codeAmount) {
        return projectResourceSequenceService.generateProjectResourceCodes(projectId, codeAmount);
    }
}
