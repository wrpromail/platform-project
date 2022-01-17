package net.coding.app.project.grpc.openapi;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.exchange.exception.ProjectNotExistsException;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.PermissionDenyException;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ResourceReferenceService;
import net.coding.proto.open.api.project.resource.ResourceReferenceProto.DescribeResourceReferencesRequest;
import net.coding.proto.open.api.project.resource.ResourceReferenceProto.DescribeResourceReferencesResponse;
import net.coding.proto.open.api.project.resource.ResourceReferenceProto.ResourceReferenceItem;
import net.coding.proto.open.api.project.resource.ResourceReferenceServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.lognet.springboot.grpc.GRpcService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.open.api.CodeProto.Code;

import static proto.open.api.CodeProto.Code.SUCCESS;

/**
 * @Description: OPEN API 项目协同资源引用列表 接口，非 OPEN API 业务 勿修改
 * @Author xxq
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiResourceReferenceGRpcService extends
        ResourceReferenceServiceGrpc.ResourceReferenceServiceImplBase {

    private final ProjectService projectService;
    private final ResourceReferenceService resourceReferenceService;
    private final LocaleMessageSource localeMessageSource;
    private final ProjectMemberService projectMemberService;

    /**
     * @param request
     * @param responseObserver
     * @return 引用资源列表
     */
    @Override
    public void describeResourceReferences(DescribeResourceReferencesRequest request,
                                           StreamObserver<DescribeResourceReferencesResponse> responseObserver) {
        try {
            int projectId = Optional.ofNullable(
                    projectService.getByNameAndTeamId(
                            request.getProjectName(),
                            request.getUser().getTeamId()
                    ))
                    .map(Project::getId)
                    .orElseThrow(ProjectNotExistsException::new);
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(
                    projectId,
                    request.getUser().getId());
            if (projectMember == null) {
                throw new PermissionDenyException();
            }
            List<ResourceReferenceItem> list = resourceReferenceService
                    .findListBySelfProjectId(projectId, request.getResourceCode())
                    .stream()
                    .map(item -> ResourceReferenceItem.newBuilder()
                            .setResourceCode(Integer.parseInt(item.getTargetIid()))
                            .setProjectId(item.getTargetProjectId())
                            .setResourceType(item.getTargetType())
                            .setResourceId(item.getTargetId())
                            .build())
                    .collect(Collectors.toList());
            responseOk(responseObserver, list);
        } catch (ProjectNotExistsException e) {
            responseError(responseObserver, Code.NOT_FOUND, localeMessageSource.getMessage(e.getKey()));
        } catch (PermissionDenyException e) {
            responseError(responseObserver, Code.NO_PERMISSION, localeMessageSource.getMessage(e.getKey()));
        }
    }

    private void responseOk(
            StreamObserver<DescribeResourceReferencesResponse> responseObserver,
            List<ResourceReferenceItem> list
    ) {
        responseObserver.onNext(DescribeResourceReferencesResponse.newBuilder()
                .setResult(CommonProto.Result.newBuilder()
                        .setCode(Code.SUCCESS.getNumber())
                        .setMessage(SUCCESS.name().toLowerCase())
                        .build())
                .addAllData(list)
                .build());
        responseObserver.onCompleted();
    }

    private void responseError(
            StreamObserver<DescribeResourceReferencesResponse> responseObserver,
            Code code,
            String message
    ) {
        responseObserver.onNext(DescribeResourceReferencesResponse.newBuilder()
                .setResult(CommonProto.Result.newBuilder()
                        .setCode(code.getNumber())
                        .setMessage(message)
                        .build())
                .build());
        responseObserver.onCompleted();
    }
}
