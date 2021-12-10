package net.coding.app.project.grpc;

import net.coding.lib.project.converter.CredentialConverter;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.CredentialTask;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.lib.project.service.credential.ProjectCredentialTaskService;
import net.coding.proto.platform.project.ProjectCredentialProto;
import net.coding.proto.platform.project.ProjectCredentialTaskServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

import static java.util.stream.Collectors.toList;

@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectCredentialTaskGrpcService extends ProjectCredentialTaskServiceGrpc.ProjectCredentialTaskServiceImplBase {
    private final ProjectService projectService;
    private final ProjectCredentialTaskService projectCredentialTaskService;
    private final ProjectCredentialService projectCredentialService;

    @Override
    public void getTaskIdsByCredential(ProjectCredentialProto.GetTaskIdsByCredentialRequest request
            , StreamObserver<ProjectCredentialProto.GetTaskIdsByCredentialResponse> responseObserver) {
        ProjectCredentialProto.GetTaskIdsByCredentialResponse.Builder builder = ProjectCredentialProto.GetTaskIdsByCredentialResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            List<CredentialTask> credentialTasks = projectCredentialTaskService
                    .getTaskIdsByCredentialId(projectId, request.getId());
            builder.addAllTaskId(credentialTasks.stream()
                    .map(CredentialTask::getTaskId)
                    .collect(Collectors.toList()));
            builder.setCode(CodeProto.Code.SUCCESS)
                    .build();
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService getTaskIdsByCredential error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getCredentialsByTaskIdAndGenerateBy(ProjectCredentialProto.GetCredentialsByTaskIdAndGenerateByRequest request
            , StreamObserver<ProjectCredentialProto.GetCredentialsByTaskIdAndGenerateByResponse> responseObserver) {
        ProjectCredentialProto.GetCredentialsByTaskIdAndGenerateByResponse.Builder builder = ProjectCredentialProto.GetCredentialsByTaskIdAndGenerateByResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            List<Credential> credentials = projectCredentialService
                    .getCredentialsByTaskIdAndGenerateBy(
                            projectId,
                            request.getTaskId(),
                            request.getGenerateBy(),
                            request.getDecrypt()
                    );
            builder.addAllCredential(toProtobufCredentialList(credentials))
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService getCredentialsByTaskIdAndGenerateBy error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getCredentialsByTaskIdAndType(
            ProjectCredentialProto.GetCredentialsByTaskIdAndTypeRequest request,
            StreamObserver<ProjectCredentialProto.GetCredentialsByTaskIdAndTypeResponse> responseObserver
    ) {
        ProjectCredentialProto.GetCredentialsByTaskIdAndTypeResponse.Builder builder = ProjectCredentialProto.GetCredentialsByTaskIdAndTypeResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            List<Credential> credentials = projectCredentialService
                    .getCredentialsByTaskIdAndType(
                            projectId,
                            request.getTaskId(),
                            request.getType().name(),
                            request.getDecrypt()
                    );
            builder.addAllCredential(toProtobufCredentialList(credentials))
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService getCredentialsByTaskIdAndType error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getCredentialsByTaskId(ProjectCredentialProto.GetCredentialsByTaskIdRequest request, StreamObserver<ProjectCredentialProto.GetCredentialsByTaskIdResponse> responseObserver) {
        ProjectCredentialProto.GetCredentialsByTaskIdResponse.Builder builder = ProjectCredentialProto.GetCredentialsByTaskIdResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            List<Credential> credentials = projectCredentialService
                    .getCredentialsByTaskId(
                            projectId,
                            request.getTaskId(),
                            request.getDecrypt()
                    );
            builder.addAllCredential(toProtobufCredentialList(credentials))
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService getCredentialsByTaskId error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private List<ProjectCredentialProto.Credential> toProtobufCredentialList(List<Credential> credentials) {
        if (CollectionUtils.isEmpty(credentials)) {
            return new ArrayList<>();
        }
        return Optional.of(credentials.stream()
                .map(CredentialConverter::toBuildCredential).collect(toList()))
                .filter(CollectionUtils::isNotEmpty)
                .orElse(new ArrayList<>());
    }

    @Override
    public void toggleTaskPermission(
            ProjectCredentialProto.ToggleTaskPermissionRequest request,
            StreamObserver<ProjectCredentialProto.ToggleTaskPermissionResponse> responseObserver
    ) {
        ProjectCredentialProto.ToggleTaskPermissionResponse.Builder builder =
                ProjectCredentialProto.ToggleTaskPermissionResponse.newBuilder();
        try {
            if (request == null || request.getProjectId() <= 0) {
                log.error("Parameter is error {}", request);
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            if (!request.getSelected()) {
                return;
            }
            int projectId = request.getProjectId();
            Project project = projectService.getById(projectId);
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            projectCredentialTaskService.toggleTaskPermission(
                    projectId,
                    request.getConnId(),
                    request.getTaskType(),
                    request.getTaskId(),
                    request.getSelected()
            );
            builder.setCode(CodeProto.Code.SUCCESS).build();
        } catch (Exception e) {
            log.error("RpcService ToggleTaskPermission error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
