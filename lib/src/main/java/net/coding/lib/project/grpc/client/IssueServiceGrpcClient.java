package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.IssueProto;
import net.coding.e.proto.IssueServiceGrpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IssueServiceGrpcClient extends EndpointGrpcClient<IssueServiceGrpc.IssueServiceBlockingStub> {

    @Value("${grpc.client.issue.service.serviceName:micro-agile}")
    private String serviceName;

    @Value("${grpc.client.issue.service.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public IssueProto.IssueResponse getIssueById(Integer issueId, Integer projectId, boolean withDeleted) {
        log.info("IssueServiceGrpcClient.getIssueById() issueId={}, projectId={}, withDeleted={}", issueId, projectId, withDeleted);
        IssueProto.IssueRequest request = IssueProto.IssueRequest.newBuilder()
                .setIssueId(issueId)
                .setProjectId(projectId)
                .setWithDeleted(withDeleted)
                .build();
        IssueProto.IssueResponse response = newStub().getIssueById(request);
        log.info("IssueServiceGrpcClient.getIssueById() response={}", response.toString());
        return response;
    }
}
