package net.coding.client.project;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
import proto.projectResources.ProjectResourceServiceGrpc;
import proto.projectResources.ProjectResourcesProto;

@Slf4j
public class ProjectResourcesGrpcClient extends EndpointGrpcClient<ProjectResourceServiceGrpc.ProjectResourceServiceBlockingStub> {
    @Value("${grpc.client.starter.serviceName:starter}")
    private String serviceName;

    @Value("${grpc.client.starter.servicePort:20153}")
    private int servicePort;

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    /**
     * 添加项目资源
     * @param projectId
     * @param title
     * @param targetId
     * @param userId
     * @param targetType
     * @param idempotentKey
     * @return
     */
    public int addProjectResources(Integer projectId, String title, Integer targetId, Integer userId, String targetType, String idempotentKey)  {
        try {
            ProjectResourcesProto.AddProjectResourcesRequest request = ProjectResourcesProto.AddProjectResourcesRequest.newBuilder()
                    .setProjectId(projectId)
                    .setTitle(title)
                    .setTargetId(targetId)
                    .setUserId(userId)
                    .setTargetType(targetType)
                    .setIdempotentKey(idempotentKey)
                    .build();
            ProjectResourcesProto.ProjectResourcesResponse response = newStub().addProjectResources(request);
            return response != null ? response.getResourcesCode() : 0;
        } catch (Exception ex) {
            log.error("addProjectResources() projectId={}, title={}, targetId={}, userId={}, targetType={}, idempotentKey={}, ex={}",
                    projectId, title, targetId, userId, targetType, idempotentKey,ex);
        }
        return 0;
    }

    /**
     * 修改项目资源
     * @param projectId
     * @param title
     * @param targetId
     * @param userId
     * @param targetType
     * @param code
     * @return
     */
    public int updateProjectResources(Integer projectId, String title, Integer targetId, Integer userId, String targetType, Integer code)  {
        try {
            ProjectResourcesProto.UpdateProjectResourcesRequest request = ProjectResourcesProto.UpdateProjectResourcesRequest.newBuilder()
                    .setProjectId(projectId)
                    .setTitle(title)
                    .setTargetId(targetId)
                    .setUserId(userId)
                    .setTargetType(targetType)
                    .setCode(code)
                    .build();
            ProjectResourcesProto.ProjectResourcesResponse response = newStub().updateProjectResources(request);
            return response != null ? response.getResourcesCode() : 0;
        } catch (Exception ex) {
                log.error("updateProjectResources() projectId={}, title={}, targetId={}, userId={}, targetType={}, idempotentKey={}, ex={}",
                        projectId, title, targetId, userId, targetType, code,ex);
        }
        return 0;
    }


    /**
     * 修改项目资源
     * @param projectId
     * @param code
     * @param userId
     * @return
     */
    public int deleteProjectResources(Integer projectId, Integer code, Integer userId)  {
        try {
            ProjectResourcesProto.DeleteProjectResourcesRequest request = ProjectResourcesProto.DeleteProjectResourcesRequest.newBuilder()
                    .setProjectId(projectId)
                    .setCode(code)
                    .setUserId(userId)
                    .build();
            ProjectResourcesProto.ProjectResourcesResponse response = newStub().deleteProjectResources(request);
            return response != null ? response.getResourcesCode() : 0;
        } catch (Exception ex) {
            log.error("deleteProjectResources() projectId={}, code={}, userId={}, ex={}",
                    projectId, code, userId, ex);
        }
        return 0;
    }

    /**
     * 根据项目id与资源编号查询项目资源信息
     * @param projectId
     * @param code
     * @return
     */
    public ProjectResourcesProto.GetProjectResourcesResponse getProjectResources(Integer projectId, Integer code) {
        ProjectResourcesProto.GetProjectResourcesRequest request = ProjectResourcesProto.GetProjectResourcesRequest.newBuilder()
                .setProjectId(projectId)
                .setCode(code)
                .build();
        ProjectResourcesProto.GetProjectResourcesResponse response = newStub().getProjectResources(request);
        return response;
    }

    public ProjectResourcesProto.FindProjectResourcesResponse findProjectResourcesList(Integer projectId, Integer page, Integer pageSize) {
        ProjectResourcesProto.Page pager = ProjectResourcesProto.Page.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build();
        ProjectResourcesProto.FindProjectResourcesRequest request = ProjectResourcesProto.FindProjectResourcesRequest.newBuilder()
                .setProjectId(projectId)
                .setPage(pager)
                .build();
        ProjectResourcesProto.FindProjectResourcesResponse response = newStub().findProjectResourcesList(request);
        return response;
    }
}
