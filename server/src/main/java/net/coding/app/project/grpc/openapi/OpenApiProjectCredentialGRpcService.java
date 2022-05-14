package net.coding.app.project.grpc.openapi;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.proto.ApiCodeProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.credential.enums.CredentialGenerated;
import net.coding.lib.project.credential.enums.CredentialScope;
import net.coding.lib.project.credential.enums.CredentialType;
import net.coding.lib.project.credential.service.ProjectCredentialService;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.VerificationMethodEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.grpc.client.CiJobGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
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
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.e.proto.ApiCodeProto.Code.INVALID_PARAMETER;
import static net.coding.e.proto.ApiCodeProto.Code.NOT_FOUND;
import static net.coding.e.proto.ApiCodeProto.Code.SUCCESS;

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
    private final LocaleMessageSource localeMessageSource;

    @Override
    public void describeProjectCredentials(
            ProjectCredentialProto.DescribeProjectCredentialsRequest request,
            StreamObserver<ProjectCredentialProto.DescribeProjectCredentialsResponse> responseObserver
    ) {
        try {
            valid(request.getUser().getId(), request.getProjectId());
            List<Credential> credentials = credentialService.getByProjectIdAndGenerateBy(
                    request.getProjectId(),
                    CredentialGenerated.MANUAL.name()
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
                        NOT_FOUND,
                        e.getMessage(),
                        null
                );
                return;
            }
            describeProjectCredentialsResponse(
                    responseObserver,
                    INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(),
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
            CredentialType credentialType = Optional.ofNullable(CredentialType
                    .of(request.getCredentialType().name()))
                    .orElse(CredentialType.USERNAME_PASSWORD);

            // 支持的类型判断
            if (credentialType != CredentialType.USERNAME_PASSWORD
                    && credentialType != CredentialType.SSH
                    && credentialType != CredentialType.KUBERNETES) {
                createCredentialsResponse(responseObserver,
                        INVALID_PARAMETER,
                        CREDENTIAL_TYPE_NOT_SUPPORT,
                        null
                );
                return;
            }
            // ssh 类型时，private_key 不能为空
            if (credentialType == CredentialType.SSH
                    && StringUtils.isBlank(request.getPrivateKey())) {
                createCredentialsResponse(responseObserver,
                        INVALID_PARAMETER,
                        PRIVATE_KEY_BLANK,
                        null
                );
                return;
            }

            validateParameter(request, credentialType);

            CredentialForm form = CredentialForm.builder()
                    .teamId(request.getUser().getTeamId())
                    .projectId(request.getProjectId())
                    .creatorId(request.getUser().getId())
                    .name(request.getName())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .type(credentialType.name())
                    .privateKey(request.getPrivateKey())
                    .scope(CredentialScope.PROJECT.getCode())
                    .allSelect(true)
                    .acceptUntrustedCertificates(request.getAcceptUntrustedCertificates())
                    .verificationMethod(request.getVerificationMethod())
                    .kubConfig(request.getKubConfig())
                    .clusterName(request.getClusterName())
                    .url(request.getUrl())
                    .secretKey(request.getSecretKey())
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
                    NOT_FOUND,
                    e.getMessage(),
                    null
            );
        } catch (Exception e) {
            log.error("RpcService createProjectCredential error {}", e.getMessage());
            createCredentialsResponse(
                    responseObserver,
                    INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(),
                    null
            );
        }
    }

    @Override
    public void deleteProjectCredential(
            ProjectCredentialProto.DeleteProjectCredentialRequest request,
            StreamObserver<ProjectCredentialProto.DeleteProjectCredentialResponse> responseObserver) {
        ProjectCredentialProto.DeleteProjectCredentialResponse.Builder builder =
                ProjectCredentialProto.DeleteProjectCredentialResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            valid(request.getUser().getId(), request.getProjectId());
            credentialService.delete(request.getCredentialId(), request.getProjectId());
            builder.setResult(resultBuilder.setCode(ApiCodeProto.Code.SUCCESS.getNumber()).build());
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder.setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build());
        } catch (Exception e) {
            log.error("RpcService deleteProjectCredential error Exception ", e);
            builder.setResult(
                    resultBuilder.setCode(ApiCodeProto.Code.INVALID_PARAMETER.getNumber())
                            .setMessage(ApiCodeProto.Code.INVALID_PARAMETER.name().toLowerCase())
                            .build());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private void describeProjectCredentialsResponse(
            StreamObserver<ProjectCredentialProto.DescribeProjectCredentialsResponse> responseObserver,
            ApiCodeProto.Code code,
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
            ApiCodeProto.Code code,
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
        Project project = projectService.getByIdAndTeamId(projectId, currentUser.getTeamId());
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

    private void validateParameter(ProjectCredentialProto.CreateProjectCredentialRequest request,
                                   CredentialType credentialType
    ) throws CoreException {
        if (StringUtils.isBlank(request.getName())) {
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NAME_NOT_EMPTY);
        }
        if (request.getName().length() > 30) {
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NAME_TOO_LONG);
        }
        if (credentialType == CredentialType.KUBERNETES) {
            if (StringUtils.isBlank(request.getVerificationMethod())) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_VERIFICATION_METHOD_NOT_EMPTY);
            }
            if (request.getVerificationMethod().equals(VerificationMethodEnums.Kubeconfig.name())) {
                if (StringUtils.isBlank(request.getKubConfig())) {
                    throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_KUB_CONFIG_NOT_EMPTY);
                }
            }
            if (request.getVerificationMethod().equals(VerificationMethodEnums.ServiceAccount.name())) {
                if (StringUtils.isBlank(request.getUrl())) {
                    throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_URL_NOT_EMPTY);
                }
                if (StringUtils.isBlank(request.getSecretKey())) {
                    throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_SECRET_KEY_NOT_EMPTY);
                }
            }
        }
    }
}
