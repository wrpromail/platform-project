package net.coding.app.project.grpc.openapi;

import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.enums.CredentialScopeEnums;
import net.coding.lib.project.enums.CredentialTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.grpc.client.CiJobGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.credential.ProjectCredentialService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.ci.CiJobProto;
import proto.open.api.CodeProto;
import proto.open.api.ResultProto;
import proto.open.api.credential.CredentialProto;
import proto.open.api.credential.CredentialServiceGrpc;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static proto.open.api.CodeProto.Code.INTERNAL_ERROR;
import static proto.open.api.CodeProto.Code.SUCCESS;

@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiCredentialGrpcService extends CredentialServiceGrpc.CredentialServiceImplBase {
    private final UserGrpcClient userGrpcClient;
    private final ProjectService projectService;
    private final AclServiceGrpcClient aclServiceGrpcClient;
    private final ProjectCredentialService credentialService;
    private final TeamGrpcClient teamGrpcClient;
    private final CiJobGrpcClient ciJobGrpcClient;

    @Override
    public void describeProjectCredentials(
            CredentialProto.DescribeProjectCredentialsRequest request,
            StreamObserver<CredentialProto.DescribeProjectCredentialsResponse> responseObserver
    ) {
        try {
            valid(request.getUser().getId(), request.getProjectId());
            List<Credential> credentials = credentialService.getByProjectIdAndGenerateBy(
                    request.getProjectId(),
                    ConnGenerateByEnums.MANUAL.name()
            );
            describeProjectCredentialsResponse(
                    responseObserver,
                    SUCCESS,
                    SUCCESS.name().toLowerCase(),
                    credentials
            );
        } catch (Exception e) {
            if (e instanceof CoreException) {
                describeProjectCredentialsResponse(
                        responseObserver,
                        INTERNAL_ERROR,
                        e.getMessage(),
                        null
                );
            }
            log.error("RpcService describeProjectCredentials error {}", e.getMessage());
            describeProjectCredentialsResponse(
                    responseObserver,
                    INTERNAL_ERROR,
                    INTERNAL_ERROR.name().toLowerCase(),
                    null
            );
        }

    }


    @Override
    public void createProjectCredential(
            CredentialProto.CreateProjectCredentialRequest request,
            StreamObserver<CredentialProto.CreateProjectCredentialResponse> responseObserver
    ) {
        try {
            Map<String, Integer> map = valid(request.getUser().getId(), request.getProjectId());
            CredentialForm form = CredentialForm.builder()
                    .teamId(MapUtils.getIntValue(map, "teamId"))
                    .projectId(request.getProjectId())
                    .creatorId(MapUtils.getIntValue(map, "userId"))
                    .name(request.getName())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .type(CredentialTypeEnums.USERNAME_PASSWORD.name())
                    .scope(CredentialScopeEnums.PROJECT.getCode())
                    .allSelect(true)
                    .build();

            List<CiJobProto.CiJob> ciJobs = ciJobGrpcClient.listByProject(request.getProjectId());

            List<ConnectionTaskDTO> taskDTOList = ciJobs.stream()
                    .map(job -> ConnectionTaskDTO.builder()
                            .selected(true)
                            .id(job.getId())
                            .type(1)
                            .build()).collect(Collectors.toList());
            form.setTaskDTOS(taskDTOList);
            int credentialId = credentialService.createCredential(form);
            Credential credential = credentialService.get(credentialId, false);
            describeCreateCredentialsResponse(
                    responseObserver,
                    SUCCESS,
                    SUCCESS.name().toLowerCase(),
                    credential
            );
        } catch (Exception e) {
            if (e instanceof CoreException) {
                describeCreateCredentialsResponse(
                        responseObserver,
                        INTERNAL_ERROR,
                        e.getMessage(),
                        null
                );
            }
            log.error("RpcService createProjectCredential error {}", e.getMessage());
            describeCreateCredentialsResponse(
                    responseObserver,
                    INTERNAL_ERROR,
                    INTERNAL_ERROR.name().toLowerCase(),
                    null
            );
        }
    }

    private void describeProjectCredentialsResponse(
            StreamObserver<CredentialProto.DescribeProjectCredentialsResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Credential> credentials) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        CredentialProto.DescribeProjectCredentialsResponse.Builder builder = CredentialProto
                .DescribeProjectCredentialsResponse.newBuilder()
                .setResult(result);

        if (CollectionUtils.isNotEmpty(credentials)) {
            builder.setData(getData(credentials));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    private void describeCreateCredentialsResponse(
            StreamObserver<CredentialProto.CreateProjectCredentialResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Credential credential) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        CredentialProto.CreateProjectCredentialResponse.Builder builder = CredentialProto
                .CreateProjectCredentialResponse.newBuilder()
                .setResult(result);

        if (credential != null) {
            builder.setData(getData(credential));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public CredentialProto.DescribeProjectCredentialsResponseData getData(List<Credential> credentials) {
        return CredentialProto.DescribeProjectCredentialsResponseData.newBuilder()
                .addAllCredentialList(toBuilderCredentials(credentials))
                .build();
    }

    public List<CredentialProto.Credential> toBuilderCredentials(List<Credential> credentials) {
        return Optional.ofNullable(credentials)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toBuilderCredential)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public CredentialProto.CreateProjectCredentialResponseData getData(Credential credential) {
        return CredentialProto.CreateProjectCredentialResponseData.newBuilder()
                .setCredential(toBuilderCredential(credential))
                .build();
    }

    public CredentialProto.Credential toBuilderCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        return CredentialProto.Credential.newBuilder()
                .setCredentialId(credential.getCredentialId())
                .setName(StringUtils.defaultString(credential.getName()))
                .build();
    }

    private Map<String, Integer> valid(Integer userId, Integer projectId) throws CoreException {
        UserProto.User currentUser = userGrpcClient.getUserById(userId);
        if (currentUser == null) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
        }
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        boolean hasPermissionInProject;
        try {
            hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectServiceConn)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    projectId,
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
        } catch (Exception e) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        if (!hasPermissionInProject) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(project.getTeamOwnerId());
        if (response == null
                || !response.getCode().equals(proto.common.CodeProto.Code.SUCCESS)
                || ObjectUtils.isEmpty(response.getData())
        ) {
            throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("userId", currentUser.getId());
        map.put("teamId", response.getData().getId());
        return map;
    }
}
