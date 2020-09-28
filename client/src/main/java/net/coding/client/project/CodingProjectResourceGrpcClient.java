package net.coding.client.project;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.ProjectResourceProto;
import net.coding.e.proto.ProjectResourceServiceGrpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CodingProjectResourceGrpcClient extends EndpointGrpcClient<ProjectResourceServiceGrpc.ProjectResourceServiceBlockingStub> {

    @Value("${grpc.client.coding.serviceName:9.135.93.238}")
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
