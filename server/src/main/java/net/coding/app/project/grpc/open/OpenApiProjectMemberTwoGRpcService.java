package net.coding.app.project.grpc.open;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.e.proto.ApiUserProto;
import net.coding.e.proto.CommonProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.UserDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.proto.open.api.project.member.ProjectMemberProto;
import net.coding.proto.open.api.project.member.ProjectMemberServiceGrpc;

import org.apache.commons.lang3.ObjectUtils;
import org.lognet.springboot.grpc.GRpcService;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.open.api.CodeProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static proto.open.api.CodeProto.Code.INVALID_PARAMETER;
import static proto.open.api.CodeProto.Code.NOT_FOUND;
import static proto.open.api.CodeProto.Code.SUCCESS;

/**
 * @Description: OPEN API 项目相关接口，非 OPEN API 业务 勿修改
 * @Author liheng
 * @Date 2021/1/4 4:16 下午
 */
@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiProjectMemberTwoGRpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectMemberService projectMemberService;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    @Override
    public void describeProjectMembers(
            ProjectMemberProto.DescribeProjectMembersRequest request,
            StreamObserver<ProjectMemberProto.DescribeProjectMembersResponse> responseObserver) {
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
            ResultPage<ProjectMemberDTO> resultPage = projectMemberService.getProjectMembers(
                    currentUser.getTeamId(),
                    request.getProjectId(),
                    EMPTY,
                    request.getRoleId(),
                    pager
            );
            describeProjectMembersResponse(responseObserver, SUCCESS,
                    SUCCESS.name().toLowerCase(), resultPage);
        } catch (CoreException e) {
            log.error("RpcService describeProjectMembers error CoreException ", e);
            describeProjectMembersResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()), null);
        } catch (Exception e) {
            log.error("rpcService describeProjectMembers error Exception ", e);
            describeProjectMembersResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase(), null);
        }
    }

    private void describeProjectMembersResponse(
            StreamObserver<ProjectMemberProto.DescribeProjectMembersResponse> responseObserver,
            CodeProto.Code code,
            String message,
            ResultPage<ProjectMemberDTO> resultPage) {
        CommonProto.Result result = CommonProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        ProjectMemberProto.DescribeProjectMembersResponse.Builder builder =
                ProjectMemberProto.DescribeProjectMembersResponse.newBuilder()
                        .setResult(result);
        if (Objects.nonNull(resultPage)) {
            List<ApiUserProto.UserData> projectMembers = Optional.ofNullable(resultPage.getList())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(this::complexProjectMemberToProto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            builder.setData(ProjectMemberProto.ProjectMemberData.newBuilder()
                    .setPageSize(resultPage.getPageSize())
                    .setPageNumber(resultPage.getPage())
                    .setTotalCount(resultPage.getTotalRow())
                    .addAllProjectMembers(projectMembers)
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
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
