package net.coding.lib.project.grpc.client;

import net.coding.grpc.client.permission.AbstractPermissionGrpcClient;

import org.springframework.stereotype.Component;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import proto.ram.mapping.RamMappingProto;
import proto.ram.mapping.RamMappingServiceGrpc;

@Slf4j
@Component
public class RamMappingGRpcClient extends AbstractPermissionGrpcClient<RamMappingServiceGrpc.RamMappingServiceBlockingStub> {

    public RamMappingProto.GetRoleIdByPolicyResponse getRoleIdByPolicy(Integer teamId,
                                                                       Integer resourceId,
                                                                       String ResourceType,
                                                                       Set<Long> policyIds) {
        RamMappingProto.GetRoleIdByPolicyRequest build = RamMappingProto.GetRoleIdByPolicyRequest.newBuilder()
                .setTeamId(teamId.longValue())
                .setResourceId(String.valueOf(resourceId))
                .setResourceType(ResourceType)
                .addAllPolicyIds(policyIds)
                .build();
        return newStub().getRoleIdByPolicy(build);

    }
}
