package net.coding.app.project.grpc.openapi;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectToken;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddProjectTokenForm;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectTokenService;
import net.coding.proto.open.api.deploy.token.DeployTokenProto;
import net.coding.proto.open.api.deploy.token.DeployTokenServiceGrpc;
import net.coding.proto.open.api.result.CommonProto;
import net.coding.proto.platform.project.ProjectDeployTokenProto;

import org.apache.commons.collections.CollectionUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.globalKey.GlobalKeyProto;
import proto.platform.user.UserProto;

import static net.coding.common.util.DateTimeUtils.toTimestamp;
import static net.coding.e.proto.ApiCodeProto.Code.INVALID_PARAMETER;
import static net.coding.e.proto.ApiCodeProto.Code.NOT_FOUND;
import static net.coding.e.proto.ApiCodeProto.Code.SUCCESS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;


@Slf4j
@GRpcService
@AllArgsConstructor
public class OpenApiDeployTokenGRpcService extends DeployTokenServiceGrpc.DeployTokenServiceImplBase {

    private final UserGrpcClient userGrpcClient;

    private final GlobalKeyGrpcClient globalKeyGrpcClient;
    private final ProjectService projectService;
    private final ProjectTokenService projectTokenService;
    private final LocaleMessageSource localeMessageSource;

    @Override
    public void createDeployToken(
            DeployTokenProto.CreateDeployTokenRequest request,
            StreamObserver<DeployTokenProto.CreateDeployTokenResponse> responseObserver) {
        DeployTokenProto.CreateDeployTokenResponse.Builder builder =
                DeployTokenProto.CreateDeployTokenResponse.newBuilder();
        CommonProto.Result.Builder resultBuilder = CommonProto.Result.newBuilder();
        try {
            UserProto.User currentUser = userGrpcClient.getUserById(request.getUser().getId());
            if (currentUser == null) {
                log.warn("creator not exist userId {}", request.getUser().getId());
                throw CoreException.of(USER_NOT_EXISTS);
            }
            Project project = projectService.getByIdAndTeamId(
                    request.getProjectId(),
                    request.getUser().getTeamId()
            );
            if (Objects.isNull(project)) {
                throw CoreException.of(PROJECT_NOT_EXIST);
            }
            ProjectDeployTokenProto.DeployTokenType type =
                    ProjectDeployTokenProto.DeployTokenType.forNumber(request.getDeployType());
            if (ObjectUtils.isEmpty(type)) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            ProjectToken projectToken = projectTokenService.saveProjectToken(
                    request.getProjectId(),
                    currentUser,
                    fromProto(request.getCreateDeployToken()),
                    null,
                    (short) request.getDeployType()
            );
            builder.setResult(resultBuilder.setCode(SUCCESS.getNumber()).build())
                    .setDeployToken(deployTokenToProto(projectToken));
        } catch (CoreException e) {
            builder.setResult(
                    resultBuilder
                            .setCode(NOT_FOUND.getNumber())
                            .setMessage(localeMessageSource.getMessage(e.getKey()))
                            .build()
            );
        } catch (Exception e) {
            log.error("RpcService createDeployToken error Exception ", e);
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

    private AddProjectTokenForm fromProto(DeployTokenProto.CreateDeployToken proto) throws CoreException {
        AddProjectTokenForm tokenForm = AddProjectTokenForm.builder()
                .tokenName(proto.getTokenName())
                .expiredAt(proto.getExpiredAt())
                .scope(proto.getScope())
                .applyToAllDepots(proto.getApplyToAllDepots())
                .build();
        if (!CollectionUtils.isEmpty(proto.getDepotScopesList())) {
            tokenForm.setDepotScopes(proto.getDepotScopesList().stream()
                    .map(this::fromProto)
                    .collect(Collectors.toList()));
        }
        projectTokenService.validateCreateForm(tokenForm);
        return tokenForm;
    }

    private ProjectTokenDepotDTO fromProto(DeployTokenProto.DeployTokenDepot proto) {
        return ProjectTokenDepotDTO.builder()
                .depotId(proto.getDepotId())
                .scope(proto.getScope())
                .build();
    }

    private DeployTokenProto.DeployTokenResp deployTokenToProto(ProjectToken token) {
        String globalKey = Optional.ofNullable(
                        globalKeyGrpcClient.getById(
                                GlobalKeyProto.GetByIdRequest.newBuilder()
                                        .setId(token.getGlobalKeyId())
                                        .build()
                        )
                )
                .map(GlobalKeyProto.GetByIdResponse::getData)
                .map(GlobalKeyProto.GlobalKey::getGlobalKey)
                .orElse(EMPTY);
        return DeployTokenProto.DeployTokenResp.newBuilder()
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
                .setApplyToAllDepots(token.getApplyToAllDepots())
                .setGlobalKey(globalKey)
                .build();
    }
}
