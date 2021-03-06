package net.coding.app.project.grpc;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.lib.project.entity.ExternalLink;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.helper.ProjectResourceServiceHelper;
import net.coding.lib.project.service.ExternalLinkService;
import net.coding.proto.platform.project.ExternalLinkProto;
import net.coding.proto.platform.project.ExternalLinkServiceGrpc;

import org.lognet.springboot.grpc.GRpcService;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

@Slf4j
@GRpcService
@AllArgsConstructor
public class ExternalLinkGrpcService extends ExternalLinkServiceGrpc.ExternalLinkServiceImplBase {

    private final ExternalLinkService externalLinkService;

    private final ProjectGrpcClient projectGrpcClient;

    private final ProjectResourceServiceHelper projectResourceServiceHelper;


    @Override
    public void addExternalLink(ExternalLinkProto.AddExternalLinkRequest request,
                                StreamObserver<ExternalLinkProto.AddExternalLinkResponse> response) {
        try {
            ExternalLink externalLink = externalLinkService.add(request.getUserId(), request.getTitle(), request.getLink());
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(externalLink.getId());
            record.setTargetType(ExternalLink.class.getSimpleName());
            record.setCreatedBy(request.getUserId());
            String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
            ProjectResource resource = projectResourceServiceHelper.addProjectResource(record, projectPath);
            externalLink.setIid(Integer.valueOf(resource.getCode()));
            GrpcUtil.addExternalLinkResponse(CodeProto.Code.SUCCESS, "add success", externalLink, response);
        } catch (Exception ex) {
            log.error("addExternalLink fail, parameter is " + request.toString(), ex);
            GrpcUtil.addExternalLinkResponse(CodeProto.Code.INTERNAL_ERROR,
                    "addExternalLink service error", null, response);
        }
    }

    @Override
    public void getExternalLinkById(ExternalLinkProto.GetExternalLinkByIdRequest request, StreamObserver<ExternalLinkProto.GetExternalLinkByIdResponse> responseObserver) {
        try {
            ExternalLink externalLink = externalLinkService.getById(request.getId());
            if (externalLink == null) {
                ExternalLinkProto.GetExternalLinkByIdResponse response = ExternalLinkProto.GetExternalLinkByIdResponse
                        .newBuilder()
                        .setCode(CodeProto.Code.NOT_FOUND)
                        .setMessage("NOT FOUND")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            ExternalLinkProto.GetExternalLinkByIdResponse response = ExternalLinkProto.GetExternalLinkByIdResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setExternalLink(
                            ExternalLinkProto.ExternalLink.newBuilder()
                                    .setId(externalLink.getId())
                                    .setLink(externalLink.getLink())
                                    .setTitle(externalLink.getTitle())
                                    .setIid(externalLink.getIid() == null ? 0 : externalLink.getIid())
                                    .setProjectId(externalLink.getProjectId())
                                    .setUserId(externalLink.getCreatorId())
                                    .build()
                    )
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getExternalLinkById fail, parameter is " + request.toString(), e);
            ExternalLinkProto.GetExternalLinkByIdResponse response = ExternalLinkProto.GetExternalLinkByIdResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL_ERROR")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
