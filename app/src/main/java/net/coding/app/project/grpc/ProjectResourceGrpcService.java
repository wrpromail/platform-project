package net.coding.app.project.grpc;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.app.project.utils.RedissonLockUtil;
import net.coding.client.project.CodingProjectResourceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ProjectResourceSequence;
import net.coding.lib.project.helper.ProjectResourceServiceHelper;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.common.PagerProto;
import proto.projectResource.ProjectResourceProto;
import proto.projectResource.ProjectResourceServiceGrpc;

@Slf4j
@GRpcService
public class ProjectResourceGrpcService extends ProjectResourceServiceGrpc.ProjectResourceServiceImplBase {

    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectResourceService projectResourceService;

    @Resource
    private ProjectResourceSequenceService projectResourceSequenceService;

    @Resource
    private ProjectResourceServiceHelper projectResourceServiceHelper;

    @Autowired
    private RedissonLockUtil redissonLockUtil;

    @Autowired
    private CodingProjectResourceGrpcClient codingProjectResourceGrpcClient;

    @Override
    public void addProjectResource(ProjectResourceProto.AddProjectResourceRequest request,
                                   StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("addProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        if(request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResource parameters error", null, response);
            return;
        }
        String lockKey = "addProjectResource_" + request.getProjectId() + "_" + request.getTargetId() + "_" + request.getTargetType();
        try {
            if(redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
                Project project = projectService.getById(request.getProjectId());
                if (Objects.isNull(project)) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResource project not exists", null, response);
                    return;
                }
                ProjectResource record = new ProjectResource();
                record.setProjectId(request.getProjectId());
                record.setTitle(request.getTitle());
                record.setTargetId(request.getTargetId());
                record.setTargetType(request.getTargetType());
                record.setCreatedBy(request.getUserId());
                record.setResourceUrl(request.getResourceUrl());
                log.info("addProjectResource() record= {}", record.toString());
                ProjectResource resource = projectResourceServiceHelper.addProjectResource(record);
                log.info("addProjectResource() resource= {}", resource.toString());
                if(Objects.nonNull(resource.getId()) && resource.getId().compareTo(0) > 0) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "add success", GrpcUtil.getProjectResource(resource), response);
                } else {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "addProjectResource add error", null, response);
                }
                redissonLockUtil.unlock(lockKey);
            }
        } catch (Exception ex) {
            log.error("addProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "addProjectResource server error", null, response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void updateProjectResource(ProjectResourceProto.OperateProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("updateProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateProjectResource parameters error", null, response);
                return;
            }
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(request.getTargetId());
            record.setTargetType(request.getTargetType());
            record.setUpdatedBy(request.getUserId());
            record.setCode(request.getCode());
            record.setResourceUrl(request.getResourceUrl());
            log.info("updateProjectResource() record= {}", record.toString());
            ProjectResource resource = projectResourceService.updateProjectResource(record);
            log.info("updateProjectResource() resource= {}", record.toString());
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "update success", GrpcUtil.getProjectResource(resource), response);
        } catch (Exception ex) {
            log.error("updateProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "updateProjectResource server error", null, response);
        }
    }

    @Override
    public void deleteProjectResource(ProjectResourceProto.DeleteProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("deleteProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetIdList().size() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            projectResourceServiceHelper.deleteProjectResource(request.getProjectId(), request.getTargetType(), request.getTargetIdList(), request.getUserId());
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
        } catch (Exception ex) {
            log.error("deleteProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.UNRECOGNIZED, "deleteProjectResource server error", response);
        }
    }

    @Override
    public void findProjectResourceList(ProjectResourceProto.FindProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.FindProjectResourceResponse> response) {
        log.info("findProjectResourcesList() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0) {
                GrpcUtil.findProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error",
                        null, null, response);
                return;
            }
            Integer page = request.getPageRequest().getPage() > 0 ? request.getPageRequest().getPage() : 1;
            Integer pageSize = request.getPageRequest().getPageSize() > 0 ? request.getPageRequest().getPageSize() : 10;
            Integer projectId = request.getProjectId();
            PageInfo<ProjectResource> pageResult = projectResourceService.findProjectResourceList(projectId,
                    request.getKeyword(), request.getTargetTypeList(), page, pageSize);
            List<ProjectResource> projectResourceList = pageResult.getList();
            Integer totalPage = pageResult.getPages();
            Integer totalRow = Long.valueOf(pageResult.getTotal()).intValue();
            PagerProto.PageRequest pageRequest = PagerProto.PageRequest.newBuilder()
                    .setPage(page)
                    .setPageSize(pageSize)
                    .build();
            PagerProto.PageInfo pageInfo = PagerProto.PageInfo.newBuilder()
                    .setPageRequest(pageRequest)
                    .setTotalPage(totalPage)
                    .setTotalRow(totalRow)
                    .build();
            if(projectResourceList.size() > 0) {
                log.info("findProjectResourcesList() projectResourceList={}", projectResourceList.toString());
                GrpcUtil.findProjectResourceResponse(CodeProto.Code.SUCCESS, "findProjectResourcesList success",
                        pageInfo, GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.findProjectResourceResponse(CodeProto.Code.NOT_FOUND, "findProjectResourcesList result null",
                        null, null, response);
            }
        } catch (Exception ex) {
            log.error("findProjectResourcesList() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findProjectResourceResponse(CodeProto.Code.UNRECOGNIZED, "findProjectResourcesList service error",
                    null, null, response);
        }
    }

    @Override
    public void batchProjectResourceList(ProjectResourceProto.BatchProjectResourceRequest request,
                                          StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        log.info("batchProjectResourcesList() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || CollectionUtils.isEmpty(request.getCodeList())) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "batchProjectResourceList parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchProjectResourceList(request.getProjectId(), request.getCodeList());
            if(Objects.nonNull(projectResourceList) && projectResourceList.size() > 0) {
                log.info("batchProjectResourceList() projectResourceList={}", projectResourceList.toString());
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchProjectResourceList success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND, "batchProjectResourceList not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchProjectResourceList() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.UNRECOGNIZED, "batchProjectResourceList service error", null, response);
        }

    }

    @Override
    public void generateCodes(ProjectResourceProto.GenerateRequest request, StreamObserver<ProjectResourceProto.MultiCodeResponse> response) {
        log.info("generateCodes() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getCodeAmount() <= 0) {
                GrpcUtil.multiCodeResponse(CodeProto.Code.INVALID_PARAMETER, "generateCodes parameters error", null, response);
                return;
            }
            Integer newCode = projectResourceServiceHelper.generateCodes(request.getProjectId(), request.getCodeAmount());
            log.info("generateCodes() grpc service newCode: {}", newCode);
            ProjectResourceProto.MultiResourceSequence multiResourceSequence = ProjectResourceProto.MultiResourceSequence.newBuilder()
                    .setProjectId(request.getProjectId())
                    .setStartCode(newCode - request.getCodeAmount() + 1)
                    .setEndCode(newCode)
                    .build();
            GrpcUtil.multiCodeResponse(CodeProto.Code.SUCCESS, "generateCodes success", multiResourceSequence, response);
        } catch (Exception ex) {
            log.error("generateCodes() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.multiCodeResponse(CodeProto.Code.UNRECOGNIZED, "generateCodes service error", null, response);
        }
    }

    @Override
    public void relateResource(ProjectResourceProto.OperateProjectResourceRequest request, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("relateResource() grpc service receive: {}", request != null ? request.toString() : "");
        Project project = projectService.getById(request.getProjectId());
        if (Objects.isNull(project)) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "relateResource project not exists", null, response);
            return;
        }
        if(request.getCode() <= 0) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "parameters code error", null, response);
            return;
        }
        ProjectResource projectResource = projectResourceService.findByProjectIdAndCode(request.getProjectId(), request.getCode());
        if (Objects.nonNull(projectResource)) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "the code has exists", null, response);
            return;
        }

        projectResource = projectResourceService.getByProjectIdAndTypeAndTarget(request.getProjectId(), request.getTargetId(), request.getTargetType());
        if (Objects.nonNull(projectResource)) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "relateResource success", GrpcUtil.getProjectResource(projectResource), response);
            return;
        } else {
            try {
                ProjectResource record = new ProjectResource();
                record.setProjectId(request.getProjectId());
                record.setTitle(request.getTitle());
                record.setTargetId(request.getTargetId());
                record.setTargetType(request.getTargetType());
                record.setCreatedBy(request.getUserId());
                record.setCode(request.getCode());
                record.setResourceUrl(request.getResourceUrl());
                ProjectResource result = projectResourceService.relateProjectResource(record);
                if(Objects.nonNull(result)) {
                    log.info("relateResource() result={}", result.toString());
                    GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "relateResource success", GrpcUtil.getProjectResource(result), response);
                } else {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "relateResource faild", null, response);
                }
            } catch (Exception ex) {
                log.error("relateResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
                GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "relateResource service error", null, response);
            }
        }
    }

    @Override
    public void batchRelateResource(ProjectResourceProto.BatchRelateResourceRequest request,
                                    StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("batchRelateResource() grpc service receive: {}", request != null ? request.toString() : "");
        if(CollectionUtils.isEmpty(request.getOperateProjectResourceRequestList())) {
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource param error", response);
            return;
        }
        try {
            List<ProjectResource> projectResourceList = new ArrayList<>();
            List<Integer> codeList = new ArrayList<>();
            Integer projectId = request.getOperateProjectResourceRequestList().get(0).getProjectId();
            Map<Integer, Integer> codeMap = new HashMap<>();
            request.getOperateProjectResourceRequestList().forEach(projectResource -> {
                if (!Objects.equals(projectId, projectResource.getProjectId())) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource projectId disaccord", response);
                    return;
                }
                if(projectResource.getCode() <= 0) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters code error", response);
                    return;
                }
                if(codeMap.containsKey(projectResource.getCode())) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters code error", response);
                    return;
                } else {
                    codeMap.put(projectResource.getCode(), 0);
                }
                ProjectResource item = new ProjectResource();
                item.setProjectId(projectResource.getProjectId());
                item.setTitle(projectResource.getTitle());
                item.setTargetId(projectResource.getTargetId());
                item.setTargetType(projectResource.getTargetType());
                item.setCreatedBy(projectResource.getUserId());
                item.setCode(projectResource.getCode());
                item.setResourceUrl(projectResource.getResourceUrl());
                item.setCreatedAt(DateUtil.getCurrentDate());
                item.setUpdatedBy(item.getCreatedBy());
                item.setUpdatedAt(item.getCreatedAt());
                item.setDeletedBy(0);
                projectResourceList.add(item);
                codeList.add(projectResource.getCode());
            });
            log.info("batchRelateResource() codeList={}", codeList.toString());
            int countCodes = projectResourceService.countByProjectIdAndCodes(projectId, codeList);
            if (countCodes > 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource projectId code has exists", response);
                return;
            }
            projectResourceService.batchRelateResource(projectResourceList);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "batchRelateResource success", response);
        } catch (Exception ex) {
            log.error("batchRelateResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.UNRECOGNIZED, "batchRelateResource service error", response);
        }
    }

    @Override
    public void getByProjectAndTypeAndTarget(ProjectResourceProto.ProjectAndTypeAndTargetRequest request,
                                                  StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("getByProjectAndTypeAndTarget() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "generateCodes parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.getByProjectIdAndTypeAndTarget(request.getProjectId(), request.getTargetId(), request.getTargetType());
            if(Objects.nonNull(resource)) {
                log.info("getByProjectAndTypeAndTarget() resource={}", resource.toString());
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getByProjectAndTypeAndTarget success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getByProjectAndTypeAndTarget not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getByProjectAndTypeAndTarget() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "getByProjectAndTypeAndTarget service error", null, response);
        }
    }

    @Override
    public void getProjectResourceByCode(ProjectResourceProto.GetProjectResourceRequest request,
                                              StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("getProjectResourceByCode() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceByCode parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.findByProjectIdAndCode(request.getProjectId(), request.getCode());
            if(Objects.nonNull(resource)) {
                log.info("getProjectResourceByCode() resource={}", resource.toString());
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getProjectResourceByCode not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getProjectResourceByCode() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "getProjectResourceByCode service error", null, response);
        }
    }

    @Override
    public void getResourceLink(ProjectResourceProto.GetResourceLinkRequest request,
                                StreamObserver<ProjectResourceProto.GetResourceLinkResponse> response) {
        log.info("getResourceLink() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "getResourceLink parameters error", null, response);
                return;
            }
            String url = codingProjectResourceGrpcClient.getResourceLink(request.getProjectResourceId());
            if(StringUtils.isEmpty(url)) {
                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
                return;
            }
            log.info("getProjectResourceByCode() url={}", url);
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", url, response);
//            ProjectResource projectResource = projectResourceService.selectById(request.getProjectResourceId());
//            if(Objects.isNull(projectResource)) {
//                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
//                return;
//            }
//            log.info("getProjectResourceByCode() projectResource={}", projectResource.toString());
//            GrpcUtil.getResourceLinkResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", projectResource.getResourceUrl(), response);
        } catch (Exception ex) {
            log.error("getResourceLink() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.UNRECOGNIZED, "getResourceLink service error", null, response);
        }
    }

    @Override
    public void recoverProjectResource(ProjectResourceProto.RecoverProjectResourceRequest request,
                                       StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("recoverProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "recoverProjectResource parameters error", response);
                return;
            }
            ProjectResource projectResource = projectResourceService.selectById(request.getProjectResourceId());
            if (Objects.isNull(projectResource)) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", response);
                return;
            }
            ProjectResource record = new ProjectResource();
            record.setId(request.getProjectResourceId());
            record.setDeletedAt(DateUtil.strToDate("1970-01-01 00:00:00"));
            log.info("recoverProjectResource() record={}", record.toString());
            projectResourceService.updateProjectResource(record);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "recoverProjectResource success", response);
        } catch (Exception ex) {
            log.error("recoverProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.UNRECOGNIZED, "recoverProjectResource service error", response);
        }
    }

    @Override
    public void addProjectResourceSequence(ProjectResourceProto.AddProjectResourceSequenceRequest request,
                                           StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("addProjectResourceSequence() grpc service receive: {}", request != null ? request.toString() : "");
        if (request.getProjectId() <= 0) {
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "recoverProjectResource parameters error", response);
            return;
        }
        String lockKey = "addProjectResourceSequence_" + request.getProjectId();
        try {
            if(redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
                ProjectResourceSequence item = projectResourceSequenceService.getByProjectId(request.getProjectId());
                if (Objects.nonNull(item)) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResourceSequence projectId has exists", response);
                    return;
                }
                ProjectResourceSequence projectResourceSequence = new ProjectResourceSequence();
                projectResourceSequence.setProjectId(request.getProjectId());
                projectResourceSequence.setCode(0);
                log.info("addProjectResourceSequence() projectResourceSequence={}", projectResourceSequence.toString());
                projectResourceSequenceService.addProjectResourceSequence(projectResourceSequence);
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "batchRelateResource success", response);
                redissonLockUtil.unlock(lockKey);
            }
        } catch (Exception ex) {
            log.error("addProjectResourceSequence() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.UNRECOGNIZED, "addProjectResourceSequence service error", response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void getProjectResourceById(ProjectResourceProto.GetResourceRequest request,
                                       StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("getProjectResourceById() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceById parameters error", null, response);
                return;
            }
            ProjectResource projectResource = projectResourceService.selectById(request.getProjectResourceId());
            if(Objects.isNull(projectResource)) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
                return;
            }
            log.info("getProjectResourceById() projectResource={}", projectResource.toString());
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceById success", GrpcUtil.getProjectResource(projectResource), response);
        } catch (Exception ex) {
            log.error("getProjectResourceById() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "getProjectResourceById service error", null, response);
        }
    }

    @Override
    public void batchListByProjectAndTypeAndTargets(ProjectResourceProto.PorjectAndTypeAndTargetsRequest request,
                                                        StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        log.info("batchListByProjectAndTypeAndTargets() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getTargetType() == null || CollectionUtils.isEmpty(request.getTargetIdList())) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER,
                        "batchListByProjectAndTypeAndTargets parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchListByProjectAndTypeAndTargets(request.getProjectId(), request.getTargetIdList(), request.getTargetType());
            if(CollectionUtils.isEmpty(projectResourceList)) {
                log.info("batchListByProjectAndTypeAndTargets() projectResourceList={}", projectResourceList.toString());
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchListByProjectAndTypeAndTargets success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND,
                        "batchListByProjectAndTypeAndTargets not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchListByProjectAndTypeAndTargets() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.UNRECOGNIZED,
                    "batchListByProjectAndTypeAndTargets service error", null, response);
        }
    }

    @Override
    public void batchListByTypeAndTargets(ProjectResourceProto.TypeAndTargetsRequest request,
                                                                StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        log.info("batchListByTypeAndTargets() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getTargetType() == null || request.getTargetIdList().size() <= 0) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER,
                        "batchListByTypeAndTargets parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchListByTypeAndTargets(request.getTargetType(), request.getTargetIdList());
            if(CollectionUtils.isEmpty(projectResourceList)) {
                log.info("batchListByTypeAndTargets() projectResourceList={}", projectResourceList.toString());
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchListByTypeAndTargets success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND,
                        "batchListByTypeAndTargets not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchListByTypeAndTargets() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.UNRECOGNIZED,
                    "batchListByTypeAndTargets service error", null, response);
        }
    }

    @Override
    public void getProjectResourceWithDeleted(ProjectResourceProto.GetProjectResourceRequest request,
                                              StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("getProjectResourceWithDeleted() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceByCode parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.getProjectResourceWithDeleted(request.getProjectId(), request.getCode());
            if(Objects.nonNull(resource)) {
                log.info("getProjectResourceWithDeleted() resource={}", resource.toString());
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceWithDeleted success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getProjectResourceWithDeleted not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getProjectResourceWithDeleted() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "getProjectResourceWithDeleted service error", null, response);
        }
    }

}
