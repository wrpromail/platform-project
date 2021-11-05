package net.coding.app.project.grpc.openapi;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ProjectLabelEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;

import org.apache.commons.collections.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.open.api.CodeProto;
import proto.open.api.ResultProto;
import proto.open.api.project.ProjectMemberProto;
import proto.open.api.project.ProjectMemberServiceGrpc;
import proto.platform.permission.PermissionProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.common.constants.RoleConstants.MEMBER;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;
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
public class OpenApiProjectMemberGRpcService extends ProjectMemberServiceGrpc.ProjectMemberServiceImplBase {

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final TeamGrpcClient teamGrpcClient;

    private final UserGrpcClient userGrpcClient;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    private final LocaleMessageSource localeMessageSource;

    @Override
    public void createProjectMember(
            ProjectMemberProto.CreateProjectMemberRequest request,
            StreamObserver<ResultProto.CommonResult> responseObserver) {
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (Objects.isNull(currentUser)) {
                throw CoreException.of(USER_NOT_EXISTS);
            }

            Project project = projectService.getById(request.getProjectId());
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
            if (CollectionUtils.isEmpty(targetUserIds)) {
                CommonResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase());
                return;
            }
            projectMemberService.doAddMember(currentUser.getId(),
                    targetUserIds,
                    (short) request.getType(),
                    project,
                    false);
            CommonResponse(responseObserver, SUCCESS, SUCCESS.name().toLowerCase());
        } catch (CoreException e) {
            CommonResponse(responseObserver, NOT_FOUND,
                    localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService createProjectMember error Exception ", e);
            CommonResponse(responseObserver, INVALID_PARAMETER,
                    INVALID_PARAMETER.name().toLowerCase());
        }
    }

    private void CommonResponse(
            StreamObserver<ResultProto.CommonResult> responseObserver,
            CodeProto.Code code,
            String message) {
        ResultProto.Result result = ResultProto.Result.newBuilder()
                .setCode(code.getNumber())
                .setId(0)
                .setMessage(message)
                .build();
        responseObserver.onNext(ResultProto.CommonResult.newBuilder()
                .setResult(result).build());
        responseObserver.onCompleted();
    }
}
