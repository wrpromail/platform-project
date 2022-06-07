package net.coding.lib.project.helper;

import net.coding.lib.project.dto.ProjectResourceDTO;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.enums.ScopeTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ResourceLinkService;
import net.coding.lib.project.service.ResourceReferenceService;
import net.coding.lib.project.service.ResourceSequenceService;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
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

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private ResourceSequenceService resourceSequenceService;

    @Autowired
    private ResourceLinkService resourceLinkService;

    @Transactional(rollbackFor = Exception.class)
    public ProjectResource addProjectResource(ProjectResource record, String projectPath) {
        ProjectResource projectResource = projectResourceService.getByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            return projectResource;
        }
        int code = projectResourceSequenceService.generateProjectResourceCode(record.getProjectId());
        record.setCode(String.valueOf(code));
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedAt(record.getCreatedAt());
        record.setUpdatedBy(record.getCreatedBy());
        record.setDeletedBy(0);
        record.setResourceUrl(projectResourceLinkService.getResourceLink(record, projectPath));
        projectResourceService.insert(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectResource addResource(ProjectResource record) throws CoreException {
        ProjectResource projectResource = projectResourceService.getByScopeIdAndScopeTypeAndTypeAndTarget(record.getProjectId(),
                record.getScopeType(), record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            return projectResource;
        }
        String resourceLink;
        String code;
        if (ScopeTypeEnum.PROJECT.value().equals(record.getScopeType())) {
            String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
            code = String.valueOf(projectResourceSequenceService.generateProjectResourceCode(record.getProjectId()));
            record.setCode(code);
            resourceLink = projectResourceLinkService.getResourceLink(record, projectPath);
        } else if (ScopeTypeEnum.TEAM.value().equals(record.getScopeType())) {
            code = resourceSequenceService.generateResourceCode(record.getProjectId(), record.getScopeType(), record.getTargetType());
            record.setCode(code);
            resourceLink = resourceLinkService.getResourceLink(record);
        } else {
            throw CoreException.of(CoreException.ExceptionType.GLOBAL_RESOURCE_SCOPE_TYPE_NOT_EXIST);
        }

        record.setCode(code);
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedAt(record.getCreatedAt());
        record.setUpdatedBy(record.getCreatedBy());
        record.setDeletedBy(0);
        record.setResourceUrl(resourceLink);
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

        if (Objects.isNull(targetType) || CollectionUtils.isEmpty(targetIdList)) {
            return;
        }
        if (result > 0) {
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

    public List<ProjectResourceDTO> getResourceReferenceMutually(Integer selfProjectId, Integer selfIid, Integer userId) {
        log.warn("ProjectResourceServiceHelper.getResourceReferenceMutually not supported");
        return Collections.emptyList();
    }

    public void deleteResource(int scopeId, int scopeType, String targetType, int targetId, int userId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("targetType", targetType);
        parameters.put("targetId", targetId);
        parameters.put("scopeType", scopeType);
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("deletedBy", userId);
        int result = projectResourceService.delete(parameters);

        if (result > 0) {
            Map<String, Object> map = new HashMap<>();
            map.put("targetScopeType", scopeType);
            map.put("targetType", targetType);
            map.put("targetId", targetId);
            map.put("deletedAt", DateUtil.getCurrentDate());
            resourceReferenceService.delete(map);

            Map<String, Object> selfMap = new HashMap<>();
            selfMap.put("selfScopeType", scopeType);
            selfMap.put("selfType", targetType);
            selfMap.put("selfId", targetId);
            selfMap.put("deletedAt", DateUtil.getCurrentDate());
            resourceReferenceService.delete(selfMap);
        }
    }

    public void recoverResource(int scopeId, int scopeType, String targetType, int targetId, int userId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("targetType", targetType);
        parameters.put("targetId", targetId);
        parameters.put("scopeType", scopeType);
        parameters.put("deletedAt", DateUtil.strToDate("1970-01-01 00:00:00"));
        projectResourceService.recoverResource(parameters);
    }
}
