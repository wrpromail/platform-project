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
import org.springframework.beans.factory.annotation.Autowired;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.projectResource.ProjectResourceProto;

@Slf4j
@GRpcService
public class ExternalLinkGrpcService extends ExternalLinkServiceGrpc.ExternalLinkServiceImplBase {

    @Autowired
    private ExternalLinkService externalLinkService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private ProjectResourceServiceHelper projectResourceServiceHelper;

    @Override
    public void addExternalLink(ExternalLinkProto.AddExternalLinkRequest request,
                                StreamObserver<ExternalLinkProto.AddExternalLinkResponse> response) {
        log.info("addExternalLink() grpc service receive: {}", request != null ? request.toString() : "");
        try {
            ExternalLink externalLink = externalLinkService.add(request.getUserId(), request.getTitle(), request.getLink());
            ProjectResource record = new ProjectResource();
            record.setProjectId(request.getProjectId());
            record.setTitle(request.getTitle());
            record.setTargetId(externalLink.getId());
            record.setTargetType(ExternalLink.class.getSimpleName());
            record.setCreatedBy(request.getUserId());
            String projectPath = projectGrpcClient.getProjectPath(record.getProjectId());
            log.info("addProjectResource() record= {}", record.toString());
            ProjectResource resource = projectResourceServiceHelper.addProjectResource(record, projectPath);
            externalLink.setIid(resource.getCode());
            GrpcUtil.addExternalLinkResponse(CodeProto.Code.SUCCESS, "add success", externalLink, response);
        } catch (Exception ex) {
            log.error("addExternalLink() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.addExternalLinkResponse(CodeProto.Code.INTERNAL_ERROR,
                    "addExternalLink service error",null, response);
        }
    }
}