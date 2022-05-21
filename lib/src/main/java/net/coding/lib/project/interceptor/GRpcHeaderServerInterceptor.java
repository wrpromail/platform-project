package net.coding.lib.project.interceptor;

import net.coding.lib.project.common.DeployTokenHeader;
import net.coding.lib.project.common.GRpcMetadataContextHolder;

import io.grpc.ForwardingServerCallListener;
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
        try {
            log.info("header open-api client:" + headers);
            String teamId = headers.get(Metadata.Key.of("TeamId", Metadata.ASCII_STRING_MARSHALLER));
            String userId = headers.get(Metadata.Key.of("UserId", Metadata.ASCII_STRING_MARSHALLER));
            String requestId = headers.get(Metadata.Key.of("RequestId", Metadata.ASCII_STRING_MARSHALLER));
            String deployTokenId = headers.get(Metadata.Key.of("DeployTokenId", Metadata.ASCII_STRING_MARSHALLER));
            String deployTokenProjectId = headers.get(Metadata.Key.of("DeployTokenProjectId", Metadata.ASCII_STRING_MARSHALLER));
            DeployTokenHeader deployTokenHeader = DeployTokenHeader.builder()
                    .teamId(teamId)
                    .userId(userId)
                    .requestId(requestId)
                    .deployTokenId(deployTokenId)
                    .deployTokenProjectId(deployTokenProjectId)
                    .build();
            GRpcMetadataContextHolder.set(deployTokenHeader);
            final ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
            return new GRpcHeaderServerCallListener<>(listener, deployTokenHeader);
        } finally {
            GRpcMetadataContextHolder.remove();
        }
    }

    public static class GRpcHeaderServerCallListener<ReqT> extends ForwardingServerCallListener<ReqT> {

        private final ServerCall.Listener<ReqT> delegate;

        private final DeployTokenHeader deployTokenHeader;

        public GRpcHeaderServerCallListener(ServerCall.Listener<ReqT> delegate, DeployTokenHeader deployTokenHeader) {
            this.delegate = delegate;
            this.deployTokenHeader = deployTokenHeader;
        }

        @Override
        protected ServerCall.Listener<ReqT> delegate() {
            return delegate;
        }

        @Override
        public void onMessage(ReqT message) {
            wrapTracingRun(deployTokenHeader, () -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            wrapTracingRun(deployTokenHeader, super::onHalfClose);
        }

        @Override
        public void onCancel() {
            wrapTracingRun(deployTokenHeader, super::onCancel);
        }

        @Override
        public void onComplete() {
            wrapTracingRun(deployTokenHeader, super::onComplete);
        }

        @Override
        public void onReady() {
            wrapTracingRun(deployTokenHeader, super::onReady);
        }
    }

    private static void wrapTracingRun(DeployTokenHeader deployTokenHeader, Runnable run) {
        GRpcMetadataContextHolder.set(deployTokenHeader);
        try {
            run.run();
        } finally {
            GRpcMetadataContextHolder.remove();
        }
    }
}