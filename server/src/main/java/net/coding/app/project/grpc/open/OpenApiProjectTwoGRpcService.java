package net.coding.app.project.grpc.open;

import net.coding.app.project.utils.ProtoConvertUtils;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.proto.ApiCodeProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.common.GRpcMetadataContextHolder;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.interceptor.GRpcHeaderServerInterceptor;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.proto.open.api.project.ProjectProto;
import net.coding.proto.open.api.project.ProjectServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.Objects;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

/**
 * @Description: OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 * @Author liheng
 * @Date 2021/1/4 4:16 下午
 */
@Slf4j
@GRpcService(interceptors = GRpcHeaderServerInterceptor.class)
@AllArgsConstructor
public class OpenApiProjectTwoGRpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final ProtoConvertUtils protoConvertUtils;

    private final LocaleMessageSource localeMessageSource;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final UserGrpcClient userGrpcClient;

    @Override
    public void describeProjectByName(
            ProjectProto.DescribeProjectByNameRequest request,
            StreamObserver<ProjectProto.DescribeProjectByNameResponse> responseObserver) {
        ProjectProto.DescribeProjectByNameResponse.Builder builder =
                ProjectProto.DescribeProjectByNameResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            Project project = projectService.getByNameAndTeamId(request.getProjectName(), request.getUser().getTeamId());
            if (Objects.isNull(project)) {
                throw CoreException.of(PROJECT_NOT_EXIST);
            }
            String projectId = GRpcMetadataContextHolder.get().getDeployTokenProjectId();
            if (StringUtils.isNotBlank(projectId)
                    && !projectId.equals(String.valueOf(project.getId()))) {
                throw CoreException.of(PERMISSION_DENIED);
            } else {
                boolean isMember = projectMemberService.isMember(currentUser, project.getId());
                if (!isMember) {
                    //验证用户接口权限
                    boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                            PermissionProto.Permission.newBuilder()
                                    .setFunction(PermissionProto.Function.EnterpriseProject)
                                    .setAction(PermissionProto.Action.View)
                                    .build(),
                            project.getId(),
                            currentUser.getGlobalKey(),
                            currentUser.getId()
                    );
                    if (!hasPermissionInProject) {
                        throw CoreException.of(PERMISSION_DENIED);
                    }
                }
            }
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            )
                    .setProject(protoConvertUtils.describeProjectToProjectProto(project));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeProjectByName error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.INVALID_PARAMETER.getNumber())
                            .setMessage(ApiCodeProto.Code.INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    public void describeOneProject(
            ProjectProto.DescribeOneProjectRequest request,
            StreamObserver<ProjectProto.DescribeOneProjectResponse> responseObserver) {
        ProjectProto.DescribeOneProjectResponse.Builder builder =
                ProjectProto.DescribeOneProjectResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Project project = projectService.getByIdAndTeamId(
                    request.getProjectId(),
                    request.getUser().getTeamId()
            );
            if (Objects.isNull(project)) {
                throw CoreException.of(PROJECT_NOT_EXIST);
            }
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(
                    request.getProjectId(),
                    request.getUser().getId()
            );
            if (Objects.isNull(projectMember)) {
                throw CoreException.of(PROJECT_NOT_EXIST);
            }
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            )
                    .setProject(protoConvertUtils.describeProjectToProjectProto(project));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeOneProject error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.INVALID_PARAMETER.getNumber())
                            .setMessage(ApiCodeProto.Code.INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
