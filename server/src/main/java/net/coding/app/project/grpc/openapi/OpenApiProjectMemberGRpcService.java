package net.coding.app.project.grpc.openapi;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.e.proto.ApiCodeProto;
import net.coding.e.proto.ApiUserProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.UserDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.member.ProjectMemberPrincipalService;
import net.coding.proto.open.api.project.member.ProjectMemberProto;
import net.coding.proto.open.api.project.member.ProjectMemberServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.common.constants.RoleConstants.MEMBER;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * @Description: OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 * @Author liheng
 * @Date 2021/1/4 4:16 下午
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProjectMemberGRpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final ProjectMemberPrincipalService projectMemberPrincipalService;

    private final TeamGrpcClient teamGrpcClient;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    @Override
    public void describeProjectMembers(
            ProjectMemberProto.DescribeProjectMembersRequest request,
            StreamObserver<ProjectMemberProto.DescribeProjectMembersResponse> responseObserver) {
        ProjectMemberProto.DescribeProjectMembersResponse.Builder builder =
                ProjectMemberProto.DescribeProjectMembersResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectMember)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    request.getProjectId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            );
            if (!hasPermissionInProject) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            int offset = Math.max((request.getPageNumber() - 1) * request.getPageSize(), 0);
            PageRowBounds pager = new PageRowBounds(offset, request.getPageSize());
            ResultPage<ProjectMemberDTO> resultPage = projectMemberPrincipalService.getProjectMembers(
                    currentUser.getTeamId(),
                    request.getProjectId(),
                    EMPTY,
                    request.getRoleId(),
                    pager
            );
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            )
                    .setData(describeProjectMemberPagesToProto(resultPage));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService describeProjectMembers error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.INVALID_PARAMETER.getNumber())
                            .setMessage(ApiCodeProto.Code.INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createProjectMember(
            ProjectMemberProto.CreateProjectMemberRequest request,
            StreamObserver<CommonProto.CommonResult> responseObserver) {
        CommonProto.CommonResult.Builder builder =
                CommonProto.CommonResult.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (Objects.isNull(currentUser)) {
                throw CoreException.of(USER_NOT_EXISTS);
            }
            Project project = projectService.getByIdAndTeamId(
                    request.getProjectId(),
                    request.getUser().getTeamId()
            );
            if (Objects.isNull(project)) {
                throw CoreException.of(PROJECT_NOT_EXIST);
            }
            //SLS 邀请项目成员 子账号会默认采用项目所有者身份
            if (ProjectLabelEnums.SLS.name().equals(project.getLabel())) {
                TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(project.getTeamOwnerId());
                if (Objects.isNull(response) || Objects.isNull(response.getData())) {
                    throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
                }
                currentUser = userGrpcClient.getUserById(response.getData().getOwner().getId());
            }
            boolean hasPermissionInEnterprise = aclServiceGrpcClient.hasPermissionInEnterprise(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.EnterpriseProject)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    currentUser.getId(),
                    currentUser.getTeamId()
            );
            if (!hasPermissionInEnterprise) {
                //验证用户接口权限
                boolean hasPermissionInProject = aclServiceGrpcClient.hasPermissionInProject(
                        PermissionProto.Permission.newBuilder()
                                .setFunction(PermissionProto.Function.ProjectMember)
                                .setAction(PermissionProto.Action.Create)
                                .build(),
                        request.getProjectId(),
                        currentUser.getGlobalKey(),
                        currentUser.getId()
                );
                if (!hasPermissionInProject) {
                    throw CoreException.of(PERMISSION_DENIED);
                }
            }
            if ((request.getType() != ADMIN && request.getType() != MEMBER)) {
                throw CoreException.of(PERMISSION_DENIED);
            }

            List<String> userGKList = request.getUserGlobalKeyListList();
            if (CollectionUtils.isEmpty(userGKList)) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            List<Integer> targetUserIds = new ArrayList<>();
            userGKList.forEach(targetUserIdStr -> {
                UserProto.User user = userGrpcClient.getUserByGlobalKey(targetUserIdStr);
                if (Objects.nonNull(user)) {
                    targetUserIds.add(user.getId());
                }
            });
            if (CollectionUtils.isNotEmpty(targetUserIds)) {
                projectMemberService.doAddMember(currentUser.getId(),
                        targetUserIds,
                        (short) request.getType(),
                        project,
                        false);
            }
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.SUCCESS.getNumber())
                            .build()
            );
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService createProjectMember error Exception ", e);
            builder.setResult(
                    resultBuilder
                            .setCode(ApiCodeProto.Code.INVALID_PARAMETER.getNumber())
                            .setMessage(ApiCodeProto.Code.INVALID_PARAMETER.name().toLowerCase())
                            .build()
            );
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private ProjectMemberProto.ProjectMemberData describeProjectMemberPagesToProto(
            ResultPage<ProjectMemberDTO> resultPage) {
        List<ApiUserProto.UserData> projectMembers = Optional.ofNullable(resultPage.getList())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::complexProjectMemberToProto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ProjectMemberProto.ProjectMemberData.newBuilder()
                .setPageSize(resultPage.getPageSize())
                .setPageNumber(resultPage.getPage())
                .setTotalCount(resultPage.getTotalRow())
                .addAllProjectMembers(projectMembers)
                .build();
    }

    public ApiUserProto.UserData complexProjectMemberToProto(ProjectMemberDTO projectMemberDTO) {
        if (Objects.isNull(projectMemberDTO)) {
            return null;
        }
        UserDTO user = projectMemberDTO.getUser();
        List<ApiUserProto.Role> roles = StreamEx.of(projectMemberDTO.getRoles())
                .map(roleDTO -> ApiUserProto.Role.newBuilder()
                        .setRoleId(roleDTO.getRoleId())
                        .setRoleType(roleDTO.getRoleType())
                        .setRoleTypeName(roleDTO.getName())
                        .build()
                )
                .collect(Collectors.toList());
        return ApiUserProto.UserData.newBuilder()
                .setId(user.getId())
                .setTeamId(user.getTeamId())
                .setAvatar(user.getAvatar())
                .setName(TextUtils.htmlEscape(user.getName()))
                .setNamePinYin(user.getName_pinyin())
                .setStatus(user.getStatus())
                .setEmail(defaultString(user.getEmail()))
                .setEmailValidation(ObjectUtils.defaultIfNull(user.getEmail_validation(), 0))
                .setPhone(TextUtils.htmlEscape(user.getPhone()))
                .setPhoneValidation(ObjectUtils.defaultIfNull(user.getPhone_validation(), 0))
                .setGlobalKey(user.getGlobal_key())
                .addAllRoles(roles)
                .build();
    }
}
