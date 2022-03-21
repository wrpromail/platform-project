package net.coding.app.project.grpc.openapi;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.common.util.StringUtils;
import net.coding.e.proto.ApiUserProto;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberQueryPageReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberReqDTO;
import net.coding.lib.project.dto.response.ProjectMemberQueryPageRespDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectActionPermissionEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalWriteService;
import net.coding.platform.ram.pojo.dto.PredicateContextInfo;
import net.coding.platform.ram.pojo.dto.request.ResourceGrantDTO;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;
import net.coding.platform.ram.service.PredicateRemoteService;
import net.coding.proto.open.api.project.principal.ProjectMemberPrincipalProto;
import net.coding.proto.open.api.project.principal.ProjectMemberPrincipalServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.collections4.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static net.coding.e.proto.ApiCodeProto.Code.INVALID_PARAMETER;
import static net.coding.e.proto.ApiCodeProto.Code.NOT_FOUND;
import static net.coding.e.proto.ApiCodeProto.Code.SUCCESS;
import static net.coding.lib.project.enums.ProjectActionPermissionEnums.CREATE_PROJECT_MEMBER;
import static net.coding.lib.project.enums.ProjectActionPermissionEnums.DELETE_PROJECT_MEMBER;
import static net.coding.lib.project.enums.ProjectActionPermissionEnums.VIEW_PROJECT_MEMBER;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProjectMemberPrincipalGRpcService
        extends ProjectMemberPrincipalServiceGrpc.ProjectMemberPrincipalServiceImplBase {
    private final ProjectDao projectDao;

    private final LocaleMessageSource localeMessageSource;

    private final PredicateRemoteService predicateRemoteService;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProjectMemberPrincipalService projectMemberPrincipalService;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    @Override
    public void describeProjectMemberPrincipals(
            ProjectMemberPrincipalProto.DescribeProjectMemberPrincipalsRequest request,
            StreamObserver<ProjectMemberPrincipalProto.DescribeProjectMemberPrincipalsResponse> responseObserver) {
        ProjectMemberPrincipalProto.DescribeProjectMemberPrincipalsResponse.Builder builder =
                ProjectMemberPrincipalProto.DescribeProjectMemberPrincipalsResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            if (!hasPermission(request.getUser(), request.getProjectId(), VIEW_PROJECT_MEMBER)) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            ResultPage<ProjectMemberQueryPageRespDTO> resultPage =
                    projectMemberPrincipalService.findProjectMemberPrincipalPages(
                            ProjectMemberQueryPageReqDTO.builder()
                                    .teamId(request.getUser().getTeamId())
                                    .userId(request.getUser().getId())
                                    .projectId(request.getProjectId())
                                    .policyId(request.getPolicyId())
                                    .keyword(request.getKeyword())
                                    .build(),
                            new LimitedPager(request.getPageNumber(), request.getPageSize()));
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build())
                    .setData(complexMemberPrincipalToProto(request.getUser().getId(), request.getProjectId(), resultPage));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder.setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build());
        } catch (Exception e) {
            log.error("rpcService describeProjectMemberPrincipals error Exception ", e);
            builder.setResult(
                    resultBuilder.setCode(INVALID_PARAMETER.getNumber())
                            .setMessage(INVALID_PARAMETER.name().toLowerCase())
                            .build());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createProjectMemberPrincipal(
            ProjectMemberPrincipalProto.CreateProjectMemberPrincipalRequest request,
            StreamObserver<ProjectMemberPrincipalProto.CreateProjectMemberPrincipalResponse> responseObserver) {
        ProjectMemberPrincipalProto.CreateProjectMemberPrincipalResponse.Builder builder =
                ProjectMemberPrincipalProto.CreateProjectMemberPrincipalResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            if (!hasPermission(request.getUser(), request.getProjectId(), CREATE_PROJECT_MEMBER)) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            projectMemberPrincipalWriteService.addMember(
                    request.getUser().getTeamId(),
                    request.getUser().getId(),
                    request.getProjectId(),
                    StreamEx.of(request.getPrincipalsList())
                            .map(principal -> ProjectMemberAddReqDTO.builder()
                                    .principalId(principal.getPrincipalId())
                                    .principalType(ProjectMemberPrincipalTypeEnum.valueOf(principal.getPrincipalType().name()))
                                    .policyIds(StreamEx.of(principal.getPolicyIdsList()).toSet())
                                    .build()
                            ).toList());
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build());
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder.setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build());
        } catch (Exception e) {
            log.error("rpcService createProjectMemberPrincipal error Exception ", e);
            builder.setResult(
                    resultBuilder.setCode(INVALID_PARAMETER.getNumber())
                            .setMessage(INVALID_PARAMETER.name().toLowerCase())
                            .build());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteProjectMemberPrincipal(
            ProjectMemberPrincipalProto.DeleteProjectMemberPrincipalRequest request,
            StreamObserver<ProjectMemberPrincipalProto.DeleteProjectMemberPrincipalResponse> responseObserver) {
        ProjectMemberPrincipalProto.DeleteProjectMemberPrincipalResponse.Builder builder =
                ProjectMemberPrincipalProto.DeleteProjectMemberPrincipalResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            if (!hasPermission(request.getUser(), request.getProjectId(), DELETE_PROJECT_MEMBER)) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            projectMemberPrincipalWriteService.delMember(
                    request.getUser().getTeamId(),
                    request.getUser().getId(),
                    request.getProjectId(),
                    StreamEx.of(request.getPrincipalsList())
                            .map(principal -> ProjectMemberReqDTO.builder()
                                    .principalId(principal.getPrincipalId())
                                    .principalType(ProjectMemberPrincipalTypeEnum.valueOf(principal.getPrincipalType().name()))
                                    .build()
                            ).toList());
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build());
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder.setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build());
        } catch (Exception e) {
            log.error("rpcService deleteProjectMemberPrincipal error Exception ", e);
            builder.setResult(
                    resultBuilder.setCode(INVALID_PARAMETER.getNumber())
                            .setMessage(INVALID_PARAMETER.name().toLowerCase())
                            .build());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    public ProjectMemberPrincipalProto.PrincipalData complexMemberPrincipalToProto(
            Integer operatorId,
            Integer projectId,
            ResultPage<ProjectMemberQueryPageRespDTO> resultPage) {
        List<ProjectMemberPrincipalProto.PrincipalResponse> principals = Optional.ofNullable(resultPage)
                .map(result -> {
                            if (CollectionUtils.isEmpty(result.getList())) {
                                return new ArrayList<ProjectMemberPrincipalProto.PrincipalResponse>();
                            }
                            Project project = projectDao.getProjectById(projectId);
                            List<ProjectMember> members = StreamEx.of(result.getList())
                                    .nonNull()
                                    .map(dto -> ProjectMember.builder()
                                            .principalId(dto.getPrincipalId())
                                            .principalType(dto.getPrincipalType())
                                            .build()
                                    ).toList();
                            Map<ResourceGrantDTO, List<PolicyResponseDTO>> policies =
                                    projectMemberInspectService.getResourceGrantPolicies(operatorId, project, members, new HashSet<>());
                            return StreamEx.of(result.getList())
                                    .nonNull()
                                    .map(dto -> memberPrincipalToProto(dto, policies))
                                    .nonNull()
                                    .toList();
                        }
                )
                .orElse(Collections.emptyList());
        Objects.requireNonNull(resultPage);
        return ProjectMemberPrincipalProto.PrincipalData.newBuilder()
                .setPageSize(resultPage.getPageSize())
                .setPageNumber(resultPage.getPage())
                .setTotalCount(resultPage.getTotalRow())
                .addAllPrincipals(principals)
                .build();
    }

    public ProjectMemberPrincipalProto.PrincipalResponse memberPrincipalToProto(
            ProjectMemberQueryPageRespDTO dto,
            Map<ResourceGrantDTO, List<PolicyResponseDTO>> policiesMap) {
        List<ProjectMemberPrincipalProto.Policy> policies = Optional.ofNullable(policiesMap)
                .map(policyMap ->
                        policyMap.entrySet().stream()
                                .filter(entry -> entry.getKey().getGrantScope().equals(dto.getPrincipalType())
                                        && entry.getKey().getGrantObjectId().equals(dto.getPrincipalId()))
                                .flatMap(entry -> StreamEx.of(entry.getValue())
                                        .map(policy ->
                                                ProjectMemberPrincipalProto.Policy.newBuilder()
                                                        .setPolicyId(policy.getPolicyId())
                                                        .setPolicyName(policy.getName())
                                                        .setPolicyAlias(policy.getAlias())
                                                        .build()

                                        ))
                                .collect(Collectors.toList())
                ).orElse(Collections.emptyList());
        return ProjectMemberPrincipalProto.PrincipalResponse.newBuilder()
                .setPrincipalId(dto.getPrincipalId())
                .setPrincipalName(dto.getPrincipalName())
                .setPrincipalType(dto.getPrincipalType())
                .setCreatedAt(dto.getCreatedAt())
                .addAllPolicies(policies)
                .build();
    }

    public Boolean hasPermission(ApiUserProto.User user, Integer projectId, ProjectActionPermissionEnums projectAction) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, user.getTeamId());
        if (Objects.isNull(project)) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        return predicateRemoteService.hasPermission(
                new PredicateContextInfo.PredicateUserContext((long) user.getId(), (long) user.getTeamId()),
                PredicateContextInfo.ActionContextInfo.ofExpression(
                        StringUtils.join(Arrays.asList(PmTypeEnums.PROJECT.name().toLowerCase(), projectAction.getAction()), ":")),
                new PredicateContextInfo.ResourceContextInfo()
                        .setResourceId(String.valueOf(project.getId()))
                        .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
        );
    }
}
