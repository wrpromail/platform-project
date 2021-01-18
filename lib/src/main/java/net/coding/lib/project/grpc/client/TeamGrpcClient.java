package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.grpc.client.platform.BeanUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import proto.platform.team.TeamProto;
import proto.platform.team.TeamServiceGrpc;

@Slf4j
@Component
public class TeamGrpcClient extends EndpointGrpcClient<TeamServiceGrpc.TeamServiceBlockingStub> {

    @Value("${grpc.client.platform.service.serviceName:platform-service}")
    private String serviceName;

    @Value("${grpc.client.platform.service.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public String getTeamHostWithProtocolByTeamId(Integer teamId) {
        TeamProto.GetTeamHostWithProtocolRequest request = TeamProto.GetTeamHostWithProtocolRequest.newBuilder()
                .setTeamId(teamId)
                .build();
        TeamProto.GetTeamHostWithProtocolResponse response = newStub().getTeamHostWithProtocol(request);
        return response.getTeamHostWithProtocol();
    }
    public TeamProto.GetTeamResponse getTeam(TeamProto.GetTeamByIdRequest request) {
        try {
            return  newStub().getTeamById(request);
        } catch (Exception e) {
            log.error("getTeam {}" ,e.getMessage());
            return null;
        }
    }
}
