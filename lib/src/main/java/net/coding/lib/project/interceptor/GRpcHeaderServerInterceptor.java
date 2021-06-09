package net.coding.lib.project.interceptor;

import net.coding.lib.project.common.DeployTokenHeader;
import net.coding.lib.project.common.GRpcMetadataContextHolder;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GRpcHeaderServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        log.info("header open-api client:" + headers);
        String teamId = headers.get(Metadata.Key.of("TeamId", Metadata.ASCII_STRING_MARSHALLER));
        String userId = headers.get(Metadata.Key.of("UserId", Metadata.ASCII_STRING_MARSHALLER));
        String requestId = headers.get(Metadata.Key.of("RequestId", Metadata.ASCII_STRING_MARSHALLER));
        String deployTokenId = headers.get(Metadata.Key.of("DeployTokenId", Metadata.ASCII_STRING_MARSHALLER));
        String deployTokenProjectId = headers.get(Metadata.Key.of("DeployTokenProjectId", Metadata.ASCII_STRING_MARSHALLER));
        GRpcMetadataContextHolder.set(DeployTokenHeader.builder()
                .teamId(teamId)
                .userId(userId)
                .requestId(requestId)
                .deployTokenId(deployTokenId)
                .deployTokenProjectId(deployTokenProjectId)
                .build());
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
        }, headers);
    }
}