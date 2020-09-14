package net.coding.app.project.grpc;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public void addProjectResource(ProjectResourceProto.AddProjectResourceRequest request,
                                   StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("addProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getUserId() <= 0) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResource parameters error", null, response);
                return;
            }
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
            ProjectResource resource = projectResourceService.addProjectResource(record);
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "add success", GrpcUtil.getProjectResource(resource), response);
        } catch (Exception ex) {
            log.error("addProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "addProjectResource server error", null, response);
        }
    }

    @Override
    public void updateProjectResource(ProjectResourceProto.UpdateProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("updateProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getUserId() <= 0) {
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
            ProjectResource resource = projectResourceService.updateProjectResource(record);
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
            if (request.getProjectId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetIdList().size() <= 0 || request.getUserId() <= 0) {
                GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "parameters project error", response);
                return;
            }
            projectResourceService.deleteProjectResource(request.getProjectId(), request.getTargetType(), request.getTargetIdList(), request.getUserId());
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
            Integer pageSize = request.getPageRequest().getPageSize() > 0 ? request.getPageRequest().getPageSize() : 20;
            Integer projectId = request.getProjectId();
            PageInfo<ProjectResource> pageResult = projectResourceService.findProjectResourceList(projectId, page, pageSize);
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
            GrpcUtil.findProjectResourceResponse(CodeProto.Code.SUCCESS, "findProjectResourcesList success",
                    pageInfo, GrpcUtil.getProjectResourceList(projectResourceList), response);
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
            if(request.getProjectId() <= 0 || request.getCodeList().size() <= 0) {
                GrpcUtil.batchProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "batchProjectResourceList parameters error", null, response);
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchProjectResourceList(request.getProjectId(), request.getCodeList());
            GrpcUtil.batchProjectResourceResponse(CodeProto.Code.SUCCESS, "batchProjectResourceList success",
                    GrpcUtil.getProjectResourceList(projectResourceList), response);
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
            Integer newCode = projectResourceService.generateCodes(request.getProjectId(), request.getCodeAmount());
            ProjectResourceProto.MultiResourceSequence multiResourceSequence = ProjectResourceProto.MultiResourceSequence.newBuilder()
                    .setProjectId(request.getProjectId())
                    .setStartCode(newCode - request.getCodeAmount())
                    .setEndCode(newCode)
                    .build();
            GrpcUtil.multiCodeResponse(CodeProto.Code.SUCCESS, "generateCodes success", multiResourceSequence, response);
        } catch (Exception ex) {
            log.error("generateCodes() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.multiCodeResponse(CodeProto.Code.UNRECOGNIZED, "generateCodes service error", null, response);
        }
    }

    @Override
    public void relateResource(ProjectResourceProto.UpdateProjectResourceRequest request, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("relateResource() grpc service receive: {}", request != null ? request.toString() : "");
        Project project = projectService.getById(request.getProjectId());
        if (Objects.isNull(project)) {
            GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "relateResource project not exists", null, response);
            return;
        }
        try {
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(request.getTargetId());
            record.setTargetType(request.getTargetType());
            record.setCreatedBy(request.getUserId());
            record.setCode(request.getCode());
            ProjectResource projectResource = projectResourceService.relateProjectResource(record);
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "relateResource success", GrpcUtil.getProjectResource(projectResource), response);
        } catch (Exception ex) {
            log.error("relateResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "relateResource service error", null, response);
        }
    }

    @Override
    public void batchRelateResource(ProjectResourceProto.BatchRelateResourceRequest request,
                                    StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("batchRelateResource() grpc service receive: {}", request != null ? request.toString() : "");
        if(Objects.isNull(request.getUpdateProjectResourceRequestList()) || request.getUpdateProjectResourceRequestList().size() <= 0) {
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource param error", response);
            return;
        }
        try {
            List<ProjectResource> projectResourceList = new ArrayList<>();
            List<Integer> codeList = new ArrayList<>();
            Integer projectId = request.getUpdateProjectResourceRequestList().get(0).getProjectId();
            request.getUpdateProjectResourceRequestList().stream().forEach(projectResource -> {
                if (!Objects.equals(projectId, projectResource.getProjectId())) {
                    GrpcUtil.projectResourceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchRelateResource projectId disaccord", response);
                    return;
                }
                ProjectResource item = new ProjectResource();
                item.setProjectId(projectResource.getProjectId());
                item.setTitle(projectResource.getTitle());
                item.setTargetId(projectResource.getTargetId());
                item.setTargetType(projectResource.getTargetType());
                item.setCreatedBy(projectResource.getUserId());
                item.setCode(projectResource.getCode());
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
            log.error("batchRelateResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceCommonResponse(CodeProto.Code.UNRECOGNIZED, "batchRelateResource service error", response);
        }
    }

    @Override
    public void getProjectResourceByTypeAndTarget(ProjectResourceProto.TypeAndTargetRequest request,
                                                  StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("getProjectResourceByTypeAndTarget() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if (request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.projectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "generateCodes parameters error", null, response);
                return;
            }
            ProjectResource resource = projectResourceService.findByProjectIdAndTypeAndTarget(request.getProjectId(), request.getTargetId(), request.getTargetType());
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceByTypeAndTarget success", GrpcUtil.getProjectResource(resource), response);
        } catch (Exception ex) {
            log.error("getProjectResourceByTypeAndTarget() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.projectResourceResponse(CodeProto.Code.UNRECOGNIZED, "getProjectResourceByTypeAndTarget service error", null, response);
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
            GrpcUtil.projectResourceResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", GrpcUtil.getProjectResource(resource), response);
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
            String url = projectResourceService.getResourceLink(request.getProjectResourceId());
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.SUCCESS, "getProjectResourceByCode success", url, response);
        } catch (Exception ex) {
            log.error("getResourceLink() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.getResourceLinkResponse(CodeProto.Code.UNRECOGNIZED, "getResourceLink service error", null, response);
        }
    }
}
