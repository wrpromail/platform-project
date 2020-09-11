package net.coding.app.project.grpc;

import com.github.pagehelper.PageInfo;

import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ProjectService;

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
                ProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "addProjectResource parameters error", null, response);
                return;
            }
            Project project = projectService.getById(request.getProjectId());
            if (Objects.isNull(project)) {
                ProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "project not exists", null, response);
                return;
            }
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(request.getTargetId());
            record.setTargetType(request.getTargetType());
            record.setCreatedBy(request.getUserId());
            ProjectResource resource = projectResourceService.addProjectResource(record);
            ProjectResourceResponse(CodeProto.Code.SUCCESS, "add success", getProjectResource(resource), response);
        } catch (Exception ex) {
            log.error("addProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            ProjectResourceResponse(CodeProto.Code.UNRECOGNIZED, "addProjectResource server error", null, response);
        }
    }

    @Override
    public void updateProjectResource(ProjectResourceProto.UpdateProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        log.info("updateProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getTargetId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getUserId() <= 0) {
                ProjectResourceResponse(CodeProto.Code.INVALID_PARAMETER, "updateProjectResource parameters error", null, response);
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
            ProjectResourceResponse(CodeProto.Code.SUCCESS, "update success", getProjectResource(resource), response);
        } catch (Exception ex) {
            log.error("updateProjectResource() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            ProjectResourceResponse(CodeProto.Code.UNRECOGNIZED, "updateProjectResource server error", null, response);
        }
    }

    @Override
    public void deleteProjectResource(ProjectResourceProto.DeleteProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        log.info("deleteProjectResource() grpc service receive: {}", request != null ? request.toString() : "");
        if(request.getProjectId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetIdList().size() <= 0) {
            ProjectResourceProto.ProjectResourceCommonResponse build = ProjectResourceProto.ProjectResourceCommonResponse.newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .setMessage("parameters project error")
                    .build();
            response.onNext(build);
            response.onCompleted();
            return;
        }

        //ProjectResourceResponse(CodeProto.Code.SUCCESS, "delete success", getProjectResource(resource), response);
    }

    @Override
    public void findProjectResourceList(ProjectResourceProto.FindProjectResourceRequest request,
                                      StreamObserver<ProjectResourceProto.FindProjectResourceResponse> response) {
        log.info("findProjectResourcesList() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0) {
                ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                        .setCode(CodeProto.Code.INVALID_PARAMETER)
                        .setMessage("parameters project error")
                        .addAllProjectResource(null)
                        .setPageInfo((PagerProto.PageInfo.Builder) null)
                        .build();
                response.onNext(build);
                response.onCompleted();
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
            ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setMessage("add success")
                    .addAllProjectResource(getProjectResourceList(projectResourceList))
                    .setPageInfo(pageInfo)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } catch (Exception ex) {
            log.error("findProjectResourcesList() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                    .setCode(CodeProto.Code.UNRECOGNIZED)
                    .setMessage("findProjectResourcesList service error")
                    .addAllProjectResource(null)
                    .setPageInfo((PagerProto.PageInfo.Builder) null)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    @Override
    public void batchProjectResourceList(ProjectResourceProto.BatchProjectResourceRequest request,
                                          StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        log.info("batchProjectResourcesList() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            if(request.getProjectId() <= 0 || request.getCodeList().size() <= 0) {
                ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                        .setCode(CodeProto.Code.INVALID_PARAMETER)
                        .setMessage("batchProjectResourceList parameters error")
                        .addAllProjectResource(null)
                        .build();
                response.onNext(build);
                response.onCompleted();
                return;
            }
            List<ProjectResource> projectResourceList = projectResourceService.batchProjectResourceList(request.getProjectId(), request.getCodeList());
            ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setMessage("add success")
                    .addAllProjectResource(getProjectResourceList(projectResourceList))
                    .build();
            response.onNext(build);
            response.onCompleted();
        } catch (Exception ex) {
            log.error("batchProjectResourceList() grpc service request={}, ex={}", request != null ? request.toString() : "", ex);
            ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                    .setCode(CodeProto.Code.UNRECOGNIZED)
                    .setMessage("batchProjectResourceList service error")
                    .addAllProjectResource(null)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }

    }

    public void ProjectResourceResponse(CodeProto.Code code, String message, ProjectResourceProto.ProjectResource resource, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        ProjectResourceProto.ProjectResourceResponse build = ProjectResourceProto.ProjectResourceResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .setProjectResource(resource)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    private ProjectResourceProto.ProjectResource getProjectResource(ProjectResource resource) {
        ProjectResourceProto.ProjectResource projectResource = ProjectResourceProto.ProjectResource.newBuilder()
                .setId(resource.getId())
                .setProjectId(resource.getProjectId())
                .setTitle(resource.getTitle())
                .setTargetId(resource.getTargetId())
                .setTargetType(resource.getTargetType())
                .setCode(resource.getCode())
                .setId(resource.getId())
                .build();
        return projectResource;
    }

    private List<ProjectResourceProto.ProjectResource> getProjectResourceList(List<ProjectResource> projectResourceList) {
        List<ProjectResourceProto.ProjectResource> result = new ArrayList<>();
        projectResourceList.stream().forEach(resource -> {
            ProjectResourceProto.ProjectResource projectResource = ProjectResourceProto.ProjectResource.newBuilder()
                    .setId(resource.getId())
                    .setProjectId(resource.getProjectId())
                    .setTitle(resource.getTitle())
                    .setTargetId(resource.getTargetId())
                    .setTargetType(resource.getTargetType())
                    .setCode(resource.getCode())
                    .setId(resource.getId())
                    .build();
            result.add(projectResource);
        });
        return result;
    }

}
