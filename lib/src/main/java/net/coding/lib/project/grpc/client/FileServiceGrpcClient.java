package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.FileProto;
import net.coding.e.proto.FileServiceGrpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileServiceGrpcClient extends EndpointGrpcClient<FileServiceGrpc.FileServiceBlockingStub> {

    @Value("${grpc.client.file.serviceName:e-file}")
    private String serviceName;

    @Value("${grpc.client.file.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public FileProto.File getProjectFileByIdWithDel(Integer projectId, Integer id) {
        log.info("FileServiceGrpcClient.getProjectFileById() projectId={}, id={}", projectId, id);
        FileProto.GetProjectFileByIdRequest request = FileProto.GetProjectFileByIdRequest.newBuilder()
                .setProjectId(projectId)
                .setId(id)
                .build();
        FileProto.GetProjectFileByIdResponse response = newStub().getProjectFileByIdWithDel(request);
        log.info("FileServiceGrpcClient.getProjectFileById() response={}", response.toString());
        if(response.getCode() == 0) {
            return response.getFile();
        }
        return null;
    }
}
