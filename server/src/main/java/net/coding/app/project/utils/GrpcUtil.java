package net.coding.app.project.utils;

import net.coding.lib.project.dto.ProjectResourceDTO;
import net.coding.lib.project.entity.ExternalLink;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ResourceReference;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.platform.project.ResourceReferenceProto;

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

    public static void FindProjectResourceMutuallyResponse(CodeProto.Code code, String message,
                                                   List<ProjectResourceDTO> projectResourceList,
                                                   StreamObserver<ProjectResourceProto.FindProjectResourceMutuallyResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            List<ProjectResourceProto.ProjectResourceDto> list = new ArrayList<>();
            projectResourceList.forEach(record -> {
                ProjectResourceProto.ProjectResourceDto dto = ProjectResourceProto.ProjectResourceDto.newBuilder()
                        .setTargetId(record.getTargetId())
                        .setTargetProjectName(record.getTargetProjectName())
                        .setTargetProjectDisplayName(record.getTargetProjectDisplayName())
                        .setCode(record.getCode())
                        .setTargetType(record.getTargetType())
                        .setTargetId(record.getTargetId())
                        .setTitle(record.getTitle())
                        .setLink(record.getLink())
                        .setStatus(record.getStatus())
                        .setHasCommentRelated(record.getHasCommentRelated())
                        .build();
                list.add(dto);
            });
            ProjectResourceProto.FindProjectResourceMutuallyResponse build = ProjectResourceProto.FindProjectResourceMutuallyResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .addAllProjectResourceDto(list)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.FindProjectResourceMutuallyResponse build = ProjectResourceProto.FindProjectResourceMutuallyResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void resourceReferenceResponse(CodeProto.Code code, String message, ResourceReferenceProto.ResourceReference resourceReference,
                                                 StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ResourceReferenceProto.ResourceReferenceResponse build = ResourceReferenceProto.ResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setResourceReference(resourceReference)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ResourceReferenceProto.ResourceReferenceResponse build = ResourceReferenceProto.ResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static ResourceReferenceProto.ResourceReference getResourceReference(ResourceReference reference) {
        ResourceReferenceProto.ResourceReference resourceReference = ResourceReferenceProto.ResourceReference.newBuilder()
                .setSelfId(reference.getSelfId())
                .setSelfProjectId(reference.getSelfProjectId())
                .setSelfIid(reference.getSelfIid())
                .setSelfType(reference.getSelfType())
                .setTargetId(reference.getTargetId())
                .setTargetProjectId(reference.getTargetProjectId())
                .setTargetType(reference.getTargetType())
                .setId(reference.getId())
                .setCreateAt(reference.getCreatedAt().getTime())
                .setUpdateAt(reference.getUpdatedAt().getTime())
                .setDeletedAt(DateUtil.dateToStr(reference.getDeletedAt()))
                .build();
        return resourceReference;
    }

    public static void resourceReferenceCommonResponse(CodeProto.Code code, String message, StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        ResourceReferenceProto.ResourceReferenceCommonResponse build = ResourceReferenceProto.ResourceReferenceCommonResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build();
        response.onNext(build);
        response.onCompleted();
    }

    public static void countByTargetResponse(CodeProto.Code code, String message, Integer counts, StreamObserver<ResourceReferenceProto.CountResourceReferenceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ResourceReferenceProto.CountResourceReferenceResponse build = ResourceReferenceProto.CountResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setCounts(counts)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ResourceReferenceProto.CountResourceReferenceResponse build = ResourceReferenceProto.CountResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static List<ResourceReferenceProto.ResourceReference> getResourceReferenceList(List<ResourceReference> references) {
        List<ResourceReferenceProto.ResourceReference> resourceReferenceList = new ArrayList<>();
        references.forEach(reference -> {
            ResourceReferenceProto.ResourceReference resourceReference = ResourceReferenceProto.ResourceReference.newBuilder()
                    .setSelfId(reference.getSelfId())
                    .setSelfProjectId(reference.getSelfProjectId())
                    .setSelfIid(reference.getSelfIid())
                    .setSelfType(reference.getSelfType())
                    .setTargetId(reference.getTargetId())
                    .setTargetProjectId(reference.getTargetProjectId())
                    .setTargetType(reference.getTargetType())
                    .setId(reference.getId())
                    .setCreateAt(reference.getCreatedAt().getTime())
                    .setUpdateAt(reference.getUpdatedAt().getTime())
                    .setDeletedAt(DateUtil.dateToStr(reference.getDeletedAt()))
                    .build();
            resourceReferenceList.add(resourceReference);
        });
        return resourceReferenceList;
    }

    public static void findResourceReferenceListResponse(CodeProto.Code code, String message,
                                                         List<ResourceReference> resourceReferenceList,
                                                         StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ResourceReferenceProto.FindResourceReferenceListResponse build = ResourceReferenceProto.FindResourceReferenceListResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .addAllResourceReference(getResourceReferenceList(resourceReferenceList))
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ResourceReferenceProto.FindResourceReferenceListResponse build = ResourceReferenceProto.FindResourceReferenceListResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void findIdsMutuallyResponse(CodeProto.Code code, String message,
                                               List<Integer> ids,
                                               StreamObserver<ResourceReferenceProto.FindIdsMutuallyResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ResourceReferenceProto.FindIdsMutuallyResponse build = ResourceReferenceProto.FindIdsMutuallyResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .addAllId(ids)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ResourceReferenceProto.FindIdsMutuallyResponse build = ResourceReferenceProto.FindIdsMutuallyResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void addExternalLinkResponse(CodeProto.Code code, String message, ExternalLink externalLink,
                                               StreamObserver<ProjectResourceProto.AddExternalLinkResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ProjectResourceProto.ExternalLink data = ProjectResourceProto.ExternalLink.newBuilder()
                    .setId(externalLink.getId())
                    .setUserId(externalLink.getCreatorId())
                    .setTitle(externalLink.getTitle())
                    .setLink(externalLink.getLink())
                    .setProjectId(externalLink.getProjectId())
                    .setIid(externalLink.getIid())
                    .build();
            ProjectResourceProto.AddExternalLinkResponse build = ProjectResourceProto.AddExternalLinkResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .setExternalLink(data)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ProjectResourceProto.AddExternalLinkResponse build = ProjectResourceProto.AddExternalLinkResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }

    public static void existsResourceReferenceResponse(CodeProto.Code code, String message, boolean existsFlag,
                                                       StreamObserver<ResourceReferenceProto.ExistsResourceReferenceResponse> response) {
        if(CodeProto.Code.SUCCESS.equals(code)) {
            ResourceReferenceProto.ExistsResourceReferenceResponse build = ResourceReferenceProto.ExistsResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setExistsFlag(existsFlag)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        } else {
            ResourceReferenceProto.ExistsResourceReferenceResponse build = ResourceReferenceProto.ExistsResourceReferenceResponse.newBuilder()
                    .setCode(code)
                    .setMessage(message)
                    .build();
            response.onNext(build);
            response.onCompleted();
        }
    }
}
