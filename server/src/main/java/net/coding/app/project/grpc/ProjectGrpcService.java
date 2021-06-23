package net.coding.app.project.grpc;


import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ProjectTemplateEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.service.ProjectService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

import net.coding.proto.platform.project.ProjectProto;
import net.coding.proto.platform.project.ProjectServiceGrpc;

import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_LOGIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static proto.common.CodeProto.Code.INTERNAL_ERROR;
import static proto.common.CodeProto.Code.SUCCESS;
import static proto.common.CodeProto.Code.NOT_FOUND;
import static proto.platform.permission.PermissionProto.Action.Create;
import static proto.platform.permission.PermissionProto.Action.Delete;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProject;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectGrpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectService projectService;
    private final TeamGrpcClient teamGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final AclServiceGrpcClient aclServiceGrpcClient;

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
                    .functionModule(new ArrayList<>())
                    .build());
            createProjectResponse(responseObserver, SUCCESS, SUCCESS.name(), project);
        } catch (CoreException e) {
            log.error("RpcService createProject error CoreException ", e);
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
            log.error("RpcService deleteProject error CoreException ", e);
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
            projectService.validateCreateProjectParameter(ProjectCreateParameter.builder()
                    .teamId(response.getData().getId())
                    .name(request.getProjectName().replace(" ", "-"))
                    .displayName(request.getProjectName())
                    .projectTemplate(ProjectTemplateEnums.DEV_OPS.name())
                    .description(EMPTY)
                    .build());
            existProjectByNameResponse(responseObserver, SUCCESS, SUCCESS.name(), false);
        } catch (CoreException e) {
            log.error("RpcService existProjectByName error CoreException ", e);
            existProjectByNameResponse(responseObserver, SUCCESS, e.getMsg(), true);
        } catch (Exception e) {
            log.error("rpcService existProjectByName error Exception ", e);
            existProjectByNameResponse(responseObserver, INTERNAL_ERROR, e.getMessage(), false);
        }
    }

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
                            .map(project -> toProtoProject(project, response.getData()))
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

    public void createProjectResponse(
            StreamObserver<ProjectProto.CreateProjectResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project project) {
        ProjectProto.CreateProjectResponse.Builder builder = ProjectProto.CreateProjectResponse.newBuilder()
                .setCode(code)
                .setMessage(message);
        if (Objects.nonNull(project)) {
            TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(project.getTeamOwnerId());
            builder.setProject(toProtoProject(project, response.getData()));
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

    private ProjectProto.Project toProtoProject(Project project, TeamProto.Team team) {
        String htmlUrl = getHtmlUrl(team, project);
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
                .build();
    }

    private String getHtmlUrl(TeamProto.Team team, Project project) {
        String hostWithProtocol = teamGrpcClient.getTeamHostWithProtocolByTeamId(team.getId());
        return hostWithProtocol + getProjectPath(project);
    }

    private String getProjectPath(Project project) {
        return "/p/" + project.getName();
    }
}
