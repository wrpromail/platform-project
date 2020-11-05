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

    public FileProto.ProjectFile getProjectFile(Integer projectId, Integer fileId) {
        log.info("FileServiceGrpcClient.getProjectFile() projectId={}", projectId);
        FileProto.GetProjectFileRequest request = FileProto.GetProjectFileRequest.newBuilder()
                .setFileId(fileId)
                .setProjectId(projectId)
                .build();
        FileProto.GetProjectFileResponse response = newStub().getProjectFile(request);
        log.info("FileServiceGrpcClient.getProjectFile() response={}", response.toString());
        if(response.getCode() == 0) {
            return response.getProjectFile();
        }
        return null;
    }

    public FileProto.File getById(Integer fileId) {
        log.info("FileServiceGrpcClient.getProjectFile() fileId={}", fileId);
        FileProto.GetFileByIdRequest request = FileProto.GetFileByIdRequest.newBuilder()
                .setId(fileId)
                .build();
        FileProto.GetFileByIdResponse response = newStub().getById(request);
        log.info("FileServiceGrpcClient.getProjectFile() file={}", response.toString());
        if(response.getCode() == 0) {
            return response.getFile();
        }
        return null;
    }
}
