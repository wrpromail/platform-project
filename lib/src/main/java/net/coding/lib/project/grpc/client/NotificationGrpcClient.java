package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import proto.notification.NotificationProto;
import proto.notification.NotificationServiceGrpc;

@Slf4j
@Component
public class NotificationGrpcClient extends EndpointGrpcClient<NotificationServiceGrpc.NotificationServiceBlockingStub> {

    @Value("${grpc.client.notification.service.serviceName:notification-service}")
    private String serviceName;

    @Value("${grpc.client.notification.service.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public NotificationProto.NotificationSendResponse send(NotificationProto.NotificationSendRequest request) {
        try {
            NotificationProto.NotificationSendResponse response = newStub().send(request);
            return response;
        } catch (Exception ex) {
            log.error("NotifiactionGrpcClient->send() userGlobalKey={}, ex={}", request, ex);
        }
        return null;
    }
}
