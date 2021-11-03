package net.coding.app.project.grpc;


import net.coding.common.util.BeanUtils;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectSetting;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectSettingService;
import net.coding.proto.platform.project.ProjectSettingProto;
import net.coding.proto.platform.project.ProjectSettingServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Override
    public void getProjectSettingByCode(
            ProjectSettingProto.ProjectSettingByCodeRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByCodeResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingByCodeResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByCodeResponse.newBuilder();
        try {
            Integer projectId = request.getProjectId();
            String code = request.getCode();
            Project project = projectService.getById(projectId);

            if (project == null || project.getDeletedAt().equals(BeanUtils.getDefaultArchivedAt())) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST_OR_ARCHIVED);
            }
            if (projectSettingService.isDisabledSystemMenu(code)) {
                //如果系统关闭菜单，则默认禁用
                builder.setCode(CodeProto.Code.SUCCESS);
                builder.setValue(ProjectSetting.valueFalse);
            } else {
                ProjectSetting projectSetting = projectSettingService.findProjectSetting(projectId, code);
                if (projectSetting == null) {
                    String defaultValue = projectSettingService.getCodeDefaultValue(code);
                    if (defaultValue == null) {
                        builder.setCode(CodeProto.Code.NOT_FOUND);
                    } else {
                        builder.setCode(CodeProto.Code.SUCCESS);
                        builder.setValue(defaultValue);
                    }
                } else {
                    builder.setCode(CodeProto.Code.SUCCESS);
                    builder.setValue(projectSetting.getValue());
                }
            }
        } catch (Exception e) {
            log.error("RpcService getProjectSettingByCode error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectSettingByCodes(
            ProjectSettingProto.ProjectSettingByCodesRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByCodesResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingByCodesResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByCodesResponse.newBuilder();
        try {
            List<String> codes = request.getCodesList();
            Integer projectId = request.getProjectId();
            Project project = projectService.getById(projectId);

            if (project == null || project.getDeletedAt().equals(BeanUtils.getDefaultArchivedAt())) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST_OR_ARCHIVED);
            }
            List<ProjectSetting> projectSettings = projectSettingService.findProjectSettings(projectId, codes);
            List<String> findCodes = projectSettings.stream().map(ProjectSetting::getCode).collect(Collectors.toList());
            codes.stream().filter(c -> !findCodes.contains(c)).forEach(c -> {
                String defaultValue = projectSettingService.getCodeDefaultValue(c);
                if (defaultValue != null) {
                    ProjectSetting projectSetting = new ProjectSetting();
                    projectSetting.setCode(c);
                    projectSetting.setProjectId(projectId);
                    projectSetting.setValue(defaultValue);
                    projectSetting.setId(0);
                    projectSettings.add(projectSetting);
                }
            });

            //如果系统关闭菜单，则默认禁用
            projectSettings.forEach(setting -> {
                if (projectSettingService.isDisabledSystemMenu(setting.getCode())) {
                    setting.setValue(ProjectSetting.valueFalse);
                }
            });

            if (CollectionUtils.isEmpty(projectSettings)) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                builder.addAllData(toBuilderList(projectSettings)).build();
            }
        } catch (Exception e) {
            log.error("RpcService getProjectSettingByCodes error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void projectSettingUpdate(
            ProjectSettingProto.ProjectSettingUpdateRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingUpdateResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingUpdateResponse.Builder builder =
                ProjectSettingProto.ProjectSettingUpdateResponse.newBuilder();
        String code = request.getCode();
        String value = request.getValue();
        Integer projectId = request.getProjectId();
        try {
            Project project = projectService.getById(projectId);
            if (project == null || project.getDeletedAt().equals(BeanUtils.getDefaultArchivedAt())) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST_OR_ARCHIVED);
            }
            ProjectSettingProto.ProjectSettingMessage ps = ProjectSettingProto.ProjectSettingMessage
                    .newBuilder()
                    .setCode(code)
                    .setValue(value)
                    .setProjectId(projectId)
                    .build();
            boolean result = saveOrUpdateSetting(ps);
            if (result) {
                ProjectSetting projectSetting = projectSettingService.findProjectSetting(projectId, code);
                builder.setData(toBuilderSetting(projectSetting));
            } else {
                builder.setCode(CodeProto.Code.INTERNAL_ERROR);
            }
        } catch (Exception e) {
            log.error("RpcService projectSettingUpdate error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void projectSettingBatchUpdate(
            ProjectSettingProto.ProjectSettingBatchUpdateRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingBatchUpdateResponse> responseObserver
    ) {
        ProjectSettingProto.ProjectSettingBatchUpdateResponse.Builder builder =
                ProjectSettingProto.ProjectSettingBatchUpdateResponse.newBuilder();
        List<ProjectSettingProto.ProjectSettingMessage> projectSettings = request.getProjectSettingsList();
        try {
            projectSettings.forEach(ps -> {
                boolean result = saveOrUpdateSetting(ps);
                if (!result) {
                    builder.setCode(CodeProto.Code.INTERNAL_ERROR);
                    responseObserver.onNext(builder.build());
                    responseObserver.onCompleted();
                }
            });
            builder.setCode(CodeProto.Code.SUCCESS);
        } catch (Exception e) {
            log.error("RpcService projectSettingBatchUpdate error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
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
                builder.setData(toBuilderSetting(projectSetting));
            }
        } catch (Exception e) {
            log.error("RpcService getProjectSettingById error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectSettingByProjectIdsAndCode(
            ProjectSettingProto.ProjectSettingByProjectIdsAndCodeRequest request,
            StreamObserver<ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse> responseObserver
    ) {
        List<Integer> projectIds = request.getProjectIdList();
        String code = request.getCode();
        ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse.Builder builder =
                ProjectSettingProto.ProjectSettingByProjectIdsAndCodeResponse.newBuilder();

        try {
            List<ProjectSettingProto.ProjectSettingMessage> projectSettingMessages = new ArrayList<>();
            if (projectIds.size() > 0) {
                List<Project> projects = projectService.getByIds(projectIds);
                if (CollectionUtils.isEmpty(projects)) {
                    throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
                }
                List<Integer> willBatchSelectProjectSetting = new ArrayList<>();
                projects.forEach(project -> {
                            if (project.getInvisible()) {
                                projectSettingMessages.add(
                                        toBuilderSetting(project.getId(), code, ProjectSetting.valueFalse, 0)
                                );
                            } else {
                                willBatchSelectProjectSetting.add(project.getId());
                            }
                        }
                );

                if (CollectionUtils.isNotEmpty(willBatchSelectProjectSetting)) {
                    List<ProjectSetting> projectSettings = projectSettingService.findProjectsSetting(
                            willBatchSelectProjectSetting,
                            code
                    );
                    List<Integer> existSettingProjectIds = new ArrayList<>(projectSettings.size());
                    if (CollectionUtils.isEmpty(projectSettings)) {
                        builder.setCode(CodeProto.Code.NOT_FOUND);
                    } else {
                        projectSettings.forEach(projectSetting -> {
                            existSettingProjectIds.add(projectSetting.getProjectId());
                            projectSettingMessages.add(
                                    toBuilderSetting(
                                            projectSetting.getProjectId(),
                                            code,
                                            projectSetting.getValue(),
                                            projectSetting.getId()
                                    )
                            );
                        });
                    }
                    // 不存在setting的项目id，填充默认值。
                    willBatchSelectProjectSetting.removeAll(existSettingProjectIds);
                    if (CollectionUtils.isNotEmpty(willBatchSelectProjectSetting)) {
                        ProjectSetting.Code defaultCode = ProjectSetting.Code.getByCode(code);
                        if (defaultCode != null) {
                            String defaultValue = defaultCode.getDefaultValue();
                            willBatchSelectProjectSetting.forEach(
                                    projectId -> projectSettingMessages.add(
                                            toBuilderSetting(projectId, code, defaultValue, 0)
                                    )
                            );
                        }
                    }
                }
            }
            builder.setCode(CodeProto.Code.SUCCESS)
                    .addAllValue(projectSettingMessages);
        } catch (Exception e) {
            log.error("RpcService getProjectSettingById error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    //npe e.getMessage is null
                    .setMessage(e.getMessage() == null ? "" : e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private ProjectSettingProto.ProjectSettingMessage toBuilderSetting(
            Integer projectId,
            String code,
            String value,
            Integer id
    ) {
        return ProjectSettingProto.ProjectSettingMessage.newBuilder()
                .setProjectId(projectId)
                .setCode(code)
                .setValue(value)
                .setId(id)
                .build();
    }

    private boolean saveOrUpdateSetting(ProjectSettingProto.ProjectSettingMessage ps) {
        String code = ps.getCode();
        String value = ps.getValue();
        Integer projectId = ps.getProjectId();
        ProjectSetting.ProjectSettingBuilder builder = ProjectSetting.builder();
        builder.projectId(projectId)
                .code(code)
                .value(value)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .deletedAt(BeanUtils.getDefaultDeletedAt());
        ProjectSetting projectSetting = projectSettingService.findProjectSetting(projectId, code);
        if (projectSetting != null) {
            builder.id(projectSetting.getId());
        }
        return projectSettingService.saveOrUpdateProjectSetting(builder.build());
    }

    private List<ProjectSettingProto.ProjectSettingMessage> toBuilderList(List<ProjectSetting> projectSettings) {
        if (CollectionUtils.isEmpty(projectSettings)) {
            return Collections.emptyList();
        }
        return projectSettings.stream()
                .map(this::toBuilderSetting)
                .collect(Collectors.toList());
    }

    private ProjectSettingProto.ProjectSettingMessage toBuilderSetting(ProjectSetting ps) {
        if (ps == null) {
            return null;
        }
        return ProjectSettingProto.ProjectSettingMessage.newBuilder()
                .setProjectId(ps.getProjectId())
                .setCode(ps.getCode())
                .setValue(ps.getValue())
                .setId(ps.getId())
                .build();
    }
}
