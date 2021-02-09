package net.coding.app.project.grpc;

import com.google.gson.internal.$Gson$Preconditions;

import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.proto.platform.project.ProjectLabelProto;
import net.coding.proto.platform.project.ProjectMemberProto;
import net.coding.proto.platform.project.ProjectMemberServiceGrpc;

import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static proto.open.api.CodeProto.Code.SUCCESS;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectMemberGrpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;


    @Override
    public void operateProjectMember(ProjectMemberProto.OperateProjectMemberRequest request,
                                     io.grpc.stub.StreamObserver<ProjectMemberProto.OperateProjectMemberResponse> responseObserver) {
        Project project = projectService.getById(request.getProjectId());
        try {
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUserId());

            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectMember)
                            .setAction(PermissionProto.Action.Create)
                            .build(),
                    request.getProjectId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            UserProto.User targetUser = userGrpcClient.getUserById(request.getTargetUserId());

            if (projectMemberService.isMember(targetUser, request.getProjectId())) {
                projectMemberService.updateProjectMemberType(request.getProjectId(), request.getTargetUserId(), (short) request.getRoleValue());
            } else {
                List<Integer> userIds = new ArrayList<>();
                userIds.add(request.getTargetUserId());
                projectMemberService.doAddMember(request.getUserId(), userIds, (short) request.getRoleValue(), project, false);
            }
            responseObserver.onNext(ProjectMemberProto.OperateProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .build());

        } catch (CoreException e) {
            log.error("RpcService operateProjectMember error CoreException ", e);
            responseObserver.onNext(ProjectMemberProto.OperateProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());

        } catch (Exception e) {
            log.error("RpcService operateProjectMember error CoreException ", e);
            responseObserver.onNext(ProjectMemberProto.OperateProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }
}
