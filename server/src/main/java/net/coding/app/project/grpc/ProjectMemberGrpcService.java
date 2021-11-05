package net.coding.app.project.grpc;


import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.RoleTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.proto.platform.project.ProjectMemberProto;
import net.coding.proto.platform.project.ProjectMemberServiceGrpc;

import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;

@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectMemberGrpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    @Override
    public void addProjectMember(
            ProjectMemberProto.AddProjectMemberRequest request,
            StreamObserver<ProjectMemberProto.AddProjectMemberResponse> responseObserver) {
        try {
            if (ObjectUtils.isEmpty(RoleTypeEnum.of(request.getType()))) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            Project project = projectService.getById(request.getProjectIdCoding());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            UserProto.User currentUser = userGrpcClient.getUserByGlobalKey(request.getUserGk());
            if (currentUser == null) {
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectMember)
                            .setAction(PermissionProto.Action.Create)
                            .build(),
                    request.getProjectIdCoding(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            List<Integer> targetUserIds = new ArrayList<>();
            Arrays.stream(request.getMemberGks().split(",")).forEach(targetUserIdStr -> {
                UserProto.User user = userGrpcClient.getUserByGlobalKey(targetUserIdStr);
                if (!ObjectUtils.isEmpty(user) && user.getId() != currentUser.getId()) {
                    Integer id = user.getId();
                    targetUserIds.add(id);
                }
            });
            if (CollectionUtils.isEmpty(targetUserIds)) {
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            projectMemberService.doAddMember(currentUser.getId(), targetUserIds, (short) request.getType(), project, false);
            responseObserver.onNext(ProjectMemberProto.AddProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .build());
        } catch (CoreException e) {
            responseObserver.onNext(ProjectMemberProto.AddProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("RpcService AddProjectMember error CoreException ", e);
            responseObserver.onNext(ProjectMemberProto.AddProjectMemberResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void delProjectMember(
            ProjectMemberProto.DelProjectMemberRequest request,
            StreamObserver<ProjectMemberProto.DelProjectMemberResponse> responseObserver) {
        ProjectMemberProto.DelProjectMemberResponse.Builder builder = ProjectMemberProto.DelProjectMemberResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            ProjectMember member = projectMemberService.getByProjectIdAndUserId(project.getId(), request.getTargetUserId());
            if (member == null) {
                log.error("User {} is not member of project {}", request.getTargetUserId(), project.getId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
            }
            projectMemberService.delMember(request.getCurrentUserId(), project, request.getTargetUserId(), member);
            builder.setCode(CodeProto.Code.SUCCESS);
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.NOT_FOUND).setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("rpcService delProjectMember error Exception ", e);
            builder.setCode(CodeProto.Code.INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
