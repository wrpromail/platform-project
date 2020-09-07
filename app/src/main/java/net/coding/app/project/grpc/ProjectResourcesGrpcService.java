package net.coding.app.project.grpc;

import net.coding.lib.project.service.ProjectResourcesService;

import org.lognet.springboot.grpc.GRpcService;

import javax.annotation.Resource;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.projectResources.ProjectResourceServiceGrpc;
import proto.projectResources.ProjectResourcesProto;

@Slf4j
@GRpcService
public class ProjectResourcesGrpcService extends ProjectResourceServiceGrpc.ProjectResourceServiceImplBase {

    @Resource
    private ProjectResourcesService ProjectResourcesService;

    @Override
    public void addProjectResources(ProjectResourcesProto.AddProjectResourcesRequest request,
                                   StreamObserver<ProjectResourcesProto.ProjectResourcesResponse> response) {
        log.info("addProjectResources() grpc service receive: {}", request != null ? request.toString() : "");
        ProjectResourcesProto.ProjectResourcesResponse build = ProjectResourcesProto.ProjectResourcesResponse.newBuilder()
                .setCode(CodeProto.Code.SUCCESS)
                .setMessage("add success")
                .setResourcesCode(1)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    @Override
    public void updateProjectResources(ProjectResourcesProto.UpdateProjectResourcesRequest request,
                                      StreamObserver<ProjectResourcesProto.ProjectResourcesResponse> response) {
        log.info("updateProjectResources() grpc service receive: {}", request != null ? request.toString() : "");
        ProjectResourcesProto.ProjectResourcesResponse build = ProjectResourcesProto.ProjectResourcesResponse.newBuilder()
                .setCode(CodeProto.Code.SUCCESS)
                .setMessage("add success")
                .setResourcesCode(1)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    @Override
    public void deleteProjectResources(ProjectResourcesProto.DeleteProjectResourcesRequest request,
                                      StreamObserver<ProjectResourcesProto.ProjectResourcesResponse> response) {
        log.info("deleteProjectResources() grpc service receive: {}", request != null ? request.toString() : "");

        ProjectResourcesProto.ProjectResourcesResponse build = ProjectResourcesProto.ProjectResourcesResponse.newBuilder()
                .setCode(CodeProto.Code.SUCCESS)
                .setMessage("add success")
                .setResourcesCode(1)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    @Override
    public void findProjectResourcesList(ProjectResourcesProto.FindProjectResourcesRequest request,
                                      StreamObserver<ProjectResourcesProto.FindProjectResourcesResponse> response) {
        log.info("findProjectResourcesList() grpc service receive: {}", request != null ? request.toString() : "");
        ProjectResourcesProto.ProjectResources projectResources = ProjectResourcesProto.ProjectResources.newBuilder()
                .setCode(1)
                .setProjectId(1)
                .setTitle("")
                .setTargetId(1)
                .setTargetType("")
                .build();
        ProjectResourcesProto.FindProjectResourcesResponse build = ProjectResourcesProto.FindProjectResourcesResponse.newBuilder()
                .setCode(CodeProto.Code.SUCCESS)
                .setMessage("add success")
                .setProjectResources(0, projectResources)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    @Override
    public void getProjectResources(ProjectResourcesProto.GetProjectResourcesRequest request,
                                      StreamObserver<ProjectResourcesProto.GetProjectResourcesResponse> response) {
        log.info("getProjectResources() grpc service receive: {}", request != null ? request.toString() : "");
        ProjectResourcesProto.ProjectResources projectResources = ProjectResourcesProto.ProjectResources.newBuilder()
                .setCode(1)
                .setProjectId(1)
                .setTitle("")
                .setTargetId(1)
                .setTargetType("")
                .build();
        ProjectResourcesProto.GetProjectResourcesResponse build = ProjectResourcesProto.GetProjectResourcesResponse.newBuilder()
                .setCode(CodeProto.Code.SUCCESS)
                .setMessage("add success")
                .setProjectResources(projectResources)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

}
