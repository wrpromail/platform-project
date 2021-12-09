package net.coding.lib.project.grpc.client;

import net.coding.grpc.client.platform.AbstractPlatformGrpcClient;
import net.coding.platform.ram.proto.grant.object.GrantObjectProto;
import net.coding.platform.ram.proto.grant.object.GrantObjectServiceGrpc;

import org.springframework.stereotype.Component;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlatformGrantObjectGRpcClient extends AbstractPlatformGrpcClient<GrantObjectServiceGrpc.GrantObjectServiceBlockingStub> {


    public GrantObjectProto.FindGrantObjectIdsResponse findGrantObjectIds(String grantScope, Integer tenantId, Integer userId) {
        GrantObjectProto.FindGrantObjectIdsRequest build = GrantObjectProto.FindGrantObjectIdsRequest.newBuilder()
                .setGrantScope(grantScope)
                .setTenantId(tenantId)
                .setUserId(userId)
                .build();
        return newStub().findGrantObjectIds(build);
    }

    public GrantObjectProto.FindUserIdsResponse findUserIds(String grantScope, Set<String> grantObjectIds) {
        GrantObjectProto.GrantObjectIdsRequest build = GrantObjectProto.GrantObjectIdsRequest.newBuilder()
                .setGrantScope(grantScope)
                .addAllGrantObjectIds(grantObjectIds)
                .build();
        return newStub().findUserIds(build);
    }

    public GrantObjectProto.GetGrantObjectNamesResponse getGrantObjectNames(String grantScope, Set<String> grantObjectIds) {
        GrantObjectProto.GrantObjectIdsRequest build = GrantObjectProto.GrantObjectIdsRequest.newBuilder()
                .setGrantScope(grantScope)
                .addAllGrantObjectIds(grantObjectIds)
                .build();
        return newStub().getGrantObjectNames(build);
    }
}
