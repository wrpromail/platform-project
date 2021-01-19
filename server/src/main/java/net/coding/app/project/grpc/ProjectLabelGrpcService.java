package net.coding.app.project.grpc;

import io.grpc.Status;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.form.ProjectLabelForm;
import net.coding.lib.project.service.ProjectLabelService;
import net.coding.proto.platform.project.ProjectLabelProto;
import net.coding.proto.platform.project.ProjectLabelProto.GetLabelsByProjectIdResponse;
import net.coding.proto.platform.project.ProjectLabelServiceGrpc;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

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
                    .map(item -> ProjectLabelProto.ProjectLabel.newBuilder()
                            .setId(item.getId())
                            .setOwnerId(item.getOwnerId())
                            .setName(item.getName())
                            .setColor(item.getColor())
                            .setProjectId(item.getProjectId())
                            .build())
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

    private String validateCreateParams(ProjectLabelForm form) {
        Set<ConstraintViolation<ProjectLabelForm>> errors = validator.validate(form);
        for (ConstraintViolation<ProjectLabelForm> error : errors) {
            return error.getMessageTemplate();
        }
        return null;
    }
}
