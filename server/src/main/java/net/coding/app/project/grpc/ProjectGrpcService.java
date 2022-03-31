package net.coding.app.project.grpc;


import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectTokenService;
import net.coding.lib.project.service.ProjectValidateService;
import net.coding.lib.project.service.project.ProjectsService;
import net.coding.lib.project.template.ProjectTemplateType;
import net.coding.proto.platform.project.ProjectProto;
import net.coding.proto.platform.project.ProjectServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.common.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_LOGIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static proto.common.CodeProto.Code.INTERNAL_ERROR;
import static proto.common.CodeProto.Code.NOT_FOUND;
import static proto.common.CodeProto.Code.SUCCESS;
import static proto.platform.permission.PermissionProto.Action.Create;
import static proto.platform.permission.PermissionProto.Action.Delete;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProject;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectGrpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectDao projectDao;
    private final ProjectService projectService;
    private final ProjectsService projectsService;
    private final ProjectValidateService projectValidateService;
    private final TeamGrpcClient teamGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final AclServiceGrpcClient aclServiceGrpcClient;
    private final ProjectTokenService projectTokenService;

    @Override
    public void containArchivedProjectsGet(
            ProjectProto.ContainArchivedProjectsGetRequest request,
            StreamObserver<ProjectProto.ContainArchivedProjectsGetResponse> responseObserver) {
        ProjectProto.ContainArchivedProjectsGetResponse.Builder builder =
                ProjectProto.ContainArchivedProjectsGetResponse.newBuilder();
        try {
            if (request.getTeamId() <= 0) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(request.getTeamId());
            if (SUCCESS != response.getCode()) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
                builder.setMessage("Team is not found");
                log.warn("Team is not found ,teamId : {}", request.getTeamId());
            } else {
                List<Project> projects = projectService.getContainArchivedProjects(request.getTeamId());
                if (CollectionUtils.isNotEmpty(projects)) {
                    builder.addAllProject(projects.stream()
                            .map(this::toProtoProject)
                            .collect(Collectors.toList()));
                    builder.setCode(SUCCESS);
                } else {
                    builder.setCode(CodeProto.Code.NOT_FOUND);
                    builder.setMessage("Project is not found");
                    log.warn("ContainArchivedProjectsGet is not found ,teamId : {}", request.getTeamId());
                }
            }
        } catch (Exception e) {
            log.error("RpcService containArchivedProjectsGet error {} ", e.getMessage());
            builder.setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void createProject(
            ProjectProto.CreateProjectRequest request,
            StreamObserver<ProjectProto.CreateProjectResponse> responseObserver) {
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUserId());
            if (Objects.isNull(currentUser)) {
                throw CoreException.of(USER_NOT_LOGIN);
            }
            //验证用户接口权限
            boolean hasPermissionInEnterprise = aclServiceGrpcClient.hasPermissionInEnterprise(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(EnterpriseProject)
                            .setAction(Create)
                            .build(),
                    currentUser.getId(),
                    currentUser.getTeamId()
            );
            if (!hasPermissionInEnterprise) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            Project project = projectService.createProject(ProjectCreateParameter.builder()
                    .userId(currentUser.getId())
                    .teamId(currentUser.getTeamId())
                    .name(request.getName().replace(" ", "-"))
                    .displayName(request.getDisplayName())
                    .description(request.getDescription())
                    .icon(EMPTY)
                    .groupId(null)
                    .projectTemplate(request.getProjectTemplate().name())
                    .template(EMPTY)
                    .functionModules(new HashSet<>())
                    .build());
            createProjectResponse(responseObserver, SUCCESS, SUCCESS.name(), project);
        } catch (CoreException e) {
            createProjectResponse(responseObserver, NOT_FOUND, e.getMsg(), null);
        } catch (Exception e) {
            log.error("rpcService createProject error Exception ", e);
            createProjectResponse(responseObserver, INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @Override
    public void deleteProject(
            ProjectProto.DeleteProjectRequest request,
            StreamObserver<ProjectProto.DeleteProjectResponse> responseObserver) {
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUserId());
            if (Objects.isNull(currentUser)) {
                throw CoreException.of(USER_NOT_LOGIN);
            }
            //验证用户接口权限
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(EnterpriseProject)
                            .setAction(Delete)
                            .build(),
                    request.getProjectId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            projectService.delete(currentUser.getId(), currentUser.getTeamId(), request.getProjectId());
            deleteProjectResponse(responseObserver, SUCCESS, SUCCESS.name());
        } catch (CoreException e) {
            deleteProjectResponse(responseObserver, NOT_FOUND, e.getMsg());
        } catch (Exception e) {
            log.error("rpcService deleteProject error Exception ", e);
            deleteProjectResponse(responseObserver, INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void existProjectByName(
            ProjectProto.ExistProjectByNameRequest request,
            StreamObserver<ProjectProto.ExistProjectByNameResponse> responseObserver) {
        try {
            TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(request.getTeamId());
            if (Objects.isNull(response) || CodeProto.Code.SUCCESS != response.getCode()
                    || ObjectUtils.isEmpty(response.getData())) {
                throw CoreException.of(TEAM_NOT_EXIST);
            }
            projectValidateService.validateCreateProjectParameter(ProjectCreateParameter.builder()
                    .teamId(response.getData().getId())
                    .name(request.getProjectName().replace(" ", "-"))
                    .displayName(request.getProjectName())
                    .projectTemplate(ProjectTemplateType.DEV_OPS.name())
                    .description(EMPTY)
                    .build());
            existProjectByNameResponse(responseObserver, SUCCESS, SUCCESS.name(), false);
        } catch (CoreException e) {
            existProjectByNameResponse(responseObserver, SUCCESS, e.getMsg(), true);
        } catch (Exception e) {
            log.error("rpcService existProjectByName error Exception ", e);
            existProjectByNameResponse(responseObserver, INTERNAL_ERROR, e.getMessage(), false);
        }
    }

    @Override
    public void getWithArchivedProject(
            ProjectProto.GetWithArchivedProjectRequest request,
            StreamObserver<ProjectProto.GetWithArchivedProjectResponse> responseObserver) {
        try {
            Project project;
            if (request.getTeamId() > 0) {
                project = projectService.getWithArchivedByIdAndTeamId(request.getProjectId(), request.getTeamId());
            } else {
                project = projectDao.getProjectNotDeleteById(request.getProjectId());
            }
            getWithArchivedProjectResponse(responseObserver, SUCCESS, SUCCESS.name(), project);
        } catch (Exception e) {
            log.error("rpcService getWithArchivedProject error Exception ", e);
            getWithArchivedProjectResponse(responseObserver, INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @Override
    public void getJoinedProjects(
            ProjectProto.GetJoinedProjectsRequest request,
            StreamObserver<ProjectProto.GetJoinedProjectsResponse> responseObserver) {
        try {
            List<Project> projects = projectsService.getJoinedProjects(request.getTeamId(),
                    request.getUserId(),
                    request.getKeyword());
            getJoinedProjectsResponse(responseObserver, SUCCESS, SUCCESS.name(), projects);
        } catch (Exception e) {
            log.error("rpcService getJoinedProjects error Exception ", e);
            getJoinedProjectsResponse(responseObserver, INTERNAL_ERROR, e.getMessage(), null);
        }
    }

    @Override
    public void getUserProjects(ProjectProto.GetJoinedProjectsRequest request,
                                StreamObserver<ProjectProto.GetProjectsResponse> responseObserver) {
        ProjectProto.GetProjectsResponse.Builder builder = ProjectProto.GetProjectsResponse.newBuilder();
        try {
            List<Project> projects = projectsService.getJoinedPrincipalProjects(request.getTeamId(),
                    request.getUserId(),
                    request.getKeyword());
            builder.setCode(SUCCESS).addAllData(toProtoProjects(projects));
        } catch (Exception e) {
            log.error("rpcService getUserProjects error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void isProjectRobotUser(ProjectProto.IsProjectRobotUserRequest request,
                                   StreamObserver<ProjectProto.IsProjectRobotUserResponse> responseObserver) {
        ProjectProto.IsProjectRobotUserResponse.Builder builder = ProjectProto.IsProjectRobotUserResponse.newBuilder();
        try {
            builder.setCode(SUCCESS)
                    .setResult(projectTokenService.isProjectRobotUser(request.getUserGK()));
        } catch (Exception e) {
            log.error("rpcService isProjectRobotUser error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectById(ProjectProto.GetProjectByIdRequest request,
                               StreamObserver<ProjectProto.GetProjectResponse> responseObserver) {
        ProjectProto.GetProjectResponse.Builder builder = ProjectProto.GetProjectResponse.newBuilder();
        try {
            Project project = Optional.of(request)
                    .filter(req -> request.getWithDeleted() == Boolean.TRUE)
                    .map(req -> projectService.getProjectWithDeleted(req.getProjectId()))
                    .orElse(projectService.getById(request.getProjectId()));
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            builder.setCode(SUCCESS).setData(toProtoProject(project));
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.NOT_FOUND).setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("rpcService getProjectById error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectByTeamIdAndName(ProjectProto.GetProjectByTeamIdAndNameRequest request,
                                          StreamObserver<ProjectProto.GetProjectResponse> responseObserver) {
        ProjectProto.GetProjectResponse.Builder builder = ProjectProto.GetProjectResponse.newBuilder();
        try {
            Project project = projectService.getByNameAndTeamId(request.getProjectName(), request.getTeamId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            builder.setCode(SUCCESS).setData(toProtoProject(project));
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.NOT_FOUND).setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("rpcService getProjectByTeamIdAndName error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectByTeamIdAndDisplayName(ProjectProto.GetProjectByTeamIdAndDisplayNameRequest request,
                                                 StreamObserver<ProjectProto.GetProjectResponse> responseObserver) {
        ProjectProto.GetProjectResponse.Builder builder = ProjectProto.GetProjectResponse.newBuilder();
        try {
            Project project = projectService.getByDisplayNameAndTeamId(request.getDisplayName(), request.getTeamId());
            if (Objects.isNull(project)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
            builder.setCode(SUCCESS).setData(toProtoProject(project));
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.NOT_FOUND).setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("rpcService getProjectByTeamIdAndDisplayName error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectsByIds(ProjectProto.GetProjectByIdsRequest request,
                                 StreamObserver<ProjectProto.GetProjectsResponse> responseObserver) {
        ProjectProto.GetProjectsResponse.Builder builder = ProjectProto.GetProjectsResponse.newBuilder();
        try {
            List<Project> projects = Optional.of(request)
                    .filter(req -> request.getWithDeleted() == Boolean.TRUE)
                    .map(req -> projectsService.getByProjectIdsWithDeleted(StreamEx.of(request.getProjectIdsList()).toSet()))
                    .orElse(projectsService.getByProjectIds(StreamEx.of(request.getProjectIdsList()).toSet()));
            builder.setCode(SUCCESS).addAllData(toProtoProjects(projects));
        } catch (Exception e) {
            log.error("rpcService getProjectsByIds error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectsByTeamId(ProjectProto.GetProjectsByTeamIdRequest request,
                                    StreamObserver<ProjectProto.GetProjectsResponse> responseObserver) {
        ProjectProto.GetProjectsResponse.Builder builder = ProjectProto.GetProjectsResponse.newBuilder();
        try {
            List<Project> projects = Optional.of(request)
                    .filter(req -> request.getWithDeleted() == Boolean.TRUE)
                    .map(req -> projectsService.getProjectsWithDeleted(ProjectQueryParameter.builder()
                            .teamId(request.getTeamId())
                            .keyword(request.getKeyword())
                            .build()))
                    .orElse(projectsService.getProjects(ProjectQueryParameter.builder()
                            .teamId(request.getTeamId())
                            .keyword(request.getKeyword())
                            .build()));
            builder.setCode(SUCCESS).addAllData(toProtoProjects(projects));
        } catch (Exception e) {
            log.error("rpcService getProjectsByTeamId error Exception ", e);
            builder.setCode(INTERNAL_ERROR).setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    public void createProjectResponse(
            StreamObserver<ProjectProto.CreateProjectResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project project) {
        ProjectProto.CreateProjectResponse.Builder builder = ProjectProto.CreateProjectResponse.newBuilder()
                .setCode(code)
                .setMessage(message);
        if (Objects.nonNull(project)) {
            builder.setProject(toProtoProject(project));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public void deleteProjectResponse(
            StreamObserver<ProjectProto.DeleteProjectResponse> responseObserver,
            CodeProto.Code code,
            String message) {
        responseObserver.onNext(ProjectProto.DeleteProjectResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build());
        responseObserver.onCompleted();
    }

    public void existProjectByNameResponse(
            StreamObserver<ProjectProto.ExistProjectByNameResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Boolean isExist) {
        responseObserver.onNext(ProjectProto.ExistProjectByNameResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .setIsExist(isExist)
                .build());
        responseObserver.onCompleted();
    }

    public void getWithArchivedProjectResponse(
            StreamObserver<ProjectProto.GetWithArchivedProjectResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project project) {
        ProjectProto.GetWithArchivedProjectResponse.Builder builder = ProjectProto.GetWithArchivedProjectResponse.newBuilder()
                .setCode(code)
                .setMessage(message);
        if (Objects.nonNull(project)) {
            builder.setProject(toProtoProject(project));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public void getJoinedProjectsResponse(
            StreamObserver<ProjectProto.GetJoinedProjectsResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Project> projects) {
        ProjectProto.GetJoinedProjectsResponse.Builder builder = ProjectProto.GetJoinedProjectsResponse.newBuilder()
                .setCode(code)
                .setMessage(message);
        if (CollectionUtils.isNotEmpty(projects)) {
            builder.addAllProjects(toProtoProjects(projects));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private List<ProjectProto.Project> toProtoProjects(List<Project> projects) {
        return StreamEx.of(projects)
                .nonNull()
                .map(this::toProtoProject)
                .nonNull()
                .collect(Collectors.toList());
    }

    private ProjectProto.Project toProtoProject(Project project) {
        String htmlUrl = getHtmlUrl(project);
        String projectPath = getProjectPath(project);
        return ProjectProto.Project.newBuilder()
                .setId(project.getId())
                .setName(StringUtils.defaultString(project.getName()))
                .setDisplayName(StringUtils.defaultString(project.getDisplayName()))
                .setDescription(StringUtils.defaultString(project.getDescription()))
                .setIcon(StringUtils.defaultString(project.getIcon()))
                .setTeamId(project.getTeamOwnerId())
                .setProjectPath(StringUtils.defaultString(projectPath))
                .setInvisible(project.getInvisible())
                .setHtmlUrl(StringUtils.defaultString(htmlUrl))
                .setLabel(StringUtils.defaultString(project.getLabel()))
                .setIsArchived(project.getDeletedAt().equals(BeanUtils.getDefaultArchivedAt()))
                .setPmType(project.getPmType())
                .setPmTypeName(PmTypeEnums.of(project.getPmType()).name())
                .setCreatedAt(com.google.protobuf.util.Timestamps.fromMillis(project.getCreatedAt().getTime()))
                .build();
    }

    private String getHtmlUrl(Project project) {
        String hostWithProtocol = teamGrpcClient.getTeamHostWithProtocolByTeamId(project.getTeamOwnerId());
        return hostWithProtocol + getProjectPath(project);
    }

    private String getProjectPath(Project project) {
        return "/p/" + project.getName();
    }
}
