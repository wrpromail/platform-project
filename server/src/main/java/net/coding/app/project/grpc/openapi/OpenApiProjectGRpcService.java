package net.coding.app.project.grpc.openapi;

import net.coding.app.project.utils.ProtoConvertUtils;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.enums.RegisterSourceEnum;
import net.coding.lib.project.enums.RoleType;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.openapi.OpenApiProjectService;

import org.apache.commons.collections.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.List;
import java.util.Optional;


import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.acl.AclProto;
import proto.open.api.CodeProto;
import proto.open.api.ResultProto;
import proto.open.api.project.ProjectProto;
import proto.open.api.project.ProjectServiceGrpc;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static proto.open.api.CodeProto.Code.INVALID_PARAMETER;
import static proto.open.api.CodeProto.Code.NOT_FOUND;
import static proto.open.api.CodeProto.Code.SUCCESS;

/**
 * @Description: OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 * @Author liheng
 * @Date 2021/1/4 4:16 下午
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProjectGRpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final OpenApiProjectService openApiProjectService;

    private final ProtoConvertUtils protoConvertUtils;

    private final TeamGrpcClient teamGrpcClient;

    private final TeamServiceGrpcClient teamServiceGrpcClient;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    /**
     * 查询项目列表 根据label查询
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void describeProjectLabels(
            ProjectProto.DescribeProjectsRequest request,
            StreamObserver<ProjectProto.DescribeProjectsResponse> responseObserver) {
        try {
            // SLS 查询出主账号所有项目
            Integer userId = request.getUser().getId();
            if (ProjectLabelEnums.SLS.name().equals(request.getLabel())) {
                TeamProto.GetTeamResponse response =
                        teamGrpcClient.getTeam(request.getUser().getTeamId());
                if (response == null || response.getData() == null) {
                    throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
                }
                userId = response.getData().getOwner().getId();
            }
            List<Project> projects = projectService.getProjects(
                    ProjectQueryParameter.builder()
                            .userId(userId)
                            .teamId(request.getUser().getTeamId())
                            .label(request.getLabel())
                            .invisible(0)
                            .build()
            );
            DescribeProjectsResponse(responseObserver, SUCCESS,
                    SUCCESS.name().toLowerCase(), projects);
        } catch (CoreException e) {
            log.error("RpcService describeProjectLabels error CoreException ", e);
            DescribeProjectsResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()), null);
        } catch (Exception e) {
            log.error("rpcService describeProjectLabels error Exception ", e);
            DescribeProjectsResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), null);
        }
    }

    @Override
    public void describeUserProjects(
            ProjectProto.DescribeUserProjectsRequest request,
            StreamObserver<ProjectProto.DescribeProjectsResponse> responseObserver) {
        try {
            Integer currentUserId = request.getUser().getId();
            Integer currentTeamId = request.getUser().getTeamId();

            boolean isMember = teamServiceGrpcClient.isMember(
                    currentTeamId,
                    request.getUserId());
            if (!isMember) {
                throw CoreException.of(CoreException.ExceptionType.TEAM_MEMBER_NOT_EXISTS);
            }

            List<AclProto.Role> roles =
                    advancedRoleServiceGrpcClient.findUserRolesInEnterprise(currentTeamId, currentUserId);
            if (CollectionUtils.isEmpty(roles)) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            //管理员可查询团队内其他成员所在项目，普通成员只能查询自己
            if (roles.stream().anyMatch(r -> RoleType.valueOf(r.getType()) == RoleType.EnterpriseMember)
                    && currentUserId != request.getUserId()) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            List<Project> projects = projectService.getProjects(
                    ProjectQueryParameter.builder()
                            .userId(request.getUserId())
                            .teamId(currentTeamId)
                            .projectName(request.getProjectName())
                            .build()
            );
            DescribeProjectsResponse(responseObserver, SUCCESS,
                    SUCCESS.name().toLowerCase(), projects);
        } catch (CoreException e) {
            log.error("RpcService describeUserProjects error CoreException ", e);
            DescribeProjectsResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()), null);
        } catch (Exception e) {
            log.error("rpcService describeUserProjects error Exception ", e);
            DescribeProjectsResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), null);
        }
    }

    @Override
    public void describeOneProject(
            ProjectProto.DescribeOneProjectRequest request,
            StreamObserver<ProjectProto.DescribeOneProjectResponse> responseObserver) {
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(
                    request.getProjectId(),
                    request.getUser().getId());
            if (projectMember == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            DescribeProjectResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase(), project);
        } catch (CoreException e) {
            log.error("RpcService describeOneProject error CoreException ", e);
            DescribeProjectResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()), null);
        } catch (Exception e) {
            log.error("rpcService describeOneProject error Exception ", e);
            DescribeProjectResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), null);
        }
    }

    @Override
    public void createProjectWithTemplate(
            ProjectProto.CreateProjectWithTemplateRequest request,
            StreamObserver<ProjectProto.CreateProjectWithTemplateResponse> responseObserver) {
        try {
            Integer projectId = openApiProjectService.createProject(request,
                    RegisterSourceEnum.QCLOUD_API.name());
            createProjectWithTemplateResponse(responseObserver, SUCCESS,
                    SUCCESS.name().toLowerCase(), projectId);
        } catch (CoreException e) {
            log.error("RpcService createProjectWithTemplate error CoreException ", e);
            createProjectWithTemplateResponse(responseObserver, NOT_FOUND,
                    e.getMessage(), 0);
        } catch (Exception e) {
            log.error("rpcService createProjectWithTemplate error Exception ", e);
            createProjectWithTemplateResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), 0);
        }
    }

    @Override
    public void createCodingProject(
            ProjectProto.CreateProjectWithTemplateRequest request,
            StreamObserver<ProjectProto.CreateProjectWithTemplateResponse> responseObserver) {
        try {
            Integer projectId = openApiProjectService.createProject(request,
                    RegisterSourceEnum.OPEN_API.name());
            createProjectWithTemplateResponse(responseObserver, SUCCESS,
                    SUCCESS.name().toLowerCase(), projectId);
        } catch (CoreException e) {
            log.error("RpcService createCodingProject error CoreException ", e);
            createProjectWithTemplateResponse(responseObserver, NOT_FOUND,
                    e.getMessage(), 0);
        } catch (Exception e) {
            log.error("rpcService createCodingProject error Exception ", e);
            createProjectWithTemplateResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), 0);
        }
    }

    @Override
    public void deleteOneProject(
            ProjectProto.DescribeOneProjectRequest request,
            StreamObserver<ResultProto.CommonResult> responseObserver) {
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.EnterpriseProject)
                            .setAction(PermissionProto.Action.Delete)
                            .build(),
                    request.getProjectId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }
            projectService.delete(currentUser.getId(), currentUser.getTeamId(), request.getProjectId());
            CommonResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase());
        } catch (CoreException e) {
            log.error("RpcService deleteOneProject error CoreException ", e);
            CommonResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService deleteOneProject error Exception ", e);
            CommonResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase());
        }
    }

    @Override
    public void modifyProject(ProjectProto.ModifyProjectRequest request,
                              StreamObserver<ResultProto.CommonResult> responseObserver) {
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            Project project = projectService.getByIdAndTeamId(
                    request.getProjectId(),
                    request.getUser().getTeamId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.EnterpriseProject)
                            .setAction(PermissionProto.Action.Update)
                            .build(),
                    request.getProjectId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
            }

            int result = projectService.update(
                    UpdateProjectForm.builder()
                            .id(String.valueOf(request.getProjectId()))
                            .name(request.getName())
                            .displayName(request.getDisplayName())
                            .description(request.getDescription())
                            .startDate(request.getStartDate())
                            .endDate(request.getEndDate())
                            .build()
            );
            if (result <= 0) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            CommonResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase());
        } catch (CoreException e) {
            log.error("RpcService modifyProject error CoreException ", e);
            CommonResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService modifyProject error Exception ", e);
            CommonResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase());
        }
    }

    private void DescribeProjectsResponse(
            StreamObserver<ProjectProto.DescribeProjectsResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Project> projects) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectProto.DescribeProjectsResponse.Builder builder = ProjectProto
                .DescribeProjectsResponse.newBuilder()
                .setResult(result);

        if (CollectionUtils.isNotEmpty(projects)) {
            builder.addAllProjectList(protoConvertUtils.describeProjectsToProtoList(projects));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void DescribeProjectResponse(
            StreamObserver<ProjectProto.DescribeOneProjectResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project project) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectProto.DescribeOneProjectResponse.Builder builder = ProjectProto
                .DescribeOneProjectResponse.newBuilder()
                .setResult(result);

        if (Optional.ofNullable(project).isPresent()) {
            builder.setProject(protoConvertUtils.describeProjectToProto(project));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public void createProjectWithTemplateResponse(
            StreamObserver<ProjectProto.CreateProjectWithTemplateResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Integer projectId) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(projectId)
                .setMessage(message)
                .build();
        responseObserver.onNext(ProjectProto.CreateProjectWithTemplateResponse.newBuilder()
                .setResult(result)
                .setProjectId(projectId)
                .build());
        responseObserver.onCompleted();
    }

    private void CommonResponse(
            StreamObserver<ResultProto.CommonResult> responseObserver,
            CodeProto.Code code,
            String message) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        responseObserver.onNext(ResultProto.CommonResult.newBuilder()
                .setResult(result).build());
        responseObserver.onCompleted();
    }
}
