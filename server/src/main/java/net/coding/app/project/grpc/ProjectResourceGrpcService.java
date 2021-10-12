package net.coding.app.project.grpc;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.app.project.utils.RedissonLockUtil;
import net.coding.exchange.dto.team.Team;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dto.ProjectResourceDTO;
import net.coding.lib.project.dto.ResourceDetailDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ProjectResourceSequence;
import net.coding.lib.project.enums.ScopeTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.helper.ProjectResourceServiceHelper;
import net.coding.lib.project.service.ExternalLinkService;
import net.coding.lib.project.service.ProjectResourceLinkService;
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

import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;

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

    private final String module = "platform-project";

    private final String service = "ProjectResourceGrpcService";

    @Autowired
    private ProjectResourceLinkService projectResourceLinkService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private ExternalLinkService externalLinkService;

    @Autowired
    private TeamServiceGrpcClient teamServiceGrpcClient;

    @Override
    public void addProjectResource(ProjectResourceProto.AddProjectResourceRequest request,
                                   StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        if (request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResource parameters error", null, response);
            return;
        }
        String lockKey = "addProjectResource_" + request.getProjectId() + "_" + request.getTargetId() + "_" + request.getTargetType();
        try {
            if (redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
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
                String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
                ProjectResource resource = projectResourceServiceHelper.addProjectResource(record, projectPath);
                if (Objects.nonNull(resource.getId()) && resource.getId().compareTo(0) > 0) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "add success", GrpcUtil.getProjectResource(resource), response);
                } else {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "addProjectResource add error", null, response);
                }
                redissonLockUtil.unlock(lockKey);
            }
        } catch (Exception ex) {
            log.error("addProjectResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "addProjectResource server error", null, response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void updateProjectResource(ProjectResourceProto.OperateProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateProjectResource parameters error", null, response);
                return;
            }
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(request.getTargetId());
            record.setTargetType(request.getTargetType());
            record.setUpdatedBy(request.getUserId());
            record.setCode(String.valueOf(request.getCode()));
            String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
            record.setResourceUrl(projectResourceLinkService.getResourceLink(record, projectPath));
            ProjectResource resource = projectResourceService.updateProjectResource(record);
            if (Objects.nonNull(resource)) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "update success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "updateProjectResource server error", null, response);
            }
        } catch (Exception ex) {
            log.error("updateProjectResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "updateProjectResource server error", null, response);
        }
    }

    @Override
    public void deleteProjectResource(ProjectResourceProto.DeleteProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        try {
            if (request.getProjectId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetIdList().size() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            projectResourceServiceHelper.deleteProjectResource(request.getProjectId(), request.getTargetType(), request.getTargetIdList(), request.getUserId());
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
        } catch (Exception ex) {
            log.error("deleteProjectResource fail parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "deleteProjectResource server error", response);
        }
    }

    @Override
    public void findProjectResourceList(ProjectResourceProto.FindProjectResourceRequest request,
                                        StreamObserver<ProjectResourceProto.FindProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0) {
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
            if (projectResourceList.size() > 0) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                projectResourceList.stream().forEach(projectResource -> {
                    projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
                });
                GrpcUtil.findProjectResourceResponse(CodeProto.Code.SUCCESS, "findProjectResourcesList success",
                        pageInfo, GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.findProjectResourceResponse(CodeProto.Code.NOT_FOUND, "findProjectResourcesList result null",
                        null, null, response);
            }
        } catch (Exception ex) {
            log.error("findProjectResourcesList fail parameter is " + request.toString(), ex);
            GrpcUtil.findProjectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "findProjectResourcesList service error",
                    null, null, response);
        }
    }

    @Override
    public void batchProjectResourceList(ProjectResourceProto.BatchProjectResourceRequest request,
                                         StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || CollectionUtils.isEmpty(request.getCodeList())) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "batchProjectResourceList parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchProjectResourceList(request.getProjectId(), request.getCodeList());
            if (Objects.nonNull(projectResourceList) && projectResourceList.size() > 0) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                projectResourceList.stream().forEach(projectResource -> {
                    projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
                });
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchProjectResourceList success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND, "batchProjectResourceList not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchProjectResourceList fail parameter is " + request.toString(), ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "batchProjectResourceList service error", null, response);
        }

    }

    @Override
    public void generateCodes(ProjectResourceProto.GenerateRequest request, StreamObserver<ProjectResourceProto.MultiCodeResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getCodeAmount() <= 0) {
                GrpcUtil.multiCodeResponse(CodeProto.Code.INVALID_PARAMETER, "generateCodes parameters error", null, response);
                return;
            }
            Integer newCode = projectResourceServiceHelper.generateCodes(request.getProjectId(), request.getCodeAmount());
            ProjectResourceProto.MultiResourceSequence multiResourceSequence = ProjectResourceProto.MultiResourceSequence.newBuilder()
                    .setProjectId(request.getProjectId())
                    .setStartCode(newCode - request.getCodeAmount() + 1)
                    .setEndCode(newCode)
                    .build();
            GrpcUtil.multiCodeResponse(CodeProto.Code.SUCCESS, "generateCodes success", multiResourceSequence, response);
        } catch (Exception ex) {
            log.error("generateCodes fail, parameter is " + request.toString(), ex);
            GrpcUtil.multiCodeResponse(CodeProto.Code.INTERNAL_ERROR, "generateCodes service error", null, response);
        }
    }

    @Override
    public void relateResource(ProjectResourceProto.OperateProjectResourceRequest request, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        Project project = projectService.getById(request.getProjectId());
        if (Objects.isNull(project)) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "relateResource project not exists", null, response);
            return;
        }
        if (request.getCode() <= 0) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "parameters code error", null, response);
            return;
        }
        ProjectResource projectResource = projectResourceService.getByProjectIdAndCode(request.getProjectId(), request.getCode());
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
                record.setCode(String.valueOf(request.getCode()));
                record.setResourceUrl(request.getResourceUrl());
                if (StringUtils.isBlank(record.getResourceUrl())) {
                    String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
                    record.setResourceUrl(projectResourceLinkService.getResourceLink(record, projectPath));
                }
                ProjectResource result = projectResourceService.relateProjectResource(record);
                if (Objects.nonNull(result)) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "relateResource success", GrpcUtil.getProjectResource(result), response);
                } else {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "relateResource faild", null, response);
                }
            } catch (Exception ex) {
                log.error("relateResource fail, parameter is " + request.toString(), ex);
                GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "relateResource service error", null, response);
            }
        }
    }

    @Override
    public void batchRelateResource(ProjectResourceProto.BatchRelateResourceRequest request,
                                    StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        if (CollectionUtils.isEmpty(request.getOperateProjectResourceRequestList())) {
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
                if (projectResource.getCode() <= 0) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters code error", response);
                    return;
                }
                if (codeMap.containsKey(projectResource.getCode())) {
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
                item.setCode(String.valueOf(projectResource.getCode()));
                item.setResourceUrl(projectResource.getResourceUrl());
                item.setCreatedAt(DateUtil.getCurrentDate());
                item.setUpdatedBy(item.getCreatedBy());
                item.setUpdatedAt(item.getCreatedAt());
                item.setDeletedBy(0);
                projectResourceList.add(item);
                codeList.add(projectResource.getCode());
            });
            int countCodes = projectResourceService.countByProjectIdAndCodes(projectId, codeList);
            if (countCodes > 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource projectId code has exists", response);
                return;
            }
            projectResourceService.batchRelateResource(projectResourceList);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "batchRelateResource success", response);
        } catch (Exception ex) {
            log.error("batchRelateResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchRelateResource service error", response);
        }
    }

    @Override
    public void getByProjectAndTypeAndTarget(ProjectResourceProto.ProjectAndTypeAndTargetRequest request,
                                             StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "generateCodes parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.getByProjectIdAndTypeAndTarget(request.getProjectId(), request.getTargetId(), request.getTargetType());
            if (Objects.nonNull(resource)) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                resource.setResourceUrl(projectResourceLinkService.getResourceLink(resource, projectPath));
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getByProjectAndTypeAndTarget success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getByProjectAndTypeAndTarget not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getByProjectAndTypeAndTarget fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "getByProjectAndTypeAndTarget service error", null, response);
        }
    }

    @Override
    public void getProjectResourceByCode(ProjectResourceProto.GetProjectResourceRequest request,
                                         StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceByCode parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.getByProjectIdAndCode(request.getProjectId(), request.getCode());
            if (Objects.nonNull(resource)) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                resource.setResourceUrl(projectResourceLinkService.getResourceLink(resource, projectPath));
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getProjectResourceByCode not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getProjectResourceByCode fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "getProjectResourceByCode service error", null, response);
        }
    }

    @Override
    public void getResourceLink(ProjectResourceProto.GetResourceLinkRequest request,
                                StreamObserver<ProjectResourceProto.GetResourceLinkResponse> response) {
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "getResourceLink parameters error", null, response);
                return;
            }
