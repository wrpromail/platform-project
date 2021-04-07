package net.coding.app.project.grpc;


import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.DeployTokenService;
import net.coding.lib.project.service.ProjectService;
import net.coding.proto.platform.project.ProjectDeployTokenProto;
import net.coding.proto.platform.project.ProjectDeployTokenServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;


import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.globalKey.GlobalKeyProto;


@Slf4j
@AllArgsConstructor
@GRpcService
public class ProjectDeployTokenGrpcService extends ProjectDeployTokenServiceGrpc.ProjectDeployTokenServiceImplBase {

    private final ProjectService projectService;
    private final DeployTokenService deployTokenService;
    private final GlobalKeyGrpcClient globalKeyGrpcClient;


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
                    tokenType = DeployTokens.TYPE_CODEDOG;
                    break;
                case QTA:
                    tokenType = DeployTokens.TYPE_QTA;
                    break;
                case QCI:
                    tokenType = DeployTokens.TYPE_QCI;
                    break;
                default:
                    throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }

            DeployTokens token = deployTokenService.refreshInternalToken(project, tokenType);
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

}
