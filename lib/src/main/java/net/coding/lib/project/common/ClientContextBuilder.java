package net.coding.lib.project.common;

import net.coding.platform.ram.client.grpc.context.SystemContextBuilder;
import net.coding.platform.ram.proto.CommonProto;

import org.springframework.stereotype.Component;

@Component
public class ClientContextBuilder implements SystemContextBuilder {
    @Override
    public CommonProto.SystemContext getGrpcContext() {
        return CommonProto.SystemContext.newBuilder()
                .setCurrentTenantId(SystemContextHolder.get().getTeamId())
                .setCurrentUserId(SystemContextHolder.get().getId())
                .build();
    }
}
