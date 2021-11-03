package net.coding.app.project.grpc;

import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.form.ProjectLabelForm;
import net.coding.lib.project.service.ProjectLabelService;
import net.coding.proto.platform.project.ProjectLabelProto;
import net.coding.proto.platform.project.ProjectLabelProto.GetLabelsByProjectIdResponse;
import net.coding.proto.platform.project.ProjectLabelServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
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
            ProjectLabel projectLabel = projectLabelService.findById(id);
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
            List<ProjectLabel> projectLabels = projectLabelService.getByIds(ids);
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
