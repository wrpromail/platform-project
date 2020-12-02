package net.coding.lib.project.helper;

import com.google.gson.Gson;

import net.coding.e.proto.FileProto;
import net.coding.e.proto.wiki.WikiProto;
import net.coding.lib.project.dto.ProjectResourceDTO;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ResourceReference;
import net.coding.lib.project.enums.ResourceTypeEnum;
import net.coding.lib.project.grpc.client.FileServiceGrpcClient;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.StorageGrpcClient;
import net.coding.lib.project.grpc.client.WikiGrpcClient;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ResourceReferenceCommentRelationService;
import net.coding.lib.project.service.ResourceReferenceService;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import proto.platform.project.ProjectProto;

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
    private WikiGrpcClient wikiGrpcClient;

    @Autowired
    private ResourceReferenceCommentRelationService resourceReferenceCommentRelationService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private FileServiceGrpcClient fileServiceGrpcClient;

    @Autowired
    private StorageGrpcClient storageGrpcClient;

    @Value("${coding-net-public-image:coding-static-bucket-public}")
    private String bucket;

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

        if(Objects.isNull(targetType) || CollectionUtils.isEmpty(targetIdList)) {
            return;
        }
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

    public List<ProjectResourceDTO> getResourceReferenceMutually(Integer selfProjectId, Integer selfIid, Integer userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("selfProjectId", selfProjectId);
        map.put("selfIid", selfIid);
        map.put("userId", userId);
        Gson gson = new Gson();
        List<ResourceReference> resourceReferenceList = resourceReferenceService.getResourceReferenceMutually(map)
                .stream()
                .filter(record -> {
                    if(ResourceTypeEnum.Wiki.getType().equals(record.getTargetType())) {
                        WikiProto.GetWikiByProjectIdAndIidData wiki = wikiGrpcClient.getWikiByProjectIdAndIidWithoutRecycleBin(record.getTargetProjectId(), record.getTargetIid());
                        if(wiki == null) {
                            return false;
                        }
                        return wikiGrpcClient.wikiCanRead(userId, wiki.getProjectId(), wiki.getIid());
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toList());
        List<ProjectResourceDTO> projectResourceDTOList = new ArrayList<>();
        resourceReferenceList.forEach(resourceReference -> {
            if(Objects.nonNull(resourceReference)) {
                Integer projectId = resourceReference.getTargetProjectId();
                Integer code = resourceReference.getTargetIid();
                ProjectResource projectResource = projectResourceService.getProjectResourceWithDeleted(projectId, code);
                if(Objects.nonNull(projectResource)) {
                    ProjectProto.Project project = projectGrpcClient.getProjectById(projectResource.getProjectId());
                    if(Objects.nonNull(project)) {
                        String link = projectResourceLinkService.getResourceLink(projectResource, project.getProjectPath());
                        projectResource.setResourceUrl(link);
                        ProjectResourceDTO projectResourceDTO =new ProjectResourceDTO(projectResource,
                                project.getName(), project.getDisplayName());

                        if ("ProjectFile".equals(projectResource.getTargetType())) {
                            FileProto.File file = fileServiceGrpcClient.getProjectFileByIdWithDel(projectId, projectResource.getTargetId());
                            if(file.getType() == 2 && Objects.nonNull(file.getStorageType())) {
                                String imagePreviewUrl = storageGrpcClient.getImagePreviewUrl(file.getStorageKey(),
                                        bucket, 1,150, 150, file.getStorageType());
                                projectResourceDTO.setImg(imagePreviewUrl);
                            }
                        }

                        boolean hasCommentRelated = false;
                        if(resourceReferenceCommentRelationService.countComment(resourceReference.getId()) > 0) {
                            hasCommentRelated = true;
                        }
                        projectResourceDTO.setHasCommentRelated(hasCommentRelated);
                        projectResourceDTOList.add(projectResourceDTO);
                    }
                }
            }
        });
        return projectResourceDTOList;
    }

}
