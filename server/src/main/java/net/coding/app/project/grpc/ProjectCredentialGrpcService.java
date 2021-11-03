package net.coding.app.project.grpc;


import net.coding.lib.project.converter.CredentialConverter;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.BaseCredentialForm;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.proto.platform.project.ProjectCredentialProto;
import net.coding.proto.platform.project.ProjectCredentialServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.user.UserProto;

import static java.util.stream.Collectors.toList;

@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectCredentialGrpcService extends ProjectCredentialServiceGrpc.ProjectCredentialServiceImplBase {
    private final ProjectCredentialService credentialService;
    private final UserGrpcClient userGrpcClient;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Override
    public void delete(
            ProjectCredentialProto.DelCredentialRequest request,
            StreamObserver<ProjectCredentialProto.DelCredentialResponse> responseObserver
    ) {
        ProjectCredentialProto.DelCredentialResponse.Builder builder = ProjectCredentialProto.DelCredentialResponse.newBuilder();
        try {
            if (request == null || request.getId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            // 查询凭证
            int credId = request.getId();
            Credential credential = credentialService.getById(credId);
            if (credential == null) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NOT_EXIST);
            }
            credentialService.delete(credId, credential.getProjectId());
            builder.setCode(CodeProto.Code.SUCCESS);
        } catch (Exception e) {
            log.error("RpcService deleteCredential error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void get(
            ProjectCredentialProto.CredentialGetRequest request,
            StreamObserver<ProjectCredentialProto.CredentialResponse> responseObserver
    ) {
        ProjectCredentialProto.CredentialResponse.Builder builder = ProjectCredentialProto.CredentialResponse.newBuilder();
        try {
            if (request == null || StringUtils.isBlank(request.getUserGk()) || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            String userGk = request.getUserGk();
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            UserProto.User user = getUser(userGk);
            if (user == null) {
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            boolean flag = projectMemberService.isMember(user, request.getProjectId());
            if (!flag) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
            }
            List<Credential> credentials = credentialService.list(projectId, user.getId(), request.getId());
            builder.addAllData(toProtobufCredentialList(credentials))
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
        } catch (Exception e) {
            log.error("RpcService getCredential error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    @Override
    public void updateUsernamePassword(
            ProjectCredentialProto.UpdateUsernamePasswordRequest request,
            StreamObserver<ProjectCredentialProto.UpdateUsernamePasswordResponse> responseObserver
    ) {
        ProjectCredentialProto.UpdateUsernamePasswordResponse.Builder builder = ProjectCredentialProto.UpdateUsernamePasswordResponse.newBuilder();
        try {

            Credential credential = credentialService.getById(request.getId());
            if (credential == null) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NOT_EXIST);
            }
            credential.setUsername(request.getUsername());
            credential.setPassword(request.getPassword());
            int id = credentialService.updateUsernamePassword(credential);
            if (id > 0) {
                builder.setCode(CodeProto.Code.SUCCESS);
            }
        } catch (Exception e) {
            log.error("RpcService updateUsernamePassword error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getById(ProjectCredentialProto.GetByIdRequest request, StreamObserver<ProjectCredentialProto.GetCredentialResponse> responseObserver) {
        ProjectCredentialProto.GetCredentialResponse.Builder builder = ProjectCredentialProto.GetCredentialResponse.newBuilder();
        try {
            Credential credential = credentialService.get(request.getId(), request.getDecrypt());
            if (credential == null) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NOT_EXIST);
            } else {
                ProjectCredentialProto.Credential protobufCredential = toProtobufCredential(credential);
                builder.setCode(CodeProto.Code.SUCCESS)
                        .setData(protobufCredential)
                        .build();
            }
        } catch (Exception e) {
            log.error("RpcService getById error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getByIdList(ProjectCredentialProto.GetByIdListRequest request, StreamObserver<ProjectCredentialProto.CredentialResponse> responseObserver) {
        ProjectCredentialProto.CredentialResponse.Builder builder = ProjectCredentialProto.CredentialResponse.newBuilder();
        try {
            if (request == null || CollectionUtils.isEmpty(request.getIdsList())) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            List<Credential> credentials = credentialService.listByIds(request.getIdsList(), request.getDecrypt());
            Collection<ProjectCredentialProto.Credential> protobufCredentialList = toProtobufCredentialList(credentials);

            builder.setCode(CodeProto.Code.SUCCESS)
                    .addAllData(protobufCredentialList)
                    .build();
        } catch (Exception e) {
            log.error("RpcService getByIdList error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void createCredential(ProjectCredentialProto.CreateCredentialRequest request, StreamObserver<ProjectCredentialProto.CreateCredentialResponse> responseObserver) {
        ProjectCredentialProto.CreateCredentialResponse.Builder builder = ProjectCredentialProto.CreateCredentialResponse.newBuilder();
        try {
            if (request == null || request.getForm().getTeamId() <= 0
                    || request.getForm().getProjectId() <= 0 || StringUtils.isBlank(request.getUserGk())) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }

            credentialService.validParam(request.getForm().getTeamId(),
                    request.getForm().getProjectId(),
                    request.getUserGk());
            BaseCredentialForm form = CredentialConverter.builderCredentialForm(request.getForm());
            int result = credentialService.createCredential(form, request.getEncrypt());
            if (result > 0) {
                builder.setCode(CodeProto.Code.SUCCESS);
            }
        } catch (Exception e) {
            log.error("RpcService getByIdList error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getByCredential(
            ProjectCredentialProto.GetByCredentialRequest request,
            StreamObserver<ProjectCredentialProto.GetByCredentialResponse> responseObserver
    ) {
        ProjectCredentialProto.GetByCredentialResponse.Builder builder = ProjectCredentialProto.GetByCredentialResponse.newBuilder();
        try {
            if (request == null || StringUtils.isEmpty(request.getCredentialId())) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            Credential credential = credentialService.getByCredential(request.getCredentialId(), request.getDecrypt());
            if (credential == null) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                builder.setCredential(toBuildCredential(credential));
                builder.setCode(CodeProto.Code.SUCCESS);
            }
        } catch (Exception e) {
            log.error("RpcService getByCredential error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listByProjectAndUser(
            ProjectCredentialProto.ListByProjectAndUserRequest request,
            StreamObserver<ProjectCredentialProto.ListByProjectAndUserResponse> responseObserver
    ) {
        ProjectCredentialProto.ListByProjectAndUserResponse.Builder builder = ProjectCredentialProto.ListByProjectAndUserResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0 || request.getUserId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            UserProto.User user = userGrpcClient.getUserById(request.getUserId());
            if (user == null) {
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }

            List<Credential> credential = credentialService.listByProjectAndUser(
                    request.getProjectId(),
                    request.getUserId(),
                    request.getAllSelect()
            );
            builder.addAllCredential(toProtobufCredentialList(credential));
            builder.setCode(CodeProto.Code.SUCCESS);
        } catch (Exception e) {
            log.error("RpcService listByProjectAndUser error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listByProjectAndGenerateBy(
            ProjectCredentialProto.ListByProjectAndGenerateByRequest request,
            StreamObserver<ProjectCredentialProto.ListByProjectAndGenerateByResponse> responseObserver
    ) {
        ProjectCredentialProto.ListByProjectAndGenerateByResponse.Builder builder = ProjectCredentialProto.ListByProjectAndGenerateByResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0
                    || ConnGenerateByEnums.valueOf(request.getGeneratedBy().name()) == null) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            List<Credential> credential = credentialService.getByProjectIdAndGenerateBy(
                    request.getProjectId(),
                    request.getGeneratedBy().name()
            );
            builder.addAllCredential(toProtobufCredentialList(credential));
            builder.setCode(CodeProto.Code.SUCCESS);
        } catch (Exception e) {
            log.error("RpcService listByProjectAndGenerateBy error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    private ProjectCredentialProto.Credential toProtobufCredential(Credential credential) {
        ProjectCredentialProto.Credential result = ProjectCredentialProto.Credential.newBuilder().build();
        if (null == credential) {
            return result;
        }
        result = Optional.ofNullable(toBuildCredential(credential)).orElse(result);
        String userGk = Optional.of(credential)
                .map(Credential::getCreatorId)
                .map(this::getUserById)
                .map(UserProto.User::getGlobalKey)
                .orElse("");
        return result.toBuilder().setUserGk(userGk).build();
    }

    private List<ProjectCredentialProto.Credential> toProtobufCredentialList(List<Credential> credentials) {
        if (CollectionUtils.isEmpty(credentials)) {
            return new ArrayList<>();
        }
        Set<Integer> creatorIds = credentials.stream()
                .filter(Objects::nonNull)
                .map(Credential::getCreatorId)
                .collect(Collectors.toSet());
        Map<Integer, String> userMap = Optional.ofNullable(
                        userGrpcClient.findUserByIds(new ArrayList<>(creatorIds)))
                .filter(CollectionUtils::isNotEmpty)
                .map(users -> users.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(UserProto.User::getId, UserProto.User::getGlobalKey, (pre, next) -> pre))
                )
                .orElse(new HashMap<>());
        return Optional.of(credentials.stream()
                        .map(this::toBuildCredential).collect(toList()))
                .filter(CollectionUtils::isNotEmpty)
                .map(vs -> vs.stream()
                        .filter(Objects::nonNull)
                        .map(c -> c.toBuilder()
                                .setUserGk(userMap.getOrDefault(c.getCreatorId(), ""))
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .orElse(new ArrayList<>());
    }

    private ProjectCredentialProto.Credential toBuildCredential(Credential credential) {
        return CredentialConverter.toBuildCredential(credential);
    }

    private UserProto.User getUser(String userGK) {
        return userGrpcClient.getUserByGlobalKey(userGK);
    }

    private UserProto.User getUserById(Integer userId) {
        return userGrpcClient.getUserById(userId);
    }
}
