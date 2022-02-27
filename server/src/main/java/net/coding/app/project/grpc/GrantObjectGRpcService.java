package net.coding.app.project.grpc;

import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.project.ProjectsService;
import net.coding.platform.ram.proto.grant.object.GrantObjectProto;
import net.coding.platform.ram.proto.grant.object.GrantObjectServiceGrpc;

import org.lognet.springboot.grpc.GRpcService;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static java.util.stream.Collectors.toSet;

/**
 * 根据权限服务提供proto模版返回 project data
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class GrantObjectGRpcService extends GrantObjectServiceGrpc.GrantObjectServiceImplBase {
    private final ProjectService projectService;

    private final ProjectsService projectsService;

    private final ProjectDao projectDao;

    private final ProjectMemberInspectService projectMemberInspectService;

    @Override
    public void findGrantObjectIds(GrantObjectProto.FindGrantObjectIdsRequest request, StreamObserver<GrantObjectProto.FindGrantObjectIdsResponse> responseObserver) {
        Set<String> grantObjectIds = projectDao.getProjects(ProjectQueryParameter.builder()
                .teamId((int) request.getTenantId())
                .build()
        )
                .stream()
                .map(project -> {
                    boolean isExist = StreamEx.of(projectMemberInspectService.getPrincipalUserMembers(project.getId()))
                            .anyMatch(member -> member.getUserId().equals((int) request.getUserId()));
                    if (isExist) {
                        return String.valueOf(project.getId());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(toSet());

        responseObserver.onNext(GrantObjectProto.FindGrantObjectIdsResponse
                .newBuilder()
                .addAllGrantObjectIds(grantObjectIds)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void findUserIds(GrantObjectProto.GrantObjectIdsRequest request, StreamObserver<GrantObjectProto.FindUserIdsResponse> responseObserver) {
        Map<String, GrantObjectProto.UserIds> userIdsMap = StreamEx.of(request.getGrantObjectIdsList())
                .map(projectId -> projectService.getById(Integer.valueOf(projectId))
                )
                .nonNull()
                .collect(Collectors.toMap(
                        project -> String.valueOf(project.getId()),
                        project -> GrantObjectProto.UserIds.newBuilder()
                                .addAllUserId(
                                        projectMemberInspectService.getPrincipalUserMembers(project.getId())
                                                .stream()
                                                .map(ProjectMember::getUserId)
                                                .map(Integer::longValue)
                                                .collect(toSet())
                                ).build()
                ));
        responseObserver.onNext(GrantObjectProto.FindUserIdsResponse
                .newBuilder()
                .putAllUserIds(userIdsMap)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getGrantObjectNames(
            GrantObjectProto.GrantObjectIdsRequest request,
            StreamObserver<GrantObjectProto.GetGrantObjectNamesResponse> responseObserver) {
        Map<String, String> grantObjectIdName = projectsService.getByProjectIds(
                StreamEx.of(request.getGrantObjectIdsList())
                        .map(Integer::valueOf)
                        .toSet()
        )
                .stream()
                .collect(Collectors.toMap(project -> String.valueOf(project.getId()), Project::getDisplayName));
        responseObserver.onNext(GrantObjectProto.GetGrantObjectNamesResponse
                .newBuilder()
                .putAllGrantObjectIdName(grantObjectIdName)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchGrantObject(GrantObjectProto.SearchGrantObjectRequest request,
                                  StreamObserver<GrantObjectProto.SearchGrantObjectResponse> responseObserver) {
        Set<String> projectIds = projectsService.getProjects(ProjectQueryParameter.builder()
                .teamId((int) request.getTenantId())
                .keyword(request.getKeyWord())
                .build()
        )
                .stream()
                .map(Project::getId)
                .map(String::valueOf)
                .collect(toSet());
        responseObserver.onNext(GrantObjectProto.SearchGrantObjectResponse
                .newBuilder()
                .addAllGrantObjectId(projectIds)
                .build());
        responseObserver.onCompleted();
    }
}
