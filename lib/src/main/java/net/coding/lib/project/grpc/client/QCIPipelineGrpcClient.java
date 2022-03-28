package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import qci.grpc.server.PipelineOuterClass;
import qci.grpc.server.PipelineServiceGrpc;

@Component
@Slf4j
public class QCIPipelineGrpcClient extends EndpointGrpcClient<PipelineServiceGrpc.PipelineServiceBlockingStub> {
    @Value("${grpc.client.qciGrpcServer.serviceName:qci-grpc-server}")
    private String serviceName;

    @Value("${grpc.client.qciGrpcServer.servicePort:15751}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public List<PipelineOuterClass.Pipeline> listByProjectId(Integer projectId) {
        try {
            return newStub()
                    .listByProjectId(
                            PipelineOuterClass.ListByProjectIdRequest
                                    .newBuilder()
                                    .setProjectId(projectId)
                                    .build()
                    )
                    .getPipelinesList();
        } catch (Exception e) {
            log.warn("Load qci pipeline by project id {} failure, cause of {}", projectId,
                    e.getMessage());
        }
        return Collections.emptyList();
    }
}
