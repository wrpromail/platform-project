package net.coding.app.project.grpc;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.service.ProgramService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.platform.program.ProgramProto;
import net.coding.proto.platform.program.ProgramServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.common.CodeProto;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProgramGRpcService extends ProgramServiceGrpc.ProgramServiceImplBase {
    private final ProgramService programService;

    @Override
    public void getProgramById(ProgramProto.GetProgramByIdRequest request,
                               StreamObserver<ProgramProto.GetProgramByIdResponse> responseObserver) {
        try {
            Project program = programService.getProgram(request.getTeamId(), request.getUserId(), request.getProgramId());
            getProgramByIdResponse(responseObserver, CodeProto.Code.SUCCESS,
                    CodeProto.Code.SUCCESS.name(), program);
        } catch (CoreException e) {
            getProgramByIdResponse(responseObserver, CodeProto.Code.NOT_FOUND,
                    e.getMsg(), null);
        } catch (Exception e) {
            log.error("rpcService getProgramById error Exception ", e);
            getProgramByIdResponse(responseObserver, CodeProto.Code.INTERNAL_ERROR,
                    e.getMessage(), null);
        }
    }

    @Override
    public void getProgramList(ProgramProto.GetProgramListRequest request,
                               StreamObserver<ProgramProto.GetProgramListResponse> responseObserver) {
        try {
            List<Project> programs = programService.getPrograms(
                    request.getTeamId(), request.getProjectId(), request.getUserId());
            getProgramListResponse(responseObserver, CodeProto.Code.SUCCESS,
                    CodeProto.Code.SUCCESS.name(), programs);
        } catch (CoreException e) {
            getProgramListResponse(responseObserver, CodeProto.Code.NOT_FOUND,
                    e.getMsg(), null);
        } catch (Exception e) {
            log.error("rpcService getProgramList error Exception ", e);
            getProgramListResponse(responseObserver, CodeProto.Code.INTERNAL_ERROR,
                    e.getMessage(), null);
        }
    }

    @Override
    public void getProgramProjectList(ProgramProto.GetProgramProjectListRequest request,
                                      StreamObserver<ProgramProto.GetProgramProjectListResponse> responseObserver) {
        try {
            List<Project> projects = programService.getProgramProjects(ProgramProjectQueryParameter.builder()
                    .teamId(request.getTeamId())
                    .programId(request.getProgramId())
                    .userId(request.getUserId())
                    .build());
            getProgramProjectListResponse(responseObserver, CodeProto.Code.SUCCESS,
                    CodeProto.Code.SUCCESS.name(), projects);
        } catch (Exception e) {
            log.error("rpcService getProgramProjectList error Exception ", e);
            getProgramProjectListResponse(responseObserver, CodeProto.Code.INTERNAL_ERROR,
                    e.getMessage(), null);
        }
    }

    private void getProgramProjectListResponse(
            StreamObserver<ProgramProto.GetProgramProjectListResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Project> projects) {
        ProgramProto.GetProgramProjectListResponse.Builder builder = ProgramProto.GetProgramProjectListResponse
                .newBuilder()
                .setCode(code)
                .setMessage(message);
        if (CollectionUtils.isNotEmpty(projects)) {
            builder.addAllProjectList(toProtoProgramProjects(projects));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void getProgramListResponse(
            StreamObserver<ProgramProto.GetProgramListResponse> responseObserver,
            CodeProto.Code code,
            String message,
            List<Project> programs) {
        ProgramProto.GetProgramListResponse.Builder builder = ProgramProto.GetProgramListResponse
                .newBuilder()
                .setCode(code)
                .setMessage(message);
        if (CollectionUtils.isNotEmpty(programs)) {
            builder.addAllProgramList(toProtoProgramProjects(programs));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void getProgramByIdResponse(
            StreamObserver<ProgramProto.GetProgramByIdResponse> responseObserver,
            CodeProto.Code code,
            String message,
            Project program) {
        ProgramProto.GetProgramByIdResponse.Builder builder = ProgramProto.GetProgramByIdResponse
                .newBuilder()
                .setCode(code)
                .setMessage(message);
        if (Objects.nonNull(program)) {
            builder.setProgram(toProtoProgramProject(program));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private List<ProgramProto.ProgramProject> toProtoProgramProjects(List<Project> programs) {
        return StreamEx.of(programs)
                .nonNull()
                .map(this::toProtoProgramProject)
                .nonNull()
                .collect(Collectors.toList());
    }

    private ProgramProto.ProgramProject toProtoProgramProject(Project program) {
        return ProgramProto.ProgramProject.newBuilder()
                .setId(program.getId())
                .setName(StringUtils.defaultString(program.getName()))
                .setDisplayName(StringUtils.defaultString(program.getDisplayName()))
                .setDescription(StringUtils.defaultString(program.getDescription()))
                .setIcon(StringUtils.defaultString(program.getIcon()))
                .setStartDate(program.getStartDate() != null ? program.getStartDate().getTime() : 0)
                .setEndDate(program.getEndDate() != null ? program.getEndDate().getTime() : 0)
                .setArchived(DateUtil.strToDate(BeanUtils.ARCHIVED_AT).equals(program.getDeletedAt()))
                .build();
    }
}