//            String url = codingProjectResourceGrpcClient.getResourceLink(request.getProjectResourceId());
//            if(StringUtils.isEmpty(url)) {
//                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
//                return;
//            }
//            log.info("getProjectResourceByCode() url={}", url);
//            GrpcUtil.getResourceLinkResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", url, response);

            ProjectResource projectResource = projectResourceService.getById(request.getProjectResourceId());
            if (Objects.isNull(projectResource)) {
                GrpcUtil.getResourceLinkResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
                return;
            }
            String projectPath = projectGrpcClient.getProjectPath(projectResource.getProjectId());
            String url = projectResourceLinkService.getResourceLink(projectResource, projectPath);
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", url, response);
        } catch (Exception ex) {
            log.error("getResourceLink fail, parameter is " + request.toString(), ex);
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.INTERNAL_ERROR, "getResourceLink service error", null, response);
        }
    }

    @Override
    public void recoverProjectResource(ProjectResourceProto.RecoverProjectResourceRequest request,
                                       StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "recoverProjectResource parameters error", response);
                return;
            }
            ProjectResource projectResource = projectResourceService.getById(request.getProjectResourceId());
            if (Objects.isNull(projectResource)) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", response);
                return;
            }
            ProjectResource record = new ProjectResource();
            record.setId(request.getProjectResourceId());
            record.setDeletedAt(DateUtil.strToDate("1970-01-01 00:00:00"));
            projectResourceService.updateProjectResource(record);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "recoverProjectResource success", response);
        } catch (Exception ex) {
            log.error("recoverProjectResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "recoverProjectResource service error", response);
        }
    }

    @Override
    public void addProjectResourceSequence(ProjectResourceProto.AddProjectResourceSequenceRequest request,
                                           StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        if (request.getProjectId() <= 0) {
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "recoverProjectResource parameters error", response);
            return;
        }
        String lockKey = "addProjectResourceSequence_" + request.getProjectId();
        try {
            if (redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
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
            log.error("addProjectResourceSequence fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "addProjectResourceSequence service error", response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void getProjectResourceById(ProjectResourceProto.GetResourceRequest request,
                                       StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (request.getProjectResourceId() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceById parameters error", null, response);
                return;
            }
            ProjectResource projectResource = projectResourceService.getById(request.getProjectResourceId());
            if (Objects.isNull(projectResource)) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "projectResource not exists", null, response);
                return;
            }
            String projectPath = projectGrpcClient.getProjectPath(projectResource.getProjectId());
            projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceById success", GrpcUtil.getProjectResource(projectResource), response);
        } catch (Exception ex) {
            log.error("getProjectResourceById fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "getProjectResourceById service error", null, response);
        }
    }

    @Override
    public void batchListByProjectAndTypeAndTargets(ProjectResourceProto.PorjectAndTypeAndTargetsRequest request,
                                                    StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getTargetType() == null || CollectionUtils.isEmpty(request.getTargetIdList())) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER,
                        "batchListByProjectAndTypeAndTargets parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchListByProjectAndTypeAndTargets(request.getProjectId(), request.getTargetIdList(), request.getTargetType());
            if (!CollectionUtils.isEmpty(projectResourceList)) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                projectResourceList.forEach(projectResource -> {
                    projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
                });
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchListByProjectAndTypeAndTargets success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND,
                        "batchListByProjectAndTypeAndTargets not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchListByProjectAndTypeAndTargets fail, parameter is " + request.toString(), ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INTERNAL_ERROR,
                    "batchListByProjectAndTypeAndTargets service error", null, response);
        }
    }

    @Override
    public void batchListByTypeAndTargets(ProjectResourceProto.TypeAndTargetsRequest request,
                                          StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        try {
            if (request.getTargetType() == null || request.getTargetIdList().size() <= 0) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER,
                        "batchListByTypeAndTargets parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchListByTypeAndTargets(request.getTargetType(), request.getTargetIdList());
            if (!CollectionUtils.isEmpty(projectResourceList)) {
                projectResourceList.forEach(projectResource -> {
                    String projectPath = projectGrpcClient.getProjectPath(projectResource.getProjectId());
                    projectResource.setResourceUrl(projectResourceLinkService.getResourceLink(projectResource, projectPath));
                });
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchListByTypeAndTargets success",
                        GrpcUtil.getProjectResourceList(projectResourceList), response);
            } else {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.NOT_FOUND,
                        "batchListByTypeAndTargets not found", null, response);
            }
        } catch (Exception ex) {
            log.error("batchListByTypeAndTargets fail, parameter is " + request.toString(), ex);
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INTERNAL_ERROR,
                    "batchListByTypeAndTargets service error", null, response);
        }
    }

    @Override
    public void getProjectResourceWithDeleted(ProjectResourceProto.GetProjectResourceRequest request,
                                              StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getProjectResourceByCode parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.getProjectResourceWithDeleted(request.getProjectId(), request.getCode());
            if (Objects.nonNull(resource)) {
                String projectPath = projectGrpcClient.getProjectPath(request.getProjectId());
                resource.setResourceUrl(projectResourceLinkService.getResourceLink(resource, projectPath));
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceWithDeleted success", GrpcUtil.getProjectResource(resource), response);
            } else {
                GrpcUtil.projectResourceResponse(CodeProto.Code.NOT_FOUND, "getProjectResourceWithDeleted not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getProjectResourceWithDeleted fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "getProjectResourceWithDeleted service error", null, response);
        }
    }

    @Override
    public void findProjectResourceMutuallyList(ProjectResourceProto.FindProjectResourceMutuallyRequest request,
                                                StreamObserver<ProjectResourceProto.FindProjectResourceMutuallyResponse> response) {
        try {
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.FindProjectResourceMutuallyResponse(CodeProto.Code.INVALID_PARAMETER,
                        "findProjectResourceMutuallyList parameters error", null, response);
                return;
            }
            List<ProjectResourceDTO> result = projectResourceServiceHelper.getResourceReferenceMutually(request.getProjectId(), request.getCode(), request.getUserId());
            if (!CollectionUtils.isEmpty(result)) {
                GrpcUtil.FindProjectResourceMutuallyResponse(CodeProto.Code.SUCCESS, "findProjectResourceMutuallyList success", result, response);
            } else {
                GrpcUtil.FindProjectResourceMutuallyResponse(CodeProto.Code.NOT_FOUND, "findProjectResourceMutuallyList not found", null, response);
            }
        } catch (Exception ex) {
            log.error("getProjectResourceWithDeleted fail, parameter is " + request.toString(), ex);
            GrpcUtil.FindProjectResourceMutuallyResponse(CodeProto.Code.INTERNAL_ERROR,
                    "findProjectResourceMutuallyList service error", null, response);
        }
    }

    @Override
    public void batchUpdateProjectResource(ProjectResourceProto.BatchUpdateProjectResourceRequest request,
                                           StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        try {
            if (request.getProjectId() <= 0 || CollectionUtils.isEmpty(request.getTargetIdList())
                    || StringUtils.isEmpty(request.getCurrentTargetType()) || StringUtils.isEmpty(request.getTargetTargetType())) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            if (request.getTargetIdList().size() > 1000) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "targetIds too long", response);
                return;
            }
            projectResourceService.batchUpdateProjectResource(request.getProjectId(), request.getTargetIdList(),
                    request.getCurrentTargetType(), request.getTargetTargetType(), request.getUserId());
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
        } catch (Exception ex) {
            log.error("batchUpdateProjectResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchUpdateProjectResource server error", response);
        }
    }

    @Override
    public void addResource(ProjectResourceProto.AddResourceRequest request, StreamObserver<ProjectResourceProto.ResourceResponse> response) {
        if(request.getScopeId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
            GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "addResource parameters error", null, response);
            return;
        }
        Integer scopeType = request.getScopeType() == 0 ? 1 : request.getScopeType();
        String lockKey = String.format("addResource:scopeId:%d:scopeType:%d:targetId:%d:targetType:%s", request.getScopeId(), scopeType, request.getTargetId(), request.getTargetType());
        try {
            if(redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
                if(ScopeTypeEnum.PROJECT.value().equals(scopeType)){
                    Project project = projectService.getById(request.getScopeId());
                    if (Objects.isNull(project)) {
                        GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "addResource project not exists", null, response);
                        return;
                    }
                } else if(ScopeTypeEnum.TEAM.value().equals(scopeType)){
                    Team team = teamServiceGrpcClient.getTeam(request.getScopeId());
                    if (Objects.isNull(team)) {
                        GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "addResource team not exists", null, response);
                        return;
                    }
                }

                ProjectResource record = new ProjectResource();
                record.setProjectId(request.getScopeId());
                record.setTitle(request.getTitle());
                record.setTargetId(request.getTargetId());
                record.setTargetType(request.getTargetType());
                record.setCreatedBy(request.getUserId());
                //设置资源覆盖范围 scopeType
                record.setScopeType(scopeType);
                ProjectResource resource = projectResourceServiceHelper.addResource(record);
                if(Objects.nonNull(resource.getId()) && resource.getId().compareTo(0) > 0) {
                    GrpcUtil.resourceResponse(CodeProto.Code.SUCCESS, "addResource success", GrpcUtil.getResource(resource), response);
                } else {
                    GrpcUtil.resourceResponse(CodeProto.Code.INTERNAL_ERROR, "addResource error", null, response);
                }
                redissonLockUtil.unlock(lockKey);
            }
        } catch (Exception ex) {
            log.error("addResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.resourceResponse(CodeProto.Code.INTERNAL_ERROR, "addResource server error", null, response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void updateResource(ProjectResourceProto.UpdateResourceRequest request, StreamObserver<ProjectResourceProto.ResourceResponse> response) {
        if(request.getScopeId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
            GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateResource parameters error", null, response);
            return;
        }
        String lockKey = String.format("updateResource:scopeId:%d:scopeType:%d:targetId:%d:targetType:%s", request.getScopeId(), request.getScopeType(),request.getTargetId(), request.getTargetType());
        try {
            if(redissonLockUtil.tryLock(lockKey, TimeUnit.MILLISECONDS, 1000, 2000)) {
                if(ScopeTypeEnum.PROJECT.value().equals(request.getScopeType())){
                    Project project = projectService.getById(request.getScopeId());
                    if (Objects.isNull(project)) {
                        GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateResource project not exists", null, response);
                        return;
                    }
                } else if(ScopeTypeEnum.TEAM.value().equals(request.getScopeType())){
                    Team team = teamServiceGrpcClient.getTeam(request.getScopeId());
                    if (Objects.isNull(team)) {
                        GrpcUtil.resourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateResource team not exists", null, response);
                        return;
                    }
                }

                ProjectResource record = projectResourceService.getByScopeIdAndScopeTypeAndTypeAndTarget(request.getScopeId(),
                        request.getScopeType(), request.getTargetId(), request.getTargetType());
                if(record == null){
                    throw CoreException.of(CoreException.ExceptionType.RESOURCE_NO_FOUND);
                }
                record.setTitle(request.getTitle());
                record.setUpdatedBy(request.getUserId());
                record.setUpdatedAt(DateUtil.getCurrentDate());
                int result = projectResourceService.update(record);
                if(result > 0) {
                    GrpcUtil.resourceResponse(CodeProto.Code.SUCCESS, "updateResource success", GrpcUtil.getResource(record), response);
                } else {
                    GrpcUtil.resourceResponse(CodeProto.Code.INTERNAL_ERROR, "updateResource error", null, response);
                }
                redissonLockUtil.unlock(lockKey);
            }
        } catch (Exception ex) {
            log.error("updateResource fail, parameter is " + request.toString(), ex);
            GrpcUtil.resourceResponse(CodeProto.Code.INTERNAL_ERROR, "updateResource server error", null, response);
            redissonLockUtil.unlock(lockKey);
        }
    }

    @Override
    public void deleteResource(ProjectResourceProto.DeleteResourceRequest request, StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        try {
            if (request.getScopeId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetId() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            projectResourceServiceHelper.deleteResource(request.getScopeId(), request.getScopeType(), request.getTargetType(), request.getTargetId(), request.getUserId());
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "deleteResource success", response);
        } catch (Exception ex) {
            log.error("deleteProjectResource fail parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "deleteResource server error", response);
        }
    }

    @Override
    public void recoverResource(ProjectResourceProto.RecoverResourceRequest request, StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        try {
            if (request.getScopeId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetId() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            projectResourceServiceHelper.recoverResource(request.getScopeId(), request.getScopeType(), request.getTargetType(), request.getTargetId(), request.getUserId());
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.SUCCESS, "recoverResource success", response);
        } catch (Exception ex) {
            log.error("deleteProjectResource fail parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "recoverResource server error", response);
        }
    }

    @Override
    public void getResourceByScopeIdAndCode(ProjectResourceProto.GetResourceByScopeIdAndCodeRequest request, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        try {
            if (ScopeTypeEnum.PROJECT.value() == request.getScopeType()) {
                //项目资源
                Project project = projectService.getById(request.getScopeId());
                if (project == null) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getResourceByScopeIdAndCode project not exists", null, response);
                    return;
                }
                ProjectResource projectResource = projectResourceService.findProjectResourceDetail(project.getId(), request.getCode(), ScopeTypeEnum.PROJECT.value());
                if (projectResource == null) {
                    throw CoreException.of(RESOURCE_NO_FOUND);
                }
                ResourceDetailDTO resourceDetailDTO = new ResourceDetailDTO(projectResource);
                resourceDetailDTO.setScopeName(project.getDisplayName());
                resourceDetailDTO.setScopeAvatar(project.getIcon());
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getResourceByScopeIdAndCode success", GrpcUtil.getProjectResource(projectResource), response);
            } else if (ScopeTypeEnum.TEAM.value() == request.getScopeType()){
                Team team = teamServiceGrpcClient.getTeam(request.getScopeId());
                if (team == null) {
                    GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "getResourceByScopeIdAndCode team not exists", null, response);
                    return;
                }
                // 全局资源
                ProjectResource projectResource = projectResourceService.findProjectResourceDetail(team.getId(), request.getCode(), ScopeTypeEnum.TEAM.value());
                if (projectResource == null) {
                    throw CoreException.of(RESOURCE_NO_FOUND);
                }
                GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getResourceByScopeIdAndCode success", GrpcUtil.getProjectResource(projectResource), response);
            }

        } catch (Exception ex){
            log.error("getResourceByScopeIdAndCode fail, parameter is " + request.toString(), ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.INTERNAL_ERROR, "getResourceByScopeIdAndCode server error", null, response);
        }
    }

}
