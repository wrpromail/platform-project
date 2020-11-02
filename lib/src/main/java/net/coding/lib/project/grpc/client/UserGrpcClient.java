package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.user.UserProto;
import proto.platform.user.UserServiceGrpc;

@Slf4j
@Component
public class UserGrpcClient extends EndpointGrpcClient<UserServiceGrpc.UserServiceBlockingStub> {

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

    public UserProto.User getUserByNameAndTeamId(String userName, Integer teamId) {
        try {
            UserProto.GetUserByNameAndTeamIdRequest request = UserProto.GetUserByNameAndTeamIdRequest.newBuilder()
                    .setName(userName)
                    .setTeamId(teamId)
                    .build();
            UserProto.GetUserResponse response = newStub().getUserByNameAndTeamId(request);
            if(CodeProto.Code.SUCCESS.equals(response.getCode())) {
                return response.getData();
            }
        } catch (Exception ex) {
            log.error("UserGrpcClient->getUserByNameAndTeamId() userName={}, teamId={}, ex={}", userName, teamId, ex);
        }
        return null;
    }

    public UserProto.User getUserByGlobalKey(String globalKey) {
        try {
            UserProto.GetUserByGlobalKeyRequest request = UserProto.GetUserByGlobalKeyRequest.newBuilder()
                    .setUserGlobalKey(globalKey)
                    .build();
            UserProto.GetUserResponse response = newStub().getUserByGlobalKey(request);
            if(CodeProto.Code.SUCCESS.equals(response.getCode())) {
                return response.getData();
            }
        } catch (Exception ex) {
            log.error("UserGrpcClient->getUserByGlobalKey() globalKey={}, ex={}", globalKey, ex);
        }
        return null;
    }

    public UserProto.User getUserById(Integer id) {
        try {
            UserProto.GetUserByIdRequest request = UserProto.GetUserByIdRequest.newBuilder()
                    .setUserId(id)
                    .build();
            UserProto.GetUserResponse response = newStub().getUserById(request);
            if(CodeProto.Code.SUCCESS.equals(response.getCode())) {
                return response.getData();
            }
        } catch (Exception ex) {
            log.error("UserGrpcClient->getUserById() id={}, ex={}", id, ex);
        }
        return null;
    }

    public String getUserHtmlLinkById(Integer id) {
        try {
            UserProto.GetUserHtmlLinkRequest request = UserProto.GetUserHtmlLinkRequest.newBuilder()
                    .setUserId(id)
                    .build();
            UserProto.GetUserHtmlLinkResponse response = newStub().getUserHtmlLink(request);
            if(CodeProto.Code.SUCCESS.equals(response.getCode())) {
                return response.getUserHtmlLink();
            }
        } catch (Exception ex) {
            log.error("UserGrpcClient->getUserHtmlLinkById() id={}, ex={}", id, ex);
        }
        return null;
    }

    public List<UserProto.User> findUserByIds(List<Integer> userIds) {
        try {
            UserProto.FindUserByIdsRequest request = UserProto.FindUserByIdsRequest.newBuilder()
                    .addAllIds(userIds)
                    .build();
            UserProto.FindUserResponse response = newStub().findUserByIds(request);
            if(CodeProto.Code.SUCCESS.equals(response.getCode())) {
                return response.getDataList();
            }
        } catch (Exception ex) {
            log.error("UserGrpcClient->findUserByIds() userIds={}, ex={}", userIds, ex);
        }
        return null;
    }
}
