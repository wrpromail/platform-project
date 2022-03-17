package net.coding.app.project.grpc.open;

import net.coding.app.project.utils.ProtoConvertUtils;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.common.GRpcMetadataContextHolder;
import net.coding.lib.project.entity.Project;
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
import proto.open.api.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static proto.open.api.CodeProto.Code.INVALID_PARAMETER;
import static proto.open.api.CodeProto.Code.NOT_FOUND;
import static proto.open.api.CodeProto.Code.SUCCESS;

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
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            Project project = projectService.getByNameAndTeamId(request.getProjectName(), request.getUser().getTeamId());
            if (project == null) {
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
            DescribeProjectResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase(), project);
        } catch (CoreException e) {
            DescribeProjectResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()), null);
        } catch (Exception e) {
            log.error("rpcService describeProjectByName error Exception ", e);
            DescribeProjectResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), null);
        }
    }

    private void DescribeProjectResponse(
            StreamObserver<ProjectProto.DescribeProjectByNameResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project project) {
        CommonProto.Result result = CommonProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectProto.DescribeProjectByNameResponse.Builder builder = ProjectProto
                .DescribeProjectByNameResponse.newBuilder()
                .setResult(result);

        if (Objects.nonNull(project)) {
            builder.setProject(protoConvertUtils.describeProjectToProjectProto(project));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
