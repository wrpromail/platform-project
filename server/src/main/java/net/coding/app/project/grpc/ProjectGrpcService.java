package net.coding.app.project.grpc;


import net.coding.common.util.BeanUtils;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.service.ProjectService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

import net.coding.proto.platform.project.ProjectProto;
import net.coding.proto.platform.project.ProjectServiceGrpc;

import proto.platform.team.TeamProto;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectGrpcService extends ProjectServiceGrpc.ProjectServiceImplBase {

    private final ProjectService projectService;
    private final TeamGrpcClient teamGrpcClient;

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
            if (CodeProto.Code.SUCCESS != response.getCode()) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
                builder.setMessage("Team is not found");
                log.warn("Team is not found ,teamId : {}", request.getTeamId());
            } else {
                List<Project> projects = projectService.getContainArchivedProjects(request.getTeamId());
                if (CollectionUtils.isNotEmpty(projects)) {
                    builder.addAllProject(projects.stream()
                            .map(project -> toProtoProject(project, response.getData()))
                            .collect(Collectors.toList()));
                    builder.setCode(CodeProto.Code.SUCCESS);
                } else {
                    builder.setCode(CodeProto.Code.NOT_FOUND);
                    builder.setMessage("Project is not found");
                    log.warn("ContainArchivedProjectsGet is not found ,teamId : {}", request.getTeamId());
                }
            }
        } catch (Exception e) {
            log.error("RpcService containArchivedProjectsGet error {} ", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }

        responseObserver.onNext(builder.build());
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
