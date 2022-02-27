package net.coding.app.project.grpc;


import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;
import static proto.common.CodeProto.Code.INTERNAL_ERROR;
import static proto.common.CodeProto.Code.NOT_FOUND;
import static proto.common.CodeProto.Code.SUCCESS;

@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectMemberGrpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final UserGrpcClient userGrpcClient;

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

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
                    .setCode(SUCCESS)
                    .build());
        } catch (CoreException e) {
            responseObserver.onNext(ProjectMemberProto.AddProjectMemberResponse.newBuilder()
                    .setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("RpcService AddProjectMember error CoreException ", e);
            responseObserver.onNext(ProjectMemberProto.AddProjectMemberResponse.newBuilder()
                    .setCode(INTERNAL_ERROR)
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
                throw CoreException.of(PROJECT_MEMBER_NOT_EXISTS);
            }
            projectMemberService.delMember(request.getCurrentUserId(), project, request.getTargetUserId(), member);
            builder.setCode(SUCCESS);
        } catch (CoreException e) {
            builder.setCode(NOT_FOUND).setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("rpcService delProjectMember error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void findProjectMembersByProjectId(ProjectMemberProto.FindProjectMembersByProjectIdRequest request,
                                              StreamObserver<ProjectMemberProto.FindProjectMembersResponse> responseObserver) {
        ProjectMemberProto.FindProjectMembersResponse.Builder builder = ProjectMemberProto.FindProjectMembersResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            List<ProjectMember> members = projectMemberService.findListByProjectId(project.getId());
            builder.setCode(SUCCESS).addAllData(toProtoProjectMembers(project.getId(), members));
        } catch (CoreException e) {
            log.error("rpcService findProjectMembersByProjectId error CoreException ", e);
            builder.setCode(NOT_FOUND).setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectMemberByProjectIdAndUserId(ProjectMemberProto.GetProjectMemberByProjectIdAndUserIdRequest request,
                                                     StreamObserver<ProjectMemberProto.GetProjectMemberResponse> responseObserver) {
        ProjectMemberProto.GetProjectMemberResponse.Builder builder = ProjectMemberProto.GetProjectMemberResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            ProjectMember member = projectMemberService.getByProjectIdAndUserId(project.getId(), request.getUserId());
            if (Objects.isNull(member)) {
                throw CoreException.of(PROJECT_MEMBER_NOT_EXISTS);
            }
            builder.setCode(SUCCESS).setData(toProtoProjectMember(member));
        } catch (CoreException e) {
            log.error("rpcService getProjectMemberByProjectIdAndUserId error CoreException ", e);
            builder.setCode(NOT_FOUND).setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void findProjectMembersByProjectIdAndUserIds(ProjectMemberProto.FindProjectMemberByProjectIdAndUserIdsRequest request,
                                                        StreamObserver<ProjectMemberProto.FindProjectMembersResponse> responseObserver) {
        ProjectMemberProto.FindProjectMembersResponse.Builder builder = ProjectMemberProto.FindProjectMembersResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            List<ProjectMember> members = StreamEx.of(request.getUserIdsList())
                    .map(userId -> projectMemberService.getByProjectIdAndUserId(project.getId(), userId))
                    .nonNull()
                    .toList();
            builder.setCode(SUCCESS).addAllData(toProtoProjectMembers(project.getId(), members));
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void findProjectMembersByProjectIdAndRoleId(ProjectMemberProto.FindProjectMemberByProjectIdAndRoleIdRequest request,
                                                       StreamObserver<ProjectMemberProto.FindProjectMembersResponse> responseObserver) {
        ProjectMemberProto.FindProjectMembersResponse.Builder builder = ProjectMemberProto.FindProjectMembersResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            List<Integer> userIds = advancedRoleServiceGrpcClient.findUsersOfRole(AclProto.Role.newBuilder().setId(request.getRoleId()).build());
            List<ProjectMember> members = StreamEx.of(projectMemberService.findListByProjectId(project.getId()))
                    .nonNull()
                    .filter(member -> userIds.contains(member.getUserId()))
                    .toList();
            builder.setCode(SUCCESS).addAllData(toProtoProjectMembers(project.getId(), members));
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void findProjectMembersByProjectIdsAndUserId(ProjectMemberProto.FindProjectMemberByProjectIdsAndUserIdRequest request,
                                                        StreamObserver<ProjectMemberProto.FindProjectMembersResponse> responseObserver) {
        ProjectMemberProto.FindProjectMembersResponse.Builder builder = ProjectMemberProto.FindProjectMembersResponse.newBuilder();
        try {
            List<ProjectMemberProto.ProjectMember> members = StreamEx.of(request.getProjectIdsList())
                    .map(projectId -> Optional.ofNullable(projectService.getById(projectId))
                            .flatMap(project -> Optional.of(projectMemberService.getByProjectIdAndUserId(project.getId(), request.getUserId()))
                                    .map(this::toProtoProjectMember))
                            .orElse(null))
                    .nonNull()
                    .toList();
            builder.setCode(SUCCESS).addAllData(members);
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void isProjectMember(ProjectMemberProto.IsProjectMemberRequest request,
                                StreamObserver<ProjectMemberProto.IsProjectMemberResponse> responseObserver) {
        ProjectMemberProto.IsProjectMemberResponse.Builder builder = ProjectMemberProto.IsProjectMemberResponse.newBuilder();
        try {
            UserProto.User user = userGrpcClient.getUserById(request.getUserId());
            if (user == null) {
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            boolean isMember = projectMemberService.isMember(user, request.getProjectId());
            builder.setCode(SUCCESS).setResult(isMember);
        } catch (Exception e) {
            log.error("rpcService isProjectMember error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private ProjectMemberProto.ProjectMember toProtoProjectMember(ProjectMember member) {
        return memberToProto(member.getProjectId(), userGrpcClient.getUserById(member.getUserId()));
    }

    private List<ProjectMemberProto.ProjectMember> toProtoProjectMembers(Integer projectId, List<ProjectMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        UserProto.FindUserResponse response = userServiceGrpcClient.findUserByIds(StreamEx.of(members).map(ProjectMember::getUserId).toList());
        return StreamEx.of(response.getDataList())
                .nonNull()
                .map(user -> memberToProto(projectId, user))
                .toList();
    }

    public ProjectMemberProto.ProjectMember memberToProto(Integer projectId, UserProto.User user) {
        if (Objects.isNull(user)) {
            return ProjectMemberProto.ProjectMember.newBuilder().build();
        }
        return ProjectMemberProto.ProjectMember.newBuilder()
                .setProjectId(projectId)
                .setUserId(user.getId())
                .setName(user.getName())
                .setNamePinyin(user.getNamePinyin())
                .setGlobalKey(user.getGlobalKey())
                .setEmail(user.getEmail())
                .setPhone(user.getPhone())
                .setAvatar(user.getAvatar())
                .build();
    }
}
