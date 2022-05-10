package net.coding.app.project.grpc.openapi;

import com.google.common.collect.ImmutableSet;

import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.grpc.client.platform.qcloud.QCloudUserGrpcClient;
import net.coding.lib.project.dto.ProjectTokenArtifactDTO;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectToken;
import net.coding.lib.project.enums.QCloudProduct;
import net.coding.lib.project.exception.AppException;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.GlobalKeyCreateErrorException;
import net.coding.lib.project.form.AddProjectTokenForm;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectTokenService;
import net.coding.proto.open.api.project.token.ProjectTokenProto;
import net.coding.proto.open.api.project.token.ProjectTokenServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;

import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.open.api.CodeProto;
import proto.platform.globalKey.GlobalKeyProto;
import proto.platform.permission.PermissionProto;
import proto.platform.qCloudUser.QCloudUserProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static proto.open.api.CodeProto.Code.INVALID_PARAMETER;
import static proto.open.api.CodeProto.Code.NOT_FOUND;
import static proto.open.api.CodeProto.Code.SUCCESS;

@Slf4j
@AllArgsConstructor
@GRpcService
public class OpenApiProjectTokenGRpcService extends ProjectTokenServiceGrpc.ProjectTokenServiceImplBase {
    public final static Short ACCOUNT_PRIMARY = 0;
    public static final short TYPE_USER = 0;        // 用户生成
    public static final short TYPE_SYSTEM_CI = 1;      // 系统生成，用于 CI，对用户不可见
    public static final String QCLOUD_API_TOKEN_NAME = "云 API 令牌";

    private final static Set<String> GLOBAL_SCOPE = ImmutableSet.of(
            DeployTokenScopeEnum.PROJECT_ISSUE_RW.getValue(),
            DeployTokenScopeEnum.FILE_RW.getValue(),
            DeployTokenScopeEnum.PROJECT_TWEET_RW.getValue(),
            DeployTokenScopeEnum.RESOURCE_REFERENCE_RW.getValue(),
            DeployTokenScopeEnum.PROJECT_MEMBER_RW.getValue(),
            DeployTokenScopeEnum.PROJECT_PERMISSION_RW.getValue(),
            DeployTokenScopeEnum.CI_AGENT_REGISTER.getValue(),
            DeployTokenScopeEnum.CI_TRIGGER_JOB.getValue(),
            DeployTokenScopeEnum.OPEN_CI_MANAGER.getValue(),
            DeployTokenScopeEnum.WIKI_RW.getValue(),
            DeployTokenScopeEnum.API_DOC_RELEASE.getValue()
    );
    private final static Set<String> DEPOT_SCOPE = ImmutableSet.of(
            DeployTokenScopeEnum.DEPOT_READ.getValue(),
            DeployTokenScopeEnum.DEPOT_WRITE.getValue(),
            DeployTokenScopeEnum.MERGE_REQUEST_RW.getValue(),
            DeployTokenScopeEnum.RELEASE_RW.getValue()
    );
    private final static Set<String> ARTIFACT_SCOPE = ImmutableSet.of(
            DeployTokenScopeEnum.ARTIFACT_R.getValue(),
            DeployTokenScopeEnum.ARTIFACT_RW.getValue(),
            DeployTokenScopeEnum.ARTIFACT_VERSION_PROPS_RW.getValue()
    );
    private final ProjectService projectService;
    private final ProjectTokenService projectTokenService;
    private final UserGrpcClient userGrpcClient;
    private final QCloudUserGrpcClient qCloudUserGrpcClient;
    private final AclServiceGrpcClient aclServiceGrpcClient;
    private final GlobalKeyGrpcClient globalKeyGrpcClient;
    private final LocaleMessageSource localeMessageSource;
    private final ProjectMemberService projectMemberService;

