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
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.proto.open.api.project.credential.ProjectCredentialProto;
import net.coding.proto.open.api.project.credential.ProjectCredentialServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.ci.CiJobProto;
import proto.open.api.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static proto.open.api.CodeProto.Code.INTERNAL_ERROR;
import static proto.open.api.CodeProto.Code.INVALID_PARAMETER;
import static proto.open.api.CodeProto.Code.SUCCESS;

@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProjectCredentialGRpcService extends ProjectCredentialServiceGrpc.ProjectCredentialServiceImplBase {
    public static final String CREDENTIAL_TYPE_NOT_SUPPORT = "credential type not support";
    public static final String PRIVATE_KEY_BLANK = "private key is required for SSH";
    private final UserGrpcClient userGrpcClient;
    private final ProjectService projectService;
    private final AclServiceGrpcClient aclServiceGrpcClient;
    private final ProjectCredentialService credentialService;
    private final CiJobGrpcClient ciJobGrpcClient;

    @Override
    public void describeProjectCredentials(
            ProjectCredentialProto.DescribeProjectCredentialsRequest request,
            StreamObserver<ProjectCredentialProto.DescribeProjectCredentialsResponse> responseObserver
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
            log.error("RpcService describeProjectCredentials error {}", e.getMessage());
            if (e instanceof CoreException) {
                describeProjectCredentialsResponse(
                        responseObserver,
                        INTERNAL_ERROR,
                        e.getMessage(),
                        null
                );
                return;
            }
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
            ProjectCredentialProto.CreateProjectCredentialRequest request,
            StreamObserver<ProjectCredentialProto.CreateProjectCredentialResponse> responseObserver
    ) {
        try {
            valid(request.getUser().getId(), request.getProjectId());
            CredentialTypeEnums credentialType = Optional.ofNullable(CredentialTypeEnums
                    .of(request.getCredentialType().name()))
                    .orElse(CredentialTypeEnums.USERNAME_PASSWORD);

            // 支持的类型判断
            if (credentialType != CredentialTypeEnums.USERNAME_PASSWORD
                    && credentialType != CredentialTypeEnums.SSH) {
                createCredentialsResponse(responseObserver,
                        INVALID_PARAMETER,
                        CREDENTIAL_TYPE_NOT_SUPPORT,
                        null
                );
                return;
            }
            // ssh 类型时，private_key 不能为空
            if (credentialType == CredentialTypeEnums.SSH
                    && StringUtils.isBlank(request.getPrivateKey())) {
                createCredentialsResponse(responseObserver,
                        INVALID_PARAMETER,
                        PRIVATE_KEY_BLANK,
                        null
                );
                return;
            }
            CredentialForm form = CredentialForm.builder()
                    .teamId(request.getUser().getTeamId())
                    .projectId(request.getProjectId())
                    .creatorId(request.getUser().getId())
                    .name(request.getName())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .type(credentialType.name())
                    .privateKey(request.getPrivateKey())
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
            createCredentialsResponse(
                    responseObserver,
                    SUCCESS,
                    SUCCESS.name().toLowerCase(),
                    credential
            );
        } catch (CoreException e) {
            createCredentialsResponse(
                    responseObserver,
                    INTERNAL_ERROR,
                    e.getMessage(),
                    null
            );
        } catch (Exception e) {
            log.error("RpcService createProjectCredential error {}", e.getMessage());
            createCredentialsResponse(
                    responseObserver,
                    INTERNAL_ERROR,
                    INTERNAL_ERROR.name().toLowerCase(),
                    null
            );
        }
    }

    private void describeProjectCredentialsResponse(
            StreamObserver<ProjectCredentialProto.DescribeProjectCredentialsResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Credential> credentials) {
        CommonProto.Result result = CommonProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectCredentialProto.DescribeProjectCredentialsResponse.Builder builder = ProjectCredentialProto
                .DescribeProjectCredentialsResponse.newBuilder()
                .setResult(result);

        if (CollectionUtils.isNotEmpty(credentials)) {
            builder.setData(getData(credentials));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    private void createCredentialsResponse(
            StreamObserver<ProjectCredentialProto.CreateProjectCredentialResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Credential credential) {
        CommonProto.Result result = CommonProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectCredentialProto.CreateProjectCredentialResponse.Builder builder = ProjectCredentialProto
                .CreateProjectCredentialResponse.newBuilder()
                .setResult(result);

        if (credential != null) {
            builder.setData(getData(credential));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    public ProjectCredentialProto.DescribeProjectCredentialsResponseData getData(List<Credential> credentials) {
        return ProjectCredentialProto.DescribeProjectCredentialsResponseData.newBuilder()
                .addAllCredentialList(toBuilderCredentials(credentials))
                .build();
    }

    public List<ProjectCredentialProto.Credential> toBuilderCredentials(List<Credential> credentials) {
        return Optional.ofNullable(credentials)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toBuilderCredential)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ProjectCredentialProto.CreateProjectCredentialResponseData getData(Credential credential) {
        return ProjectCredentialProto.CreateProjectCredentialResponseData.newBuilder()
                .setCredential(toBuilderCredential(credential))
                .build();
    }

    public ProjectCredentialProto.Credential toBuilderCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        return ProjectCredentialProto.Credential.newBuilder()
                .setCredentialId(credential.getCredentialId())
                .setName(StringUtils.defaultString(credential.getName()))
                .build();
    }

    private void valid(Integer userId, Integer projectId) throws CoreException {
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
    }
}
