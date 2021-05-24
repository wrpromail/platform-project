package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.oauth.OauthProto;
import proto.platform.oauth.OauthServiceGrpc;

@Slf4j
@Component
public class OauthServiceGrpcClient extends EndpointGrpcClient<OauthServiceGrpc.OauthServiceBlockingStub> {
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

    public OauthProto.OauthAccessToken getOauthAccessToken(Integer oauthAccessTokenId) {

        OauthProto.GetOauthAccessTokenRequest request = OauthProto.GetOauthAccessTokenRequest.newBuilder()
                .setOauthAccessTokenId(oauthAccessTokenId)
                .build();

        OauthProto.GetOauthAccessTokenResponse response = newStub().getOauthAccessToken(request);

        if (CodeProto.Code.SUCCESS != response.getCode()) {
            log.warn("OAuthGrpcClient.getOauthAccessToken({}):{}", oauthAccessTokenId, response.getMessage());
        }
        return response.getData();
    }
}
