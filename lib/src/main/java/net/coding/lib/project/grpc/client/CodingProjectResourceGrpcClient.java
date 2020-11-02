package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.CodingProjectResourceServiceGrpc;
import net.coding.e.proto.ProjectResourceProto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CodingProjectResourceGrpcClient extends EndpointGrpcClient<CodingProjectResourceServiceGrpc.CodingProjectResourceServiceBlockingStub> {

    @Value("${grpc.client.coding.serviceName:e-coding}")
    private String serviceName;

    @Value("${grpc.client.coding.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    /**
     * 根据项目id与目标类型及目标id查询项目资源
     *
     * @param projectResourceId
     * @return
     */
    public String getResourceLink(Integer projectResourceId) {
        try {
            ProjectResourceProto.GetResourceLinkRequest request = ProjectResourceProto.GetResourceLinkRequest.newBuilder()
                    .setProjectResourceId(projectResourceId)
                    .build();
            ProjectResourceProto.GetResourceLinkResponse response = newStub().getResourceLink(request);
            if (0 == response.getCode()) {
                return response.getUrl();
            }
        } catch (Exception ex) {
            log.error("CodingProjectResourceGrpcClient->getResourceLink() projectResourceId={}, ex={}", projectResourceId, ex);
        }
        return "";
    }

}
