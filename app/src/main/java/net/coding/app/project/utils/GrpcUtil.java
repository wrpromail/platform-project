package net.coding.app.project.utils;

import net.coding.lib.project.entity.ProjectResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.grpc.stub.StreamObserver;
import proto.common.CodeProto;
import proto.common.PagerProto;
import proto.projectResource.ProjectResourceProto;

public class GrpcUtil {

    public static void projectResourceResponse(CodeProto.Code code, String message, ProjectResourceProto.ProjectResource resource, StreamObserver<ProjectResourceProto.ProjectResourceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.ProjectResourceResponse build = ProjectResourceProto.ProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setProjectResource(resource)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.ProjectResourceResponse build = ProjectResourceProto.ProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
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
                .setId(resource.getId() != null ? resource.getId() : 0)
                .setProjectId(resource.getProjectId())
                .setTitle(resource.getTitle())
                .setTargetId(resource.getTargetId())
                .setTargetType(resource.getTargetType())
                .setCode(resource.getCode())
                .setId(resource.getId())
                .setUrl(resource.getResourceUrl())
                .build();
        return projectResource;
    }

    public static List<ProjectResourceProto.ProjectResource> getProjectResourceList(List<ProjectResource> projectResourceList) {
        List<ProjectResourceProto.ProjectResource> result = new ArrayList<>();
        projectResourceList.forEach(resource -> {
            ProjectResourceProto.ProjectResource projectResource = ProjectResourceProto.ProjectResource.newBuilder()
                    .setId(resource.getId())
                    .setProjectId(resource.getProjectId())
                    .setTitle(resource.getTitle())
                    .setTargetId(resource.getTargetId())
                    .setTargetType(resource.getTargetType())
                    .setCode(resource.getCode())
                    .setId(resource.getId())
                    .setUrl(Objects.nonNull(resource.getResourceUrl()) ? resource.getResourceUrl() : "")
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
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.FindProjectResourceResponse build = ProjectResourceProto.FindProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void batchProjectResourceResponse(CodeProto.Code code, String message,
                                                    List<ProjectResourceProto.ProjectResource> projectResourceList,
                                                    StreamObserver<ProjectResourceProto.BatchProjectResourceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .addAllProjectResource(projectResourceList)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.BatchProjectResourceResponse build = ProjectResourceProto.BatchProjectResourceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void multiCodeResponse(CodeProto.Code code, String message,
                                         ProjectResourceProto.MultiResourceSequence multiResourceSequence,
                                         StreamObserver<ProjectResourceProto.MultiCodeResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.MultiCodeResponse build = ProjectResourceProto.MultiCodeResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setMultiResourceSequence(multiResourceSequence)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.MultiCodeResponse build = ProjectResourceProto.MultiCodeResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void getResourceLinkResponse(CodeProto.Code code, String message, String url,
                                               StreamObserver<ProjectResourceProto.GetResourceLinkResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.GetResourceLinkResponse build = ProjectResourceProto.GetResourceLinkResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setUrl(url)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.GetResourceLinkResponse build = ProjectResourceProto.GetResourceLinkResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }
}
