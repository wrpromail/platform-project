package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import proto.ci.CiJobProto;
import proto.ci.CiJobServiceGrpc;

@Component
@Slf4j
public class CiJobGrpcClient extends EndpointGrpcClient<CiJobServiceGrpc.CiJobServiceBlockingStub> {
    @Value("${grpc.client.ci.manager.serviceName:ci-manager}")
    private String serviceName;

    @Value("${grpc.client.ci.manager.servicePort:15751}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public List<CiJobProto.CiJob> listByProject(Integer projectId) {
        try {
            CiJobProto.ListByProjectRequest request = CiJobProto.ListByProjectRequest.newBuilder()
                    .setProjectId(projectId)
                    .build();
            CiJobProto.ListByProjectResponse response = newStub().listByProject(request);
            if (!ObjectUtils.isEmpty(response)) {
                return response.getCiJobsList();
            }
        } catch (Exception e) {
            log.warn("Load cci job by project id {} failure, cause of {}", projectId, e.getMessage());
        }
        return Collections.emptyList();
    }
}
