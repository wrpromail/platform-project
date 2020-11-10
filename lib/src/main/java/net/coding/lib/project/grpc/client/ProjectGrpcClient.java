package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import proto.platform.project.ProjectProto;
import proto.platform.project.ProjectServiceGrpc;

@Slf4j
@Component
public class ProjectGrpcClient extends EndpointGrpcClient<ProjectServiceGrpc.ProjectServiceBlockingStub> {
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

    public boolean isProjectRobotUser(String userGlobalKey) {
        try {
            ProjectProto.IsProjectRobotUserRequest request = ProjectProto.IsProjectRobotUserRequest.newBuilder()
                    .setUserGK(userGlobalKey)
                    .build();
            ProjectProto.GetIsProjectRobotUserResponse response = newStub().isProjectRobotUser(request);
            return response.getResult();
        } catch (Exception ex) {
            log.error("UserGrpcClient->isProjectRobotUser() userGlobalKey={}, ex={}", userGlobalKey, ex);
        }
        return false;
    }

    /**
     * 先调platform-service，后续再迁移过来
     * @return
     */
    public String getProjectPath(Integer projectId) {
        try {
            ProjectProto.GetProjectByIdRequest request = ProjectProto.GetProjectByIdRequest.newBuilder()
                    .setProjectId(projectId)
                    .build();
            ProjectProto.GetProjectResponse response = newStub().getProjectById(request);
            return response.getData().getProjectPath();
        } catch (Exception ex) {
            log.error("UserGrpcClient->getProjectPath() projectId={}, ex={}", projectId, ex);
        }
        return null;
    }

    /**
     * 获取htmlUrl
     * @param projectId
     * @return
     */
    public ProjectProto.Project getProjectById(Integer projectId) {
        try {
            ProjectProto.GetProjectByIdRequest request = ProjectProto.GetProjectByIdRequest.newBuilder()
                    .setProjectId(projectId)
                    .build();
            ProjectProto.GetProjectResponse response = newStub().getProjectById(request);
            return response.getData();
        } catch (Exception ex) {
            log.error("UserGrpcClient->getHtmlUrl() projectId={}, ex={}", projectId, ex);
        }
        return null;
    }
}
