package net.coding.lib.project.grpc.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.storage.StorageProto;
import proto.storage.StorageServiceGrpc;

@Slf4j
@Component
public class StorageGrpcClient extends EndpointGrpcClient<StorageServiceGrpc.StorageServiceBlockingStub> {

    @Value("${grpc.client.storage.service.serviceName:storage-service}")
    private String serviceName;

    @Value("${grpc.client.storage.service.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public Boolean store(String key, byte[] content, String bucket) throws Exception {
        StorageProto.StoreRequest request = StorageProto.StoreRequest.newBuilder()
                .setBucket(StringValue.of(bucket))
                .setKey(key)
                .setContent(ByteString.copyFrom(content))
                .build();
        StorageProto.StoreResponse response = newStub().store(request);
        CodeProto.Code code = response.getCode();
        if (code.getNumber() == 4 || code.getNumber() == 3) {
            throw new Exception(response.getMessage());
        }
        return true;
    }

    public String getDownloadUrl(String bucket, String key) throws Exception {
        StorageProto.GetDownloadUrlRequest request = StorageProto.GetDownloadUrlRequest.newBuilder()
                .setBucket(bucket)
                .setKey(key)
                .build();
        StorageProto.GetDownloadUrlResponse response = newStub().getDownloadUrl(request);
        CodeProto.Code code = response.getCode();
        if (code.getNumber() == 4 || code.getNumber() == 3) {
            throw new Exception(response.getMessage());
        }
        return response.getUrl();
    }
}
