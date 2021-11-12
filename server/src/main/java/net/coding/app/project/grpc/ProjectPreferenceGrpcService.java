package net.coding.app.project.grpc;


import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.enums.ProjectPreferenceEnum;
import net.coding.lib.project.enums.ProjectPreferenceStatusEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectPreferenceService;
import net.coding.lib.project.service.ProjectService;
import net.coding.proto.platform.project.ProjectPreferenceProto;
import net.coding.proto.platform.project.ProjectPreferenceServiceGrpc;

import org.apache.commons.lang3.math.NumberUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectPreferenceGrpcService extends ProjectPreferenceServiceGrpc.ProjectPreferenceServiceImplBase {

    private final ProjectService projectService;
    private final ProjectPreferenceService projectPreferenceService;


    @Override
    public void get(
            ProjectPreferenceProto.ProjectPreferenceGetRequest request,
            StreamObserver<ProjectPreferenceProto.ProjectPreferenceGetResponse> responseObserver) {
        ProjectPreferenceProto.ProjectPreferenceGetResponse.Builder builder = ProjectPreferenceProto.ProjectPreferenceGetResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectPreferenceEnum projectPreferenceEnum = ProjectPreferenceEnum.of(request.getName());
            if (ObjectUtils.isEmpty(projectPreferenceEnum)) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            ProjectPreference projectPreference = projectPreferenceService.getByProjectIdAndType(request.getProjectId(), Short.parseShort(request.getName()));
            if (projectPreference != null) {
                builder.setCode(CodeProto.Code.SUCCESS)
                        .setProjectPreference(toProjectPreference(projectPreference));
            } else {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            }
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService ProjectPreferenceGet error CoreException {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void set(
            ProjectPreferenceProto.ProjectPreferenceSetRequest request,
            StreamObserver<ProjectPreferenceProto.ProjectPreferenceSetResponse> responseObserver) {
        ProjectPreferenceProto.ProjectPreferenceSetResponse.Builder newBuilder = ProjectPreferenceProto.ProjectPreferenceSetResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectPreferenceEnum projectPreferenceEnum = ProjectPreferenceEnum.of(request.getName());
            if (ObjectUtils.isEmpty(projectPreferenceEnum)) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            ProjectPreferenceStatusEnum statusEnum = ProjectPreferenceStatusEnum.of(request.getValue());
            if (ObjectUtils.isEmpty(statusEnum)) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            short type = NumberUtils.toShort(request.getName());
            short value = NumberUtils.toShort(request.getValue());
            boolean result = projectPreferenceService.toggleProjectPreference(request.getProjectId(), type, value);
            if (result) {
                newBuilder.setCode(CodeProto.Code.SUCCESS);
            } else {
                newBuilder.setCode(CodeProto.Code.NOT_FOUND);
            }
        } catch (CoreException e) {
            newBuilder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("RpcService ProjectPreferenceSet error CoreException {}", e.getMessage());
            newBuilder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(newBuilder.build());
            responseObserver.onCompleted();
        }
    }

    private ProjectPreferenceProto.ProjectPreference toProjectPreference(ProjectPreference projectPreference) {
        return ProjectPreferenceProto.ProjectPreference.newBuilder()
                .setProjectId(projectPreference.getProjectId())
                .setName(String.valueOf(projectPreference.getType()))
                .setValue(String.valueOf(projectPreference.getStatus()))
                .build();
    }
}
