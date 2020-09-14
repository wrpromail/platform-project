package net.coding.app.project.utils;

import net.coding.lib.project.entity.ProjectResource;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import proto.common.CodeProto;
import proto.common.PagerProto;
import proto.projectResource.ProjectResourceProto;

public class GrpcUtil {

    public static void projectResourceResponse(CodeProto.Code code, String message, ProjectResourceProto.ProjectResource resource, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        ProjectResourceProto.ProjectResourceResponse build = ProjectResourceProto.ProjectResourceResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .setProjectResource(resource)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    public static void projectResourceCommonResponse(CodeProto.Code code, String message, StreamObserver<ProjectResourceProto.ProjectResourceCommonResponse> response) {
        ProjectResourceProto.ProjectResourceCommonResponse build = ProjectResourceProto.ProjectResourceCommonResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    public static ProjectResourceProto.ProjectResource getProjectResource(ProjectResource resource) {
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

    public static List<ProjectResourceProto.ProjectResource> getProjectResourceList(List<ProjectResource> projectResourceList) {
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

    public static void findProjectResourceResponse(CodeProto.Code code, String message, PagerProto.PageInfo pageInfo,
                                                   List<ProjectResourceProto.ProjectResource> projectResourceList,
                                                   StreamObserver<ProjectResourceProto.FindProjectResourceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .addAllProjectResource(projectResourceList)
                    .setPageInfo(pageInfo)
                    .build();
        } else {
            ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
        }
    }

    public static void batchProjectResourceResponse(CodeProto.Code code, String message,
                                                    List<ProjectResourceProto.ProjectResource> projectResourceList,
                                                    StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .addAllProjectResource(projectResourceList)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    public static void multiCodeResponse(CodeProto.Code code, String message,
                                         ProjectResourceProto.MultiResourceSequence multiResourceSequence,
                                         StreamObserver<ProjectResourceProto.MultiCodeResponse> response) {
        ProjectResourceProto.MultiCodeResponse build = ProjectResourceProto.MultiCodeResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .setData(multiResourceSequence)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    public static void getResourceLinkResponse(CodeProto.Code code, String message, String url,
                                               StreamObserver<ProjectResourceProto.GetResourceLinkResponse> response) {
        ProjectResourceProto.GetResourceLinkResponse build = ProjectResourceProto.GetResourceLinkResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .setUrl(url)
                .build();
        response.onNext(build);
        response.onCompleted();
    }
}
