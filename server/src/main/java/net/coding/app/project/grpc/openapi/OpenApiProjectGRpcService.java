package net.coding.app.project.grpc.openapi;

import net.coding.app.project.utils.ProtoConvertUtils;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.e.proto.ApiCodeProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.common.GRpcMetadataContextHolder;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.enums.RegisterSourceEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.interceptor.GRpcHeaderServerInterceptor;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.openapi.OpenApiProjectInvisibleService;
import net.coding.lib.project.service.openapi.OpenApiProjectService;
import net.coding.proto.open.api.project.ProjectProto;
import net.coding.proto.open.api.project.ProjectServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.List;
import java.util.Objects;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

/**
 * OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 */
@Slf4j
@GRpcService(interceptors = GRpcHeaderServerInterceptor.class)
@AllArgsConstructor
public class OpenApiProjectGRpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectService projectService;

    private final OpenApiProjectInvisibleService openApiProjectInvisibleService;

    private final ProjectMemberService projectMemberService;

    private final OpenApiProjectService openApiProjectService;

    private final ProtoConvertUtils protoConvertUtils;

    private final TeamGrpcClient teamGrpcClient;

    private final TeamServiceGrpcClient teamServiceGrpcClient;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    @Override
    public void describeCodingProjects(
            ProjectProto.DescribeCodingProjectsRequest request,
            StreamObserver<ProjectProto.DescribeCodingProjectsResponse> responseObserver) {
        ProjectProto.DescribeCodingProjectsResponse.Builder builder =
                ProjectProto.DescribeCodingProjectsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Integer currentUserId = request.getUser().getId();
            Integer currentTeamId = request.getUser().getTeamId();
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInEnterprise(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.EnterpriseProject)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    currentUserId,
                    currentTeamId
            );
            //有权限可以查询其他成员所在项目，否则只能查询自己
            if (!hasPermissionInProject) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            LimitedPager pager = new LimitedPager(request.getPageNumber(), request.getPageSize());
            ResultPage<Project> resultPage = projectService.getProjects(
                    ProjectQueryParameter.builder()
                            .teamId(currentTeamId)
                            .keyword(request.getProjectName())
                            .build(),
                    pager);
            builder.setResult(
                            resultBuilder
                                    .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                                    .build()
                    )
                    .setData(protoConvertUtils.describeProjectPagesToProto(resultPage));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeCodingProjects error Exception ", e);
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

    /**
     * 查询项目列表 根据label查询
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void describeProjectLabels(
            ProjectProto.DescribeProjectLabelsRequest request,
            StreamObserver<ProjectProto.DescribeProjectsResponse> responseObserver) {
        ProjectProto.DescribeProjectsResponse.Builder builder =
                ProjectProto.DescribeProjectsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
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
            List<Project> projects = openApiProjectInvisibleService.getJoinedProjectsByLabel(
                    request.getUser().getTeamId(),
                    userId,
                    request.getLabel()
            );
            builder.setResult(
                            resultBuilder
                                    .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                                    .build()
                    )
                    .addAllProjectList(protoConvertUtils.describeProjectsToProtoList(projects));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeProjectLabels error Exception ", e);
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

    @Override
    public void describeUserProjects(
            ProjectProto.DescribeUserProjectsRequest request,
            StreamObserver<ProjectProto.DescribeProjectsResponse> responseObserver) {
        ProjectProto.DescribeProjectsResponse.Builder builder =
                ProjectProto.DescribeProjectsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Integer currentUserId = request.getUser().getId();
            Integer currentTeamId = request.getUser().getTeamId();
            boolean isMember = teamServiceGrpcClient.isMember(
                    currentTeamId,
                    request.getUserId());
            if (!isMember) {
                throw CoreException.of(CoreException.ExceptionType.TEAM_MEMBER_NOT_EXISTS);
            }
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInEnterprise(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.EnterpriseProject)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    currentUserId,
                    currentTeamId
            );
            //有权限可以查询其他成员所在项目，否则只能查询自己
            if (!hasPermissionInProject && currentUserId != request.getUserId()) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            List<Project> projects = projectService.getUserProjects(
                    ProjectQueryParameter.builder()
                            .userId(request.getUserId())
                            .teamId(currentTeamId)
                            .keyword(request.getProjectName())
                            .invisible(0)
                            .build()
            );
            builder.setResult(
                            resultBuilder
                                    .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                                    .build()
                    )
                    .addAllProjectList(protoConvertUtils.describeProjectsToProtoList(projects));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeUserProjects error Exception ", e);
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
                    .setProject(protoConvertUtils.describeProjectToProto(project));
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

    @Override
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
                    .setProject(protoConvertUtils.describeProjectToProto(project));
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

    @Override
    public void createProjectWithTemplate(
            ProjectProto.CreateProjectRequest request,
            StreamObserver<ProjectProto.CreateProjectResponse> responseObserver) {
        ProjectProto.CreateProjectResponse.Builder builder =
                ProjectProto.CreateProjectResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Integer projectId = openApiProjectInvisibleService.createProject(request);
            builder.setResult(
                            resultBuilder
                                    .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                                    .build()
                    )
                    .setProjectId(projectId);
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(e.getMsg())
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService createProjectWithTemplate error Exception ", e);
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

    @Override
    public void createCodingProject(
            ProjectProto.CreateProjectRequest request,
            StreamObserver<ProjectProto.CreateProjectResponse> responseObserver) {
        ProjectProto.CreateProjectResponse.Builder builder =
                ProjectProto.CreateProjectResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Integer projectId = openApiProjectService.createProject(request,
                    RegisterSourceEnum.OPEN_API.name());
            builder.setResult(
                            resultBuilder
                                    .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                                    .build()
                    )
                    .setProjectId(projectId);
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(e.getMsg())
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService createCodingProject error Exception ", e);
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

    @Override
    public void deleteOneProject(
            ProjectProto.DescribeOneProjectRequest request,
            StreamObserver<CommonProto.CommonResult> responseObserver) {
        CommonProto.CommonResult.Builder builder =
                CommonProto.CommonResult.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
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
                throw CoreException.of(PERMISSION_DENIED);
            }
            projectService.delete(currentUser.getId(), currentUser.getTeamId(), request.getProjectId());
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            );
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService deleteOneProject error Exception ", e);
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

    @Override
    public void modifyProject(ProjectProto.ModifyProjectRequest request,
                              StreamObserver<CommonProto.CommonResult> responseObserver) {
        CommonProto.CommonResult.Builder builder =
                CommonProto.CommonResult.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
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
                throw CoreException.of(PERMISSION_DENIED);
            }

            projectService.update(
                    currentUser.getTeamId(),
                    currentUser.getId(),
                    UpdateProjectForm.builder()
                            .id(String.valueOf(request.getProjectId()))
                            .name(request.getName())
                            .displayName(request.getDisplayName())
                            .description(request.getDescription())
                            .startDate(request.getStartDate())
                            .endDate(request.getEndDate())
                            .build()
            );
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            );
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService modifyProject error Exception ", e);
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
