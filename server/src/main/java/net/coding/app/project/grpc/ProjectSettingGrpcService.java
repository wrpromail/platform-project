package net.coding.app.project.grpc;


import net.coding.common.util.BeanUtils;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.setting.ProjectSetting;
import net.coding.lib.project.setting.ProjectSettingService;
import net.coding.proto.platform.project.ProjectSettingProto;
import net.coding.proto.platform.project.ProjectSettingServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectSettingGrpcService extends ProjectSettingServiceGrpc.ProjectSettingServiceImplBase {
    private final ProjectService projectService;
    private final ProjectSettingService projectSettingService;


    private void assertProjectNotArchived(final Integer projectId) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null || project.getDeletedAt().equals(BeanUtils.getDefaultArchivedAt())) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST_OR_ARCHIVED);
        }
    }


    @Override
    public void getProjectSettingByCode(
            ProjectSettingProto.ProjectSettingByCodeRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByCodeResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingByCodeResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByCodeResponse.newBuilder()
                        .setCode(CodeProto.Code.NOT_FOUND);
        try {
            assertProjectNotArchived(request.getProjectId());
            ProjectSetting projectSetting = projectSettingService.findByCode(request.getProjectId(), request.getCode());
            builder.setCode(CodeProto.Code.SUCCESS)
                    .setValue(projectSetting.getValue());
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } catch (Exception e) {
            log.error("RpcService getProjectSettingByCode error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectSettingByCodes(
            ProjectSettingProto.ProjectSettingByCodesRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByCodesResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingByCodesResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByCodesResponse.newBuilder();
        try {
            assertProjectNotArchived(request.getProjectId());
            List<ProjectSetting> projectSettings = projectSettingService.findProjectSettings(request.getProjectId(), request.getCodesList());
            builder.addAllData(toProto(projectSettings)).build();
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } catch (Exception e) {
            log.error("RpcService getProjectSettingByCodes error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void projectSettingUpdate(
            ProjectSettingProto.ProjectSettingUpdateRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingUpdateResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingUpdateResponse.Builder builder =
                ProjectSettingProto.ProjectSettingUpdateResponse.newBuilder();
        try {
            assertProjectNotArchived(request.getProjectId());
            ProjectSetting setting = projectSettingService.update(
                    request.getProjectId(),
                    request.getCode(),
                    request.getValue()
            );
            builder.setData(toProto(setting));
        } catch (CoreException e) {
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } catch (Exception e) {
            log.error("RpcService projectSettingUpdate error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void projectSettingBatchUpdate(
            ProjectSettingProto.ProjectSettingBatchUpdateRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingBatchUpdateResponse> responseObserver
    ) {
        log.info("update project settins {}", request.getProjectSettingsList());
        ProjectSettingProto.ProjectSettingBatchUpdateResponse.Builder builder =
                ProjectSettingProto.ProjectSettingBatchUpdateResponse.newBuilder();
        List<ProjectSettingProto.ProjectSettingMessage> projectSettings = request.getProjectSettingsList();
        try {
            builder.setCode(CodeProto.Code.SUCCESS);
            projectSettings.forEach(s -> {
                ProjectSetting setting = projectSettingService.update(
                        s.getProjectId(),
                        s.getCode(),
                        s.getValue()
                );
                if (setting == null) {
                    builder.setCode(CodeProto.Code.INTERNAL_ERROR);
                }
            });
        } catch (Exception e) {
            log.error("RpcService projectSettingBatchUpdate error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectSettingById(
            ProjectSettingProto.ProjectSettingGetByIdRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingGetByIdResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingGetByIdResponse.Builder builder =
                ProjectSettingProto.ProjectSettingGetByIdResponse.newBuilder();
        try {
            ProjectSetting projectSetting = projectSettingService.get(request.getId());
            if (projectSetting == null) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                builder.setData(toProto(projectSetting));
            }
        } catch (Exception e) {
            log.error("RpcService getProjectSettingById error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProjectSettingByProjectIdsAndCode(
            ProjectSettingProto.ProjectSettingByProjectIdsAndCodeRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse.newBuilder();
        try {
            List<ProjectSetting> projectSettings = projectSettingService.findProjectsSetting(
                    request.getProjectIdList(),
                    request.getCode()
            );
            builder.setCode(CodeProto.Code.SUCCESS)
                    .addAllValue(toProto(projectSettings));
        } catch (Exception e) {
            log.error("RpcService getProjectSettingById error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(StringUtils.defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private List<ProjectSettingProto.ProjectSettingMessage> toProto(List<ProjectSetting> settings) {
        if (CollectionUtils.isEmpty(settings)) {
            return Collections.emptyList();
        }
        return settings.stream()
                .map(this::toProto)
                .collect(Collectors.toList());
    }

    private ProjectSettingProto.ProjectSettingMessage toProto(ProjectSetting setting) {
        if (setting == null) {
            return null;
        }
        return ProjectSettingProto.ProjectSettingMessage.newBuilder()
                .setProjectId(setting.getProjectId())
                .setCode(Optional.ofNullable(setting.getCode()).orElse(StringUtils.EMPTY))
                .setValue(Optional.ofNullable(setting.getValue()).orElse(StringUtils.EMPTY))
                .setId(Optional.ofNullable(setting.getId()).orElse(0))
                .build();
    }
}