    @Override
    public void deleteProjectToken(
            ProjectTokenProto.DeleteProjectTokenRequest request,
            StreamObserver<ProjectTokenProto.DeleteProjectTokenResponse> responseObserver
    ) {
        ProjectTokenProto.DeleteProjectTokenResponse.Builder builder = ProjectTokenProto.DeleteProjectTokenResponse.newBuilder();
        CommonProto.Result.Builder result = CommonProto.Result.newBuilder()
                .setCode(SUCCESS.getNumber())
                .setMessage(SUCCESS.name().toLowerCase());
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (currentUser == null) {
                log.warn("creator not exist userId {}", request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            Project project = projectService.getByIdAndTeamId(request.getProjectId(), request.getUser().getTeamId());
            if (project == null) {
                log.warn("Project not exist projectId {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(project.getId(), currentUser.getId());
            if (projectMember == null) {
                log.warn("Project member not exist projectId {} userId {}", request.getProjectId(), request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
            }
            if (!aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectDeployToken)
                            .setAction(PermissionProto.Action.Delete)
                            .build(),
                    project.getId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            )) {
                throw CoreException.of(PERMISSION_DENIED);
            }

            if (!projectTokenService
                    .deleteProjectToken(request.getProjectId(), request.getId())) {
                log.warn("Project token not exist {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_NOT_EXIST);
            }
            builder.setDeleted(true);
        } catch (CoreException e) {
            log.warn("rpcService deleteProjectToken error CoreException {}", e.getKey());
            result.setCode(NOT_FOUND.getNumber()).setMessage(
                    localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService deleteProjectToken error Exception ", e);
            result.setCode(INVALID_PARAMETER.getNumber()).setMessage(
                    INVALID_PARAMETER.name().toLowerCase());
        } finally {
            builder.setResult(result.build());
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void describeProjectTokens(
            ProjectTokenProto.DescribeProjectTokensRequest request,
            StreamObserver<ProjectTokenProto.DescribeProjectTokensResponse> responseObserver
    ) {
        ProjectTokenProto.DescribeProjectTokensResponse.Builder builder = ProjectTokenProto.DescribeProjectTokensResponse.newBuilder();

        CommonProto.Result.Builder result = CommonProto.Result.newBuilder()
                .setCode(SUCCESS.getNumber())
                .setMessage(SUCCESS.name().toLowerCase());

        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (currentUser == null) {
                log.warn("creator not exist userId {}", request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            Project project = projectService.getByIdAndTeamId(request.getProjectId(), request.getUser().getTeamId());
            if (project == null) {
                log.warn("Project not exist projectId {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(project.getId(), currentUser.getId());
            if (projectMember == null) {
                log.warn("Project member not exist projectId {} userId {}", request.getProjectId(), request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
            }
            if (!aclServiceGrpcClient.hasPermissionInProject(
                    PermissionProto.Permission.newBuilder()
                            .setFunction(PermissionProto.Function.ProjectDeployToken)
                            .setAction(PermissionProto.Action.View)
                            .build(),
                    project.getId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            )) {
                throw CoreException.of(PERMISSION_DENIED);
            }

            LimitedPager pager = new LimitedPager(request.getPageNumber(), request.getPageSize());
            ResultPage<ProjectToken> proTokenPages = projectTokenService
                    .getProjectTokenPages(request.getProjectId(), pager);
            handlePages2Response(builder, proTokenPages);

        } catch (CoreException e) {
            log.warn("rpcService describeProjectTokens error CoreException {}", e.getKey());
            result.setCode(NOT_FOUND.getNumber()).setMessage(
                    localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService describeProjectTokens error Exception ", e);
            result.setCode(INVALID_PARAMETER.getNumber()).setMessage(
                    INVALID_PARAMETER.name().toLowerCase());
        } finally {
            builder.setResult(result.build());
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createProjectToken(
            ProjectTokenProto.CreateProjectTokenRequest request,
            StreamObserver<ProjectTokenProto.CreateProjectTokenResponse> responseObserver
    ) {
        ProjectTokenProto.CreateProjectTokenResponse.Builder builder = ProjectTokenProto.CreateProjectTokenResponse.newBuilder();
        CommonProto.Result.Builder result = CommonProto.Result.newBuilder()
                .setCode(SUCCESS.getNumber())
                .setMessage(SUCCESS.name().toLowerCase());
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (currentUser == null) {
                log.warn("creator not exist userId {}", request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            Project project = projectService.getByIdAndTeamId(request.getProjectId(), request.getUser().getTeamId());
            if (project == null) {
                log.warn("Project not exist projectId {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(project.getId(), currentUser.getId());
            if (projectMember == null) {
                log.warn("Project member not exist projectId {} userId {}", request.getProjectId(), request.getUser().getId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
            }
            if (QCloudProduct.TCB.name().equals(project.getLabel())) {
                currentUser = getQCloudOwnerUser(currentUser);
            }
            if (!aclServiceGrpcClient.hasPermissionInProject(PermissionProto.Permission.newBuilder()
                            .setAction(PermissionProto.Action.Create)
                            .setFunction(PermissionProto.Function.ProjectDeployToken)
                            .build(),
                    project.getId(),
                    currentUser.getGlobalKey(),
                    currentUser.getId()
            )) {
                throw CoreException.of(PERMISSION_DENIED);
            }
            ProjectToken projectToken = projectTokenService.saveProjectToken(
                    request.getProjectId(),
                    currentUser,
                    handleRequestForm(request),
                    null,
                    request.getScopesList().isEmpty() ? TYPE_SYSTEM_CI : TYPE_USER
            );
            GlobalKeyProto.GetByIdResponse response = globalKeyGrpcClient.getById(
                    GlobalKeyProto.GetByIdRequest.newBuilder()
                            .setId(projectToken.getGlobalKeyId())
                            .build()
            );
            if (response == null) {
                throw new GlobalKeyCreateErrorException();
            }
            builder.setData(
                    ProjectTokenProto.ProjectToken.newBuilder()
                            .setId(projectToken.getId())
                            .setCreatedAt(projectToken.getCreatedAt().getTime())
                            .setCreatorId(projectToken.getCreatorId())
                            .setEnabled(projectToken.getEnabled())
                            .setProjectId(projectToken.getProjectId())
                            .setExpireAt(projectToken.getExpiredAt().getTime())
                            .setTokenName(projectToken.getTokenName())
                            .setUserName(response.getData().getGlobalKey())
                            .setLastActivityAt(projectToken.getLastActivityAt().getTime())
                            .setToken(projectToken.getToken())
                            .setGlobalKey(response.getData().getGlobalKey())
                            .setUpdatedAt(projectToken.getUpdatedAt().getTime())
                            .addAllScopes(request.getScopesList())
                            .build()
            ).setResult(CommonProto.Result.newBuilder()
                    .setCode(CodeProto.Code.SUCCESS.getNumber())
                    .setMessage(SUCCESS.name().toLowerCase())
                    .build());
        } catch (CoreException e) {
            log.warn("rpcService createProjectToken error CoreException {}", e.getKey());
            result.setCode(NOT_FOUND.getNumber())
                    .setMessage(localeMessageSource.getMessage(e.getKey()));
        } catch (AppException e) {
            log.warn("rpcService createProjectToken error AppException {}", e.getKey());
            result.setCode(NOT_FOUND.getNumber())
                    .setMessage(localeMessageSource.getMessage(e.getKey()));
        } catch (Exception e) {
            log.error("rpcService createProjectToken error Exception ", e);
            result.setCode(INVALID_PARAMETER.getNumber()).setMessage(
                    INVALID_PARAMETER.name().toLowerCase());
        } finally {
            builder.setResult(result.build());
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private void handlePages2Response(ProjectTokenProto.DescribeProjectTokensResponse.Builder builder, ResultPage<ProjectToken> proTokenPages) {
        ProjectTokenProto.DescribeProjectTokenData.Builder builderData = ProjectTokenProto.DescribeProjectTokenData.newBuilder()
                .setPageNumber(proTokenPages.getPage())
                .setTotalCount(proTokenPages.getTotalRow())
                .setPageSize(proTokenPages.getPageSize());


        proTokenPages.getList().stream()
                .map(dt -> projectTokenService.toProjectTokenDTO(dt, false))
                .forEach(dto -> {
                            ProjectTokenProto.ProjectToken.Builder projectTokenBuilder = ProjectTokenProto.ProjectToken.newBuilder();
                            dto.getScopes().
                                    forEach(scope ->
                                            projectTokenBuilder.addScopes(ProjectTokenProto.Scope.newBuilder().
                                                    setText(scope.getText()).
                                                    setValue(scope.getValue()).
                                                    build())
                                    );

                            dto.getDepotScopes()
                                    .forEach(depotScopeDTO -> depotScopeDTO.getScopes()
                                            .forEach(d -> projectTokenBuilder.addScopes(ProjectTokenProto.Scope.newBuilder().
                                                    setValue(d.getValue()).
                                                    setText(d.getText()).
                                                    setTarget(depotScopeDTO.getId())
                                                    .build()))
                                    );

                            dto.getArtifactScopes()
                                    .forEach(artifactScopeDTO -> artifactScopeDTO.getScopes()
                                            .forEach(a -> projectTokenBuilder.addScopes(ProjectTokenProto.Scope.newBuilder().
                                                    setValue(a.getValue()).
                                                    setText(a.getText()).
                                                    setTarget(artifactScopeDTO.getId())
                                                    .build()))
                                    );

                            projectTokenBuilder
                                    .setId(dto.getId())
                                    .setProjectId(dto.getProjectId())
                                    .setCreatorId(dto.getCreatorId())
                                    .setTokenName(dto.getTokenName())
                                    .setUserName(dto.getUserName())
                                    .setExpireAt(dto.getExpiredAt().getTime())
                                    .setCreatedAt(dto.getCreatedAt())
                                    .setLastActivityAt(dto.getLastActivityAt())
                                    .setEnabled(dto.isEnabled())
                                    .setUpdatedAt(dto.getUpdatedAt());
                            builderData.addProjectToken(projectTokenBuilder.build());
                        }

                );
        builder.setData(builderData.build());
    }

    public UserProto.User getQCloudOwnerUser(UserProto.User user) {
        Optional<QCloudUserProto.GetUserResponse> response = Optional.ofNullable(qCloudUserGrpcClient.GetQCloudUser(user.getId()));
        if (response.isPresent()) {
            QCloudUserProto.QCloudUser qCloudUser = response.get().getData();
            // 如果是主账号
            if (qCloudUser.getAccountType() == ACCOUNT_PRIMARY) {
                return user;
            } else {
                // 如果是子账号
                Optional<QCloudUserProto.GetUserResponse> userResponse = Optional.
                        ofNullable(qCloudUserGrpcClient.
                                GetByOwnerUinAndUin(qCloudUser.getOwnerUin(), qCloudUser.getOwnerUin()));
                if (userResponse.isPresent()) {
                    return userGrpcClient.getUserById(userResponse.get().getData().getUserId());
                }
            }
        }
        return user;
    }

    private AddProjectTokenForm handleRequestForm(ProjectTokenProto.CreateProjectTokenRequest request) {

        List<String> tokenScopes = request.getScopesList().stream()
                .filter(s -> (s.getTarget() <= 0) || (GLOBAL_SCOPE.contains(s.getValue())))
                .map(ProjectTokenProto.Scope::getValue)
                .collect(Collectors.toList());

        // 处理仓库scope拼接
        List<ProjectTokenDepotDTO> depotScopes = request.getScopesList().stream()
                .filter(s -> (s.getTarget() > 0 && DEPOT_SCOPE.contains(s.getValue())))
                .collect(Collectors.groupingBy(
                        ProjectTokenProto.Scope::getTarget, Collectors.mapping(
                                ProjectTokenProto.Scope::getValue, Collectors.joining(","))))
                .entrySet()
                .stream()
                .map(entry -> ProjectTokenDepotDTO.builder()
                        .depotId(String.valueOf(entry.getKey()))
                        .scope(entry.getValue())
                        .build())
                .collect(Collectors.toList());


        // 处理制品库scope拼接
        List<ProjectTokenArtifactDTO> artifactScopes = request.getScopesList().stream()
                .filter(s -> (s.getTarget() > 0 && ARTIFACT_SCOPE.contains(s.getValue())))
                .collect(Collectors.groupingBy(
                        ProjectTokenProto.Scope::getTarget, Collectors.mapping(
                                ProjectTokenProto.Scope::getValue, Collectors.joining(","))))
                .entrySet()
                .stream()
                .map(entry -> ProjectTokenArtifactDTO.builder()
                        .artifactId(entry.getKey())
                        .scope(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        AddProjectTokenForm.AddProjectTokenFormBuilder builder = AddProjectTokenForm.builder();

        if (request.getScopesList().isEmpty()) {
            final String scope = Arrays.stream(DeployTokenScopeEnum.values())
                    .map(DeployTokenScopeEnum::getValue)
                    .collect(Collectors.joining(","));
            builder.scope(scope)
                    .tokenName(QCLOUD_API_TOKEN_NAME);
            return builder.build();
        }

        return builder.tokenName(request.getTokenName())
                .depotScopes(depotScopes)
                .artifactScopes(artifactScopes)
                .expiredAt(request.getExpiredAt())
                .scope(StringUtils.collectionToCommaDelimitedString(tokenScopes))
                .expiredAt(request.getExpiredAt())
                .applyToAllDepots(depotScopes.isEmpty())
                .applyToAllArtifacts(artifactScopes.isEmpty())
                .build();

    }
}
