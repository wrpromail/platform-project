package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import proto.artifact.*;

/**
 * @Author liuying
 * @Date 2021/1/11 4:41 下午
 * @Version 1.0
 */
@Component
public class ArtifactRepositoryGrpcClient extends EndpointGrpcClient<ArtifactRepositoryServiceGrpc.ArtifactRepositoryServiceBlockingStub> {
    @Value("${grpc.client.artifacts.serviceName: artifacts-api-server}")
    private String serviceName;

    @Value("${grpc.client.artifacts.servicePort:20153}")
    private int servicePort;
    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public Map<Integer, String> getArtifactReposByIds(List<Integer> ids) {
        ArtifactRepositoryProto.GetReposByIdsRequest request = ArtifactRepositoryProto.GetReposByIdsRequest.newBuilder()
                .addAllRepoIds(ids)
                .build();
        ArtifactRepositoryProto.GetReposResponse response = newStub().getReposByIds(request);
        return response.getRepositoriesList().stream()
                .collect(Collectors.toMap(ArtifactRepositoryProto.Repository::getId, ArtifactRepositoryProto.Repository::getName));
    }
}
