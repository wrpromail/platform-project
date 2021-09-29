package net.coding.app.project.grpc;


import net.coding.exchange.dto.team.Team;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;
import net.coding.lib.project.dto.ProjectTokenKeyDTO;
import net.coding.lib.project.entity.ProjectTokenArtifact;
import net.coding.lib.project.entity.ProjectToken;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectTokenDepot;
import net.coding.lib.project.exception.AppException;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddProjectTokenForm;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectTokenArtifactService;
import net.coding.lib.project.service.ProjectTokenDepotService;
import net.coding.lib.project.service.ProjectTokenService;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.utils.DesensitizationUtil;
import net.coding.proto.platform.project.ProjectDeployTokenProto;
import net.coding.proto.platform.project.ProjectDeployTokenServiceGrpc;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;


import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.globalKey.GlobalKeyProto;
import proto.platform.user.UserProto;

import static net.coding.common.util.DateTimeUtils.toTimestamp;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static proto.common.CodeProto.Code.INTERNAL_ERROR;
import static proto.common.CodeProto.Code.NOT_FOUND;
import static proto.common.CodeProto.Code.SUCCESS;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectDeployTokenGrpcService extends ProjectDeployTokenServiceGrpc.ProjectDeployTokenServiceImplBase {

    private final ProjectService projectService;
    private final ProjectTokenService projectTokenService;
    private final GlobalKeyGrpcClient globalKeyGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final ProjectMemberService projectMemberService;
    private final TeamGrpcClient teamGrpcClient;
    private final TeamServiceGrpcClient teamServiceGrpcClient;
    private final ProjectTokenArtifactService projectTokenArtifactService;
    private final ProjectTokenDepotService projectTokenDepotService;


    public void addDeployToken(
            ProjectDeployTokenProto.AddDeployTokenRequest request,
            StreamObserver<ProjectDeployTokenProto.AddDeployTokenResponse> responseObserver
    ) {
        ProjectDeployTokenProto.AddDeployTokenResponse.Builder builder = ProjectDeployTokenProto.AddDeployTokenResponse.newBuilder();
        try {
            ProjectDeployTokenProto.DeployTokenType type = ProjectDeployTokenProto.DeployTokenType.forNumber(request.getDeployType());
            if (ObjectUtils.isEmpty(type)) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                log.warn("Project not exist projectId {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            UserProto.User user = getUser(request.getUserId());
            if (user == null) {
                log.warn("creator not exist userId {}", request.getUserId());
                throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
            }
            UserProto.User associated;
            if (request.getAssociatedId() > 0) {
                associated = getUser(request.getAssociatedId());
                if (associated == null) {
                    log.warn("associated not exist associatedId {}", request.getAssociatedId());
                    throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
                }
                boolean isMember = projectMemberService.isMember(associated, request.getProjectId());
                if (!isMember) {
                    log.warn("Member not exist userId {},projectId {}", request.getAssociatedId(), request.getProjectId());
                    throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
                }
            }
            ProjectToken projectToken = projectTokenService.saveProjectToken(
                    request.getProjectId(),
                    user,
                    fromProto(request.getAddDeployTokenForm()),
                    request.getAssociatedId(),
                    (short) request.getDeployType()
            );
            builder.setCode(CodeProto.Code.SUCCESS_VALUE)
                    .setData(toDeployToken(projectToken));
        } catch (Exception e) {
            log.error("RpcService addDeployToken is error {}", e.getMessage());
            responseObserver.onError(Status.INTERNAL
                    .withCause(e)
                    .withDescription(StringUtils.defaultString(e.getMessage()))
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDeployTokenById(
            ProjectDeployTokenProto.GetDeployTokenByIdRequest request,
            StreamObserver<ProjectDeployTokenProto.GetDeployTokenByIdResponse> responseObserver
    ) {
        ProjectDeployTokenProto.GetDeployTokenByIdResponse.Builder builder = ProjectDeployTokenProto.GetDeployTokenByIdResponse.newBuilder();
        try {
            ProjectToken projectToken = projectTokenService.getProjectToken(request.getId());
            if (projectToken == null) {
                log.warn("token not exist id {}", request.getId());
                builder.setCode(CodeProto.Code.NOT_FOUND);
                builder.setMessage("not found");
            } else {
                builder.setCode(CodeProto.Code.SUCCESS)
                        .setData(toDeployToken(projectToken));
            }
        } catch (Exception e) {
            log.error("RpcService getDeployTokenById error CoreException {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteDeployToken(ProjectDeployTokenProto.DeleteDeployTokenRequest request, StreamObserver<ProjectDeployTokenProto.DeleteDeployTokenResponse> responseObserver) {
        ProjectDeployTokenProto.DeleteDeployTokenResponse.Builder builder = ProjectDeployTokenProto.DeleteDeployTokenResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                log.warn("Project not exist projectId {}", request.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            boolean result = projectTokenService.deleteProjectToken(request.getProjectId(), request.getDeployTokenId());
            builder.setCode(CodeProto.Code.SUCCESS_VALUE)
                    .setResult(result);
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withCause(e)
                    .withDescription(StringUtils.defaultString(e.getMessage()))
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * 根据 deploy token 获取项目 ID
     */
    @Override
    public void getProjectByToken(ProjectDeployTokenProto.GetProjectByTokenRequest request, StreamObserver<ProjectDeployTokenProto.GetProjectByTokenResponse> responseObserver) {
        ProjectDeployTokenProto.GetProjectByTokenResponse.Builder builder = ProjectDeployTokenProto.GetProjectByTokenResponse.newBuilder();
        try {
            ProjectToken projectToken = projectTokenService.getProjectToken(request.getDeployToken());
            if (projectToken == null) {
                log.warn("Token not exist  {}", request.getDeployToken());
                throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_NOT_EXIST);
            }
            Project project = projectService.getById(projectToken.getProjectId());
            if (project == null) {
                log.warn("Project not exist userId {}", projectToken.getProjectId());
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            String teamHost = teamGrpcClient.getTeamHostWithProtocolByTeamId(project.getTeamOwnerId());
            builder.setCode(CodeProto.Code.SUCCESS)
                    .setProjectId(project.getId())
                    .setProjectName(StringUtils.isNotEmpty(project.getName()) ? project.getName() : StringUtils.EMPTY)
                    .setTeamHost(StringUtils.isNotEmpty(teamHost) ? teamHost : StringUtils.EMPTY);
        } catch (Exception e) {
            log.error("RpcService getProjectByToken error CoreException {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkPermission(ProjectDeployTokenProto.CheckPermissionRequest request,
                                StreamObserver<ProjectDeployTokenProto.CheckPermissionResponse> responseObserver) {
        ProjectDeployTokenProto.CheckPermissionResponse.Builder builder = ProjectDeployTokenProto.CheckPermissionResponse.newBuilder();
        try {
            boolean result = projectTokenService.checkPermission(request.getTokenId(),
                    request.getToken(),
                    request.getProjectId(),
                    new HashSet<>(request.getSupportScopeList()));
            builder.setCode(CodeProto.Code.SUCCESS_VALUE)
                    .setResult(result);
        } catch (AppException ae) {
            builder.setCode(ae.getCode())
                    .setErrKey(ae.getKey());
        } catch (Exception e) {
            log.error("DeployTokenGrpcService.checkPermission error:{}", e.getMessage());
            responseObserver.onNext(builder
                    .setCode(CodeProto.Code.INTERNAL_ERROR_VALUE)
                    .setErrKey(e.getMessage())
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * 验证指定 deploy token 是否有 ci agent 执行权限
     */
    @Override
    public void checkCiAgentPermission(ProjectDeployTokenProto.CheckCiAgentPermissionRequest request,
                                       StreamObserver<ProjectDeployTokenProto.CheckCiAgentPermissionResponse> responseObserver) {
        ProjectDeployTokenProto.CheckCiAgentPermissionResponse.Builder builder = ProjectDeployTokenProto.CheckCiAgentPermissionResponse.newBuilder();

        try {
            boolean hasTokenPermission = projectTokenService.checkCiAgentPermission(request.getDeployToken());
            builder.setCode(CodeProto.Code.SUCCESS)
                    .setHasTokenPermission(hasTokenPermission);
        } catch (Exception e) {
            log.error("RpcService checkCiAgentPermission is error {}", e.getMessage());
            builder.setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDeployTokenByTokenAndTeamGKAndProjectName(ProjectDeployTokenProto.DeployTokenByTokenAndTeamGKAndProjectNameRequest request,
                                                             StreamObserver<ProjectDeployTokenProto.DeployTokenResponse> responseObserver) {
        ProjectDeployTokenProto.DeployTokenResponse.Builder newBuilder = ProjectDeployTokenProto.DeployTokenResponse.newBuilder();
        try {
            Team team = teamServiceGrpcClient.getTeamByGlobalKey(request.getTeamGk());
            if (team == null) {
                throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
            }
            Project project = projectService.getByNameAndTeamId(request.getProjectName(), team.getId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectToken projectToken = projectTokenService.getTokenByTokenAndProjectId(request.getToken(), project.getId());
            if (projectToken == null) {
                newBuilder.setCode(NOT_FOUND)
                        .setMessage("token is not found");
            } else {
                newBuilder.setCode(SUCCESS)
                        .setData(toDeployToken(projectToken));
            }
        } catch (Exception e) {
            log.error("RpcService getDeployTokenByTokenAndTeamGKAndProjectName error {}", e.getMessage());
            newBuilder.setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(newBuilder.build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 根据 token和globalKey 获取 令牌
     */
    @Override
    public void getDeployTokenByTokenAndGlobalKey(ProjectDeployTokenProto.GetDeployTokenByTokenAndGlobalKeyRequest request,
                                                  StreamObserver<ProjectDeployTokenProto.DeployTokenResponse> responseObserver) {
        ProjectDeployTokenProto.DeployTokenResponse.Builder newBuilder = ProjectDeployTokenProto.DeployTokenResponse.newBuilder();
        try {
            Integer globalKeyId = getGlobalKey(request.getTeamGlobalKey());
            if (globalKeyId == null) {
                throw CoreException.of(CoreException.ExceptionType.GLOBAL_KEY_INVALID);
            }
            ProjectToken token = projectTokenService.getByTokenAndGlobalKeyId(request.getToken(), globalKeyId);
            if (token == null) {
                newBuilder.setCode(NOT_FOUND);
                newBuilder.setMessage("token is not found");
            } else {
                newBuilder.setCode(SUCCESS)
                        .setData(toDeployToken(token));
            }
        } catch (Exception e) {
            log.error("RpcService.getDeployTokenByTokenAndGlobalKey token {}, globalKey {}, error {}",
                    DesensitizationUtil.left(request.getToken(), 10),
                    request.getTeamGlobalKey(),
                    e.getMessage()
            );
            newBuilder.setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(newBuilder.build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 根据项目 获取 TYPE_USER 令牌
     */
    @Override
    public void getUserDeployTokensByProjectId(ProjectDeployTokenProto.GetUserDeployTokensByProjectIdRequest request,
                                               StreamObserver<ProjectDeployTokenProto.GetUserDeployTokensByProjectIdResponse> responseObserver) {

        ProjectDeployTokenProto.GetUserDeployTokensByProjectIdResponse.Builder newBuilder = ProjectDeployTokenProto
                .GetUserDeployTokensByProjectIdResponse
                .newBuilder();
        try {
            List<ProjectToken> tokens = projectTokenService.selectUserProjectToken(request.getProjectId());
            if (CollectionUtils.isNotEmpty(tokens)) {
                newBuilder.addAllTokens(tokens.stream()
                        .map(this::toDeployToken)
                        .collect(Collectors.toList()))
                        .setCode(SUCCESS);
            } else {
                newBuilder.setCode(NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("DeployTokenGrpcService.getUserDeployTokensByProjectId error:{}", e.getMessage());
            newBuilder.setCode(INTERNAL_ERROR)
                    .setMessage(defaultString(e.getMessage()));
        } finally {
            responseObserver.onNext(newBuilder.build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 根据 token 获取 令牌
     */
    @Override
    public void getDeployToken(ProjectDeployTokenProto.GetDeployTokenRequest request,
                               StreamObserver<ProjectDeployTokenProto.DeployTokenResponse> responseObserver) {
        ProjectDeployTokenProto.DeployTokenResponse.Builder newBuilder = ProjectDeployTokenProto.DeployTokenResponse.newBuilder();
        try {
            ProjectToken projectToken = projectTokenService.getProjectToken(request.getToken());
            if (projectToken == null) {
                newBuilder.setCode(NOT_FOUND)
                        .setMessage("token is not found");
            } else {
                List<ProjectTokenArtifact> artifacts = projectTokenArtifactService.getByTokenId(projectToken.getId());
                List<ProjectTokenDepot> depots = projectTokenDepotService.getTokenById(projectToken.getId());
                newBuilder.setCode(SUCCESS)
                        .setData(toDeployToken(projectToken, artifacts, depots));
            }
        } catch (Exception e) {
            log.error("RpcService.getDeployToken token {},error:{}",
                    DesensitizationUtil.left(request.getToken(), 10),
                    e.getMessage()
            );
            newBuilder.setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        } finally {
            responseObserver.onNext(newBuilder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void obtainDefaultTokenKey(ProjectDeployTokenProto.ObtainDefaultTokenKeyRequest request, StreamObserver<ProjectDeployTokenProto.ObtainDefaultTokenKeyResponse> responseObserver) {
        ProjectDeployTokenProto.ObtainDefaultTokenKeyResponse.Builder builder = ProjectDeployTokenProto.ObtainDefaultTokenKeyResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            ProjectDeployTokenProto.DeployTokenType type = ProjectDeployTokenProto.DeployTokenType.forNumber(request.getDeployTokenType());
            if (ObjectUtils.isEmpty(type)) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }
            ProjectTokenKeyDTO tokenKeyDTO = projectTokenService.getOrGenerateTokenString(project, (short) request.getDeployTokenType());
            if (tokenKeyDTO == null) {
                builder.setCode(NOT_FOUND)
                        .setMessage("tokenKey is not found");
            } else {
                builder.setCode(SUCCESS);
                builder.setGlobalKey(tokenKeyDTO.getGlobalKey())
                        .setToken(tokenKeyDTO.getToken())
                        .build();
            }
        } catch (Exception e) {
            log.error("RpcService.obtainDefaultTokenKey error:{}", e.getMessage());
            builder.setCode(INTERNAL_ERROR)
                    .setMessage(e.getMessage());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void refreshInternalAccessToken(
            ProjectDeployTokenProto.RefreshInternalAccessTokenRequest request,
            StreamObserver<ProjectDeployTokenProto.RefreshInternalAccessTokenResponse> responseObserver) {
        ProjectDeployTokenProto.RefreshInternalAccessTokenResponse.Builder builder = ProjectDeployTokenProto.RefreshInternalAccessTokenResponse.newBuilder();
        try {
            Project project = projectService.getById(request.getProjectId());
            if (project == null) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
            }
            short tokenType;
            switch (request.getAccessType()) {
                case CODEDOG:
                    tokenType = ProjectToken.TYPE_CODEDOG;
                    break;
                case QTA:
                    tokenType = ProjectToken.TYPE_QTA;
                    break;
                case QCI:
                    tokenType = ProjectToken.TYPE_QCI;
                    break;
                default:
                    throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }

            ProjectToken token = projectTokenService.refreshInternalToken(project, tokenType);
            if (token != null) {
                String globalKey = getGlobalKey(token.getGlobalKeyId());
                if (StringUtils.isNotEmpty(globalKey)) {
                    builder.setCode(CodeProto.Code.SUCCESS)
                            .setAccessGK(globalKey)
                            .setToken(token.getToken())
                            .setExpiredAt(token.getExpiredAt().getTime());
                } else {
                    builder.setCode(CodeProto.Code.NOT_FOUND)
                            .setMessage("AccessGK is null");
                }
            } else {
                builder.setCode(CodeProto.Code.NOT_FOUND)
                        .setMessage("AccessToken is null");
            }

            responseObserver.onNext(builder.build());
        } catch (Exception e) {
            log.error("RpcService refreshInternalAccessToken error CoreException {}", e.getMessage());
            responseObserver.onNext(builder
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage(e.getMessage())
                    .build());
        }
        responseObserver.onCompleted();
    }

    private AddProjectTokenForm fromProto(ProjectDeployTokenProto.AddDeployTokenForm proto) {
        AddProjectTokenForm data = new AddProjectTokenForm();
        data.setTokenName(proto.getTokenName());
        data.setExpiredAt(proto.getExpiredAt());
        data.setScope(proto.getScope());
        data.setApplyToAllDepots(proto.getApplyToAllDepots());
        if (!CollectionUtils.isEmpty(proto.getDepotScopesList())) {
            data.setDepotScopes(proto.getDepotScopesList().stream()
                    .map(this::fromProto)
                    .collect(Collectors.toList()));
        }
        return data;
    }

    private ProjectDeployTokenProto.DeployToken toDeployToken(ProjectToken token) {
        return this.toDeployToken(token, null, null);

    }

    private ProjectDeployTokenProto.User toUser(UserProto.User user) {
        if (user == null) {
            return null;
        }
        return ProjectDeployTokenProto.User.newBuilder()
                .setId(user.getId())
                .setName(user.getName())
                .setAvatar(user.getAvatar())
                .setHtmlUrl(user.getHtmlUrl())
                .setUrl(user.getUrl())
                .setTeamId(user.getTeamId())
                .build();
    }

    private UserProto.User getUser(Integer userId) {
        return userGrpcClient.getUserById(userId);
    }

    private ProjectDeployTokenProto.User toUser(Integer userId) {
        if (userId != null) {
            UserProto.User user = getUser(userId);
            if (!ObjectUtils.isEmpty(user)) {
                return toUser(user);
            }
        }
        return null;
    }

    private ProjectDeployTokenProto.DeployToken toDeployToken(ProjectToken token,
                                                              List<ProjectTokenArtifact> deployTokenArtifactList,
                                                              List<ProjectTokenDepot> projectTokenDepots) {
        ProjectDeployTokenProto.User associated = toUser(token.getAssociatedId());
        ProjectDeployTokenProto.User creator = toUser(token.getCreatorId());
        ProjectDeployTokenProto.DeployToken.Builder builder = ProjectDeployTokenProto.DeployToken.newBuilder()
                .setId(token.getId())
                .setProjectId(token.getProjectId())
                .setCreatorId(token.getCreatorId())
                .setGlobalKeyId(token.getGlobalKeyId())
                .setTokenName(token.getTokenName())
                .setToken(defaultString(token.getToken()))
                .setScope(defaultString(token.getScope()))
                .setType(token.getType())
                .setEnabled(token.getEnabled())
                .setExpiredAt(toTimestamp(token.getExpiredAt()))
                .setLastActivityAt(toTimestamp(token.getLastActivityAt()))
                .setCreatedAt(toTimestamp(token.getCreatedAt()))
                .setUpdatedAt(toTimestamp(token.getUpdatedAt()))
                .setDeletedAt(toTimestamp(token.getDeletedAt()))
                .setApplyToAllDepots(token.getApplyToAllDepots())
                .setApplyToAllArtifacts(token.getApplyToAllArtifacts());

        if (associated != null) {
            builder.setAssociatedId(token.getAssociatedId())
                    .setAssociated(associated);
        }
        if (creator != null) {
            builder.setCreator(creator);
        }
        if (CollectionUtils.isNotEmpty(deployTokenArtifactList)) {
            builder.addAllDeployTokenArtifacts(deployTokenArtifactList.stream()
                    .map(this::toProtoDeployTokenArtifact).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(projectTokenDepots)) {
            builder.addAllDeployTokenDepots(projectTokenDepots.stream()
                    .map(this::toProtoDeployTokenDepot).collect(Collectors.toList()));
        }
        return builder.build();
    }

    private ProjectDeployTokenProto.DeployTokenArtifact toProtoDeployTokenArtifact(ProjectTokenArtifact deployTokenArtifact) {
        return ProjectDeployTokenProto.DeployTokenArtifact.newBuilder()
                .setDeployTokenId(deployTokenArtifact.getDeployTokenId())
                .setArtifactId(deployTokenArtifact.getArtifactId())
                .setArtifactScope(deployTokenArtifact.getArtifactScope())
                .build();
    }

    private ProjectDeployTokenProto.DeployTokenDepot toProtoDeployTokenDepot(ProjectTokenDepot projectTokenDepot) {
        return ProjectDeployTokenProto.DeployTokenDepot.newBuilder()
                .setDeployTokenId(projectTokenDepot.getDeployTokenId())
                .setDepotId(projectTokenDepot.getDepotId())
                .setDepotScope(projectTokenDepot.getDepotScope())
                .build();
    }

    private ProjectTokenDepotDTO fromProto(ProjectDeployTokenProto.DeployTokenDepotDTO proto) {
        return ProjectTokenDepotDTO.builder()
                .depotId(proto.getDepotId())
                .scope(proto.getScope())
                .build();
    }

    private String getGlobalKey(Integer globalKeyId) {
        try {

            GlobalKeyProto.GetByIdRequest request = GlobalKeyProto.GetByIdRequest.newBuilder()
                    .setId(globalKeyId)
                    .build();
            GlobalKeyProto.GetByIdResponse response = globalKeyGrpcClient.getById(request);
            return response.getData().getGlobalKey();
        } catch (Exception e) {
            log.error("RpcService getGlobalKey error CoreException {}", e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    private Integer getGlobalKey(String globalKey) {
        GlobalKeyProto.GetByGlobalKeyRequest request = GlobalKeyProto.GetByGlobalKeyRequest.newBuilder()
                .setGlobalKey(globalKey)
                .build();
        GlobalKeyProto.GetByGlobalKeyResponse response = globalKeyGrpcClient.getByGlobalKey(request);
        if (response.getCode() == SUCCESS) {
            return response.getData().getId();
        }
        return null;
    }

}
