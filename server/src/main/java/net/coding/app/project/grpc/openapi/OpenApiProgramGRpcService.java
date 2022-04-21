package net.coding.app.project.grpc.openapi;

import net.coding.common.base.util.QiniuCDNReplaceUtil;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.AppProperties;
import net.coding.lib.project.dto.ProgramDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramPageQueryParameter;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.service.ProgramService;
import net.coding.lib.project.service.openapi.OpenApiProgramService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.open.api.program.ProgramProto;
import net.coding.proto.open.api.program.ProgramServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static net.coding.e.proto.ApiCodeProto.Code.INVALID_PARAMETER;
import static net.coding.e.proto.ApiCodeProto.Code.NOT_FOUND;
import static net.coding.e.proto.ApiCodeProto.Code.SUCCESS;

/**
 * @Description: OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 * @Author liheng
 * @Date 2021/1/4 4:16 下午
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProgramGRpcService extends ProgramServiceGrpc.ProgramServiceImplBase {

    private final OpenApiProgramService openApiProgramService;

    private final ProgramService programService;

    private final LocaleMessageSource localeMessageSource;

    private final AppProperties appProperties;

    @Override
    public void describePrograms(
            ProgramProto.DescribeProgramsRequest request,
            StreamObserver<ProgramProto.DescribeProgramsResponse> responseObserver) {
        ProgramProto.DescribeProgramsResponse.Builder builder =
                ProgramProto.DescribeProgramsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            ResultPage<ProgramDTO> resultPage = openApiProgramService.findProgramPages(
                    ProgramPageQueryParameter.builder()
                            .teamId(request.getUser().getTeamId())
                            .userId(request.getUser().getId())
                            .keyword(request.getKeyword())
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .page(request.getPageNumber())
                            .pageSize(request.getPageSize())
                            .build()
            );
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build())
                    .setData(complexProgramsToProto(resultPage));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describePrograms error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(INVALID_PARAMETER.getNumber())
                            .setMessage(INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void describeProgramProjects(
            ProgramProto.DescribeProgramProjectsRequest request,
            StreamObserver<ProgramProto.DescribeProgramProjectsResponse> responseObserver) {
        ProgramProto.DescribeProgramProjectsResponse.Builder builder =
                ProgramProto.DescribeProgramProjectsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            Project program = programService.getProgram(
                    request.getUser().getTeamId(),
                    request.getUser().getId(),
                    request.getProgramId()
            );
            List<Project> projects = programService.getProgramProjects(
                    ProgramProjectQueryParameter.builder()
                            .teamId(request.getUser().getTeamId())
                            .programId(program.getId())
                            .build()
            );
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build())
                    .addAllData(complexProjectsToProto(projects));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describePrograms error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(INVALID_PARAMETER.getNumber())
                            .setMessage(INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    public ProgramProto.ProgramData complexProgramsToProto(ResultPage<ProgramDTO> resultPage) {
        List<ProgramProto.Program> programs = Optional.ofNullable(resultPage)
                .map(result -> StreamEx.of(result.getList())
                        .nonNull()
                        .map(this::programToProto)
                        .nonNull()
                        .toList()
                )
                .orElse(Collections.emptyList());
        Objects.requireNonNull(resultPage);
        return ProgramProto.ProgramData.newBuilder()
                .setPageSize(resultPage.getPageSize())
                .setPageNumber(resultPage.getPage())
                .setTotalCount(resultPage.getTotalRow())
                .addAllPrograms(programs)
                .build();
    }

    public List<ProgramProto.Program> complexProjectsToProto(List<Project> projects) {
        return Optional.ofNullable(projects)
                .map(projectList -> StreamEx.of(projectList)
                        .nonNull()
                        .map(this::projectToProto)
                        .nonNull()
                        .toList()
                )
                .orElse(Collections.emptyList());
    }

    public ProgramProto.Program programToProto(ProgramDTO programDTO) {
        return projectToProto(Project.builder()
                .id(programDTO.getId())
                .name(programDTO.getName())
                .displayName(programDTO.getDisplayName())
                .namePinyin(programDTO.getNamePinyin())
                .icon(programDTO.getIcon())
                .description(programDTO.getDescription())
                .startDate(programDTO.getStartDate())
                .endDate(programDTO.getEndDate())
                .createdAt(programDTO.getCreatedAt())
                .updatedAt(programDTO.getUpdatedAt())
                .build()
        );
    }

    public ProgramProto.Program projectToProto(Project project) {
        String icon = project.getIcon();
        if (StringUtils.startsWith(icon, "/")) {
            icon = appProperties.getDomain().getSchemaHome() + icon;
        }
        icon = QiniuCDNReplaceUtil.replace(icon);
        return ProgramProto.Program.newBuilder()
                .setId(project.getId())
                .setName(project.getName())
                .setDisplayName(project.getDisplayName())
                .setNamePinyin(project.getNamePinyin())
                .setIcon(icon)
                .setDescription(TextUtils.filterUserInputContent(project.getDescription()))
                .setStartDate(Optional.ofNullable(project.getStartDate()).map(DateUtil::DateToTime).orElse(0L))
                .setEndDate(Optional.ofNullable(project.getEndDate()).map(DateUtil::DateToTime).orElse(0L))
                .setCreatedAt(project.getCreatedAt().getTime())
                .setUpdatedAt(project.getUpdatedAt().getTime())
                .build();
    }
}
