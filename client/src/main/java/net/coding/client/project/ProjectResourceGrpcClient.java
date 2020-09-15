package net.coding.client.project;

import net.coding.common.rpc.client.EndpointGrpcClient;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import proto.common.PagerProto;
import proto.projectResource.ProjectResourceProto;
import proto.projectResource.ProjectResourceServiceGrpc;

@Slf4j
public class ProjectResourceGrpcClient extends EndpointGrpcClient<ProjectResourceServiceGrpc.ProjectResourceServiceBlockingStub> {

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
     *
     * @param projectId
     * @param title
     * @param targetId
     * @param userId
     * @param targetType
     * @param resourceUrl
     * @return
     */
    public ProjectResourceProto.ProjectResourceResponse addProjectResource(Integer projectId, String title, Integer targetId,
                                                                           Integer userId, String targetType, String resourceUrl) {
        ProjectResourceProto.AddProjectResourceRequest request = ProjectResourceProto.AddProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setTitle(title)
                .setTargetId(targetId)
                .setUserId(userId)
                .setTargetType(targetType)
                .setResourceUrl(resourceUrl)
                .build();
        ProjectResourceProto.ProjectResourceResponse response = newStub().addProjectResource(request);
        return response;
    }

    /**
     * 修改项目资源
     *
     * @param projectId
     * @param title
     * @param targetId
     * @param userId
     * @param targetType
     * @param code
     * @return
     */
    public ProjectResourceProto.ProjectResourceResponse updateProjectResource(Integer projectId, String title, Integer targetId,
                                                                              Integer userId, String targetType, Integer code, String resourceUrl) {
        ProjectResourceProto.OperateProjectResourceRequest request = ProjectResourceProto.OperateProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setTitle(title)
                .setTargetId(targetId)
                .setUserId(userId)
                .setTargetType(targetType)
                .setCode(code)
                .setResourceUrl(resourceUrl)
                .build();
        ProjectResourceProto.ProjectResourceResponse response = newStub().updateProjectResource(request);
        return response;
    }


    /**
     * 删除项目资源
     *
     * @param projectId
     * @param targetType
     * @param targetIds
     * @param userId
     * @return
     */
    public ProjectResourceProto.ProjectResourceCommonResponse deleteProjectResource(Integer projectId, String targetType, List<Integer> targetIds, Integer userId) {
        ProjectResourceProto.DeleteProjectResourceRequest request = ProjectResourceProto.DeleteProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setTargetType(targetType)
                .addAllTargetId(targetIds)
                .setUserId(userId)
                .build();
        ProjectResourceProto.ProjectResourceCommonResponse response = newStub().deleteProjectResource(request);
        return response;
    }

    /**
     * 根据项目id及分页信息查询资源信息
     *
     * @param projectId
     * @param page
     * @param pageSize
     * @return
     */
    public ProjectResourceProto.FindProjectResourceResponse findProjectResourceList(Integer projectId, Integer page, Integer pageSize) {
        PagerProto.PageRequest pager = PagerProto.PageRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build();
        ProjectResourceProto.FindProjectResourceRequest request = ProjectResourceProto.FindProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setPageRequest(pager)
                .build();
        ProjectResourceProto.FindProjectResourceResponse response = newStub().findProjectResourceList(request);
        return response;
    }


    /**
     * 批量获取项目资源
     *
     * @param projectId
     * @param codes
     * @return
     */
    public ProjectResourceProto.BatchProjectResourceResponse batchProjectResourceList(Integer projectId, List<Integer> codes) {
        ProjectResourceProto.BatchProjectResourceRequest request = ProjectResourceProto.BatchProjectResourceRequest.newBuilder()
                .addAllCode(codes)
                .setProjectId(projectId)
                .build();
        ProjectResourceProto.BatchProjectResourceResponse response = newStub().batchProjectResourceList(request);
        return response;
    }

    /**
     * 根据项目id与资源编号查询项目资源信息
     *
     * @param projectId
     * @param code
     * @return
     */
    public ProjectResourceProto.ProjectResourceResponse GetProjectResourceByCode(Integer projectId, Integer code) {
        ProjectResourceProto.GetProjectResourceRequest request = ProjectResourceProto.GetProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setCode(code)
                .build();
        ProjectResourceProto.ProjectResourceResponse response = newStub().getProjectResourceByCode(request);
        return response;
    }

    /**
     * 生成一定长度的项目资源
     *
     * @param projectId
     * @param codeAmount
     * @return
     */
    public ProjectResourceProto.MultiCodeResponse generateCodes(Integer projectId, Integer codeAmount) {
        ProjectResourceProto.GenerateRequest request = ProjectResourceProto.GenerateRequest.newBuilder()
                .setProjectId(projectId)
                .setCodeAmount(codeAmount)
                .build();
        ProjectResourceProto.MultiCodeResponse response = newStub().generateCodes(request);
        return response;
    }

    /**
     * 补充项目资源
     *
     * @param projectId
     * @param title
     * @param targetId
     * @param userId
     * @param targetType
     * @param code
     * @return
     */
    public ProjectResourceProto.ProjectResourceResponse relateResource(Integer projectId, String title, Integer targetId, Integer userId,
                                                                       String targetType, Integer code, String resourceUrl) {
        ProjectResourceProto.OperateProjectResourceRequest request = ProjectResourceProto.OperateProjectResourceRequest.newBuilder()
                .setProjectId(projectId)
                .setTitle(title)
                .setTargetId(targetId)
                .setUserId(userId)
                .setTargetType(targetType)
                .setCode(code)
                .setResourceUrl(resourceUrl)
                .build();
        ProjectResourceProto.ProjectResourceResponse response = newStub().relateResource(request);
        return response;
    }

    /**
     * 批量补充项目资源
     *
     * @param lists
     * @return
     */
    public ProjectResourceProto.ProjectResourceCommonResponse batchRelateResource(List<ProjectResourceProto.OperateProjectResourceRequest> lists) {
        ProjectResourceProto.BatchRelateResourceRequest request = ProjectResourceProto.BatchRelateResourceRequest.newBuilder()
                .addAllOperateProjectResourceRequest(lists)
                .build();
        ProjectResourceProto.ProjectResourceCommonResponse response = newStub().batchRelateResource(request);
        return response;
    }


    /**
     * 根据项目id与目标类型及目标id查询项目资源
     *
     * @param projectId
     * @param targetType
     * @param targetId
     * @return
     */
    public ProjectResourceProto.ProjectResourceResponse getProjectResourceByTypeAndTarget(Integer projectId, String targetType, Integer targetId) {
        ProjectResourceProto.TypeAndTargetRequest request = ProjectResourceProto.TypeAndTargetRequest.newBuilder()
                .setProjectId(projectId)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .build();
        ProjectResourceProto.ProjectResourceResponse response = newStub().getProjectResourceByTypeAndTarget(request);
        return response;
    }

    /**
     * 根据项目id与目标类型及目标id查询项目资源
     *
     * @param projectResourceId
     * @return
     */
    public ProjectResourceProto.GetResourceLinkResponse getResourceLink(Integer projectResourceId) {
        ProjectResourceProto.GetResourceLinkRequest request = ProjectResourceProto.GetResourceLinkRequest.newBuilder()
                .setProjectResourceId(projectResourceId)
                .build();
        ProjectResourceProto.GetResourceLinkResponse response = newStub().getResourceLink(request);
        return response;
    }
}
