package net.coding.app.project.grpc;

import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectLabelDao;
import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.form.ProjectLabelForm;
import net.coding.lib.project.service.ProjectLabelService;
import net.coding.proto.platform.project.ProjectLabelProto;
import net.coding.proto.platform.project.ProjectLabelProto.GetLabelsByProjectIdResponse;
import net.coding.proto.platform.project.ProjectLabelServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectLabelGrpcService extends ProjectLabelServiceGrpc.ProjectLabelServiceImplBase {

    private final ProjectLabelService projectLabelService;
    private final ProjectDao projectDao;
    private final Validator validator;


    /**
     * <pre>
     * 获取项目下的标签列表
     * </pre>
     */
    @Override
    public void getLabelsByProjectId(ProjectLabelProto.GetLabelsByProjectIdRequest request,
                                     io.grpc.stub.StreamObserver<ProjectLabelProto.GetLabelsByProjectIdResponse> responseObserver) {
        if (request.getProjectId() <= 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("getLabelsByProjectId invalid parameters, projectId is " + request.getProjectId())
                    .asRuntimeException()
            );
            return;
        }
        try {
            List<ProjectLabel> list = projectLabelService
                    .getAllLabelByProject(request.getProjectId());
            List<ProjectLabelProto.ProjectLabel> result = list.stream()
                    .map(this::toBuilder)
                    .collect(Collectors.toList());
            responseObserver.onNext(GetLabelsByProjectIdResponse.newBuilder()
                    .addAllList(result)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getLabelsByProjectId exception", e);
            Status status = Status.fromThrowable(e);
            if (status.getCode().equals(Status.UNKNOWN.getCode())) {
                status = Status.INTERNAL;
            }
            responseObserver.onError(status
                    .withDescription("get labels by projectId fail, " + e.getMessage() + ", projectId is " + request.getProjectId())
                    .asRuntimeException());
        }
    }

    /**
     * 创建标签
     */
    @Override
    public void createLabel(ProjectLabelProto.CreateLabelRequest request,
                            io.grpc.stub.StreamObserver<ProjectLabelProto.CreateLabelResponse> responseObserver) {

        if (request.getOwnerId() <= 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("createLabel invalid parameters, ownerId is " + request.getOwnerId())
                    .asRuntimeException());
            return;
        }
        ProjectLabelForm form = ProjectLabelForm.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .color(request.getColor())
                .build();
        String exceptionKey = validateCreateParams(form);
        if (StringUtils.isNoneEmpty(exceptionKey)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(exceptionKey).asRuntimeException());
            return;
        }
        try {
            int id = projectLabelService.createLabel(request.getOwnerId(), form);
            responseObserver.onNext(ProjectLabelProto.CreateLabelResponse.newBuilder()
                    .setId(id)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("createLabel exception", e);
            Status status = Status.fromThrowable(e);
            if (status.getCode().equals(Status.UNKNOWN.getCode())) {
                status = Status.INTERNAL;
            }
            responseObserver.onError(status.withDescription(
                            "createLabel fail, " + e.getMessage() +
                                    ", projectId is " + request.getProjectId() +
                                    ", name is " + request.getName() +
                                    ", color is " + request.getColor() +
                                    ", ownerId is " + request.getOwnerId())
                    .asRuntimeException());
        }
    }

    @Override
    public void getLabelById(
            ProjectLabelProto.GetLabelByIdRequest request,
            StreamObserver<ProjectLabelProto.GetLabelByIdResponse> responseObserver
    ) {
        ProjectLabelProto.GetLabelByIdResponse.Builder builder =
                ProjectLabelProto.GetLabelByIdResponse.newBuilder();
        try {
            Integer id = request.getId();
            boolean withDeleted = request.getWithDeleted();
            ProjectLabel projectLabel;
            if (withDeleted) {
                projectLabel = projectLabelService.findByIdWithDeleted(id);
            } else {
                projectLabel = projectLabelService.findById(id);
            }
            if (projectLabel == null) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                builder.setProjectLabel(toBuilder(projectLabel));
            }

        } catch (Exception e) {
            log.error("RpcService getLabelById error {} ", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLabelByIdList(
            ProjectLabelProto.GetLabelByIdListRequest request,
            StreamObserver<ProjectLabelProto.GetLabelByIdListResponse> responseObserver
    ) {
        ProjectLabelProto.GetLabelByIdListResponse.Builder builder =
                ProjectLabelProto.GetLabelByIdListResponse.newBuilder();
        try {
            List<Integer> ids = request.getIdList();
            boolean withDeleted = request.getWithDeleted();
            List<ProjectLabel> projectLabels;
            if (withDeleted) {
                projectLabels = projectLabelService.getByIdsWithDeleted(ids);
            } else {
                projectLabels = projectLabelService.getByIds(ids);
            }
            if (CollectionUtils.isEmpty(projectLabels)) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                List<ProjectLabelProto.ProjectLabel> projectLabelList = projectLabels
                        .stream()
                        .map(this::toBuilder)
                        .collect(Collectors.toList());
                builder.setCode(CodeProto.Code.SUCCESS)
                        .addAllProjectLabel(projectLabelList)
                        .build();
            }
        } catch (Exception e) {
            log.error("RpcService getLabelByIdList error {} ", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLabelsByProjectIdListAndNames(ProjectLabelProto.GetLabelsByProjectIdListAndNamesRequest request, StreamObserver<ProjectLabelProto.GetLabelsByProjectIdListAndNamesResponse> responseObserver) {
        List<String> names = request.getNameList();
        List<Integer> projectIdList = request.getProjectIdList();
        if (names.isEmpty() || projectIdList.isEmpty()) {
            responseObserver.onNext(
                    ProjectLabelProto.GetLabelsByProjectIdListAndNamesResponse.newBuilder()
                            .setCode(CodeProto.Code.INVALID_PARAMETER)
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        try {
            List<ProjectLabel> projectLabels = projectLabelService.getLabelsByProjectIdAndNames(names, projectIdList);
            responseObserver.onNext(
                    ProjectLabelProto.GetLabelsByProjectIdListAndNamesResponse.newBuilder()
                            .setCode(CodeProto.Code.SUCCESS)
                            .addAllProjectLabel(projectLabels.stream().map(this::toBuilder).collect(Collectors.toList()))
                            .build()
            );
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            responseObserver.onNext(
                    ProjectLabelProto.GetLabelsByProjectIdListAndNamesResponse.newBuilder()
                            .setCode(CodeProto.Code.INTERNAL_ERROR)
                            .build()
            );
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLabelByProjectAndName(
            ProjectLabelProto.GetLabelByProjectAndNameRequest request,
            StreamObserver<ProjectLabelProto.GetLabelByProjectAndNameResponse> responseObserver
    ) {
        ProjectLabelProto.GetLabelByProjectAndNameResponse.Builder builder =
                ProjectLabelProto.GetLabelByProjectAndNameResponse.newBuilder();
        try {
            ProjectLabel projectLabel = projectLabelService.getByNameAndProject(
                    request.getName(),
                    request.getProjectId()
            );
            if (projectLabel == null) {
                builder.setCode(CodeProto.Code.NOT_FOUND);
            } else {
                builder.setProjectLabel(toBuilder(projectLabel))
                        .setCode(CodeProto.Code.SUCCESS);
            }
        } catch (Exception e) {
            log.error("RpcService getLabelByProjectAndName error {} ", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLabelsByTeamIdAndNames(ProjectLabelProto.GetLabelsByTeamIdAndNamesRequest request, StreamObserver<ProjectLabelProto.GetLabelsByTeamIdAndNamesResponse> responseObserver) {
        Integer teamId = request.getTeamId();
        List<String> names = request.getNameList();
        if (teamId == 0 || names.isEmpty()) {
            responseObserver.onNext(ProjectLabelProto.GetLabelsByTeamIdAndNamesResponse.newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build()
            );
            responseObserver.onCompleted();
            return;
        }

        try {
            List<ProjectLabel> list = projectLabelService.getLabelsByProjectIdAndNames(names, projectDao.getAllProjectIdByTeamId(teamId));
            responseObserver.onNext(
                    ProjectLabelProto.GetLabelsByTeamIdAndNamesResponse
                            .newBuilder()
                            .setCode(CodeProto.Code.SUCCESS)
                            .addAllProjectLabel(list.stream().map(this::toBuilder).collect(Collectors.toList()))
                            .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            responseObserver.onNext(ProjectLabelProto.GetLabelsByTeamIdAndNamesResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build()
            );
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLabelsByTeamId(ProjectLabelProto.GetLabelsByTeamIdRequest request, StreamObserver<ProjectLabelProto.GetLabelsByTeamIdResponse> responseObserver) {
        Integer teamId = request.getTeamId();
        if (teamId == 0) {
            responseObserver.onNext(ProjectLabelProto.GetLabelsByTeamIdResponse.newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build()
            );
            responseObserver.onCompleted();
            return;
        }

        try {
            List<ProjectLabel> list = projectLabelService.getLabelsByProjectIds(projectDao.getAllProjectIdByTeamId(teamId));
            responseObserver.onNext(
                    ProjectLabelProto.GetLabelsByTeamIdResponse
                            .newBuilder()
                            .setCode(CodeProto.Code.SUCCESS)
                            .addAllProjectLabel(list.stream().map(this::toBuilder).collect(Collectors.toList()))
                            .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            responseObserver.onNext(ProjectLabelProto.GetLabelsByTeamIdResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build()
            );
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getOrCreateLabel(ProjectLabelProto.GetOrCreateLabelRequest request, StreamObserver<ProjectLabelProto.GetOrCreateLabelResponse> responseObserver) {
        Integer projectId = request.getProjectId();
        Integer userId = request.getUserId();
        String name = request.getName();
        String color = request.getColor();
        if (userId == 0) {
            responseObserver.onNext(ProjectLabelProto.GetOrCreateLabelResponse.newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build()
            );
            responseObserver.onCompleted();
            return;
        }

        ProjectLabelForm form = ProjectLabelForm.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .color(request.getColor())
                .build();
        String exceptionKey = validateCreateParams(form);
        if (StringUtils.isNoneEmpty(exceptionKey)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(exceptionKey).asRuntimeException());
            return;
        }

        try {
            ProjectLabel projectLabel = projectLabelService.getOrCreateLabel(projectId, userId, name, color);
            responseObserver.onNext(ProjectLabelProto.GetOrCreateLabelResponse.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setProjectLabel(toBuilder(projectLabel))
                    .build()
            );
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            responseObserver.onNext(ProjectLabelProto.GetOrCreateLabelResponse.newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTER ERROR")
                    .build()
            );
            responseObserver.onCompleted();
        }
    }

    private ProjectLabelProto.ProjectLabel toBuilder(ProjectLabel projectLabel) {
        if (projectLabel == null) {
            return null;
        }
        return ProjectLabelProto.ProjectLabel.newBuilder()
                .setId(projectLabel.getId())
                .setOwnerId(projectLabel.getOwnerId())
                .setName(projectLabel.getName())
                .setColor(projectLabel.getColor())
                .setProjectId(projectLabel.getProjectId())
                .build();
    }

    private String validateCreateParams(ProjectLabelForm form) {
        Set<ConstraintViolation<ProjectLabelForm>> errors = validator.validate(form);
        for (ConstraintViolation<ProjectLabelForm> error : errors) {
            return error.getMessageTemplate();
        }
        return null;
    }
}
