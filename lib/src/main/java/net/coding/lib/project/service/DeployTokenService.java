package net.coding.lib.project.service;

import net.coding.common.base.gson.JSON;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.lib.project.dao.DeployTokensDao;
import net.coding.lib.project.dao.DepotDao;
import net.coding.lib.project.dto.ArtifactScopeDTO;
import net.coding.lib.project.dto.DeployTokenDTO;
import net.coding.lib.project.dto.DeployTokenDepotDTO;
import net.coding.lib.project.dto.DeployTokenScopeDTO;
import net.coding.lib.project.dto.DepotScopeDTO;
import net.coding.lib.project.entity.DeployTokenArtifacts;
import net.coding.lib.project.entity.DeployTokenDepot;
import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.entity.Depot;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.ArtifactRepositoryGrpcClient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.globalKey.GlobalKeyProto;

/**
 * @Author liuying
 * @Date 2021/1/7 10:56 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class DeployTokenService {

    private static final short TYPE_USER = 0;        // 用户生成

    private static final String HIDE_STAR = "**********";

    private static final String DEPLOY_TOKEN_SCOPE_DELIMITER = ",";

    private final GlobalKeyGrpcClient globalKeyGrpcClient;

    private final DeployTokensDao deployTokensDao;

    private final DeployTokenDepotService deployTokenDepotService;

    private final DepotDao depotDao;

    private final DeployTokenArtifactsService deployTokenArtifactsService;

    private final ArtifactRepositoryGrpcClient artifactRepositoryGrpcClient;


    public List<DeployTokenDTO> getUserDeployTokens(Integer projectId) {

        DeployTokens query = DeployTokens.builder()
                .projectId(projectId)
                .type(TYPE_USER)
                .build();
        List<DeployTokens> deployTokens = deployTokensDao.selectByDeployTokens(query);
        if (!CollectionUtils.isEmpty(deployTokens)) {
            return deployTokens.stream()
                    .map(dt -> toDeployTokenDto(dt, false))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();

    }

    public DeployTokens getDeployToken(Integer projectId, Integer id) throws CoreException {

        DeployTokens deployToken = deployTokensDao.selectByPrimaryKey(id);
        if (deployToken == null) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_NOT_EXIST);
        }
        if (!deployToken.getProjectId().equals(projectId)) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_PROJECT_NOT_MATCH);
        }
        return deployToken;

    }


    /**
     * 转换为可见结果
     *
     * @param showToken 新建部署令牌应返回完整密码
     */
    public DeployTokenDTO toDeployTokenDto(DeployTokens deployToken, boolean showToken) {

        String originToken = deployToken.getToken();
        List<DeployTokenScopeDTO> scopes = new ArrayList<>();
        String combinedScopes = deployToken.getScope();
        if (StringUtils.isNotBlank(combinedScopes)) {
            String[] scopeArr = combinedScopes.split(DEPLOY_TOKEN_SCOPE_DELIMITER);
            DeployTokenScopeEnum scopeEnum;
            for (String scope : scopeArr) {
                scopeEnum = DeployTokenScopeEnum.getWithValue(StringUtils.trim(scope));
                if (scopeEnum != null) {
                    DeployTokenScopeDTO deployTokenScopeDTO = DeployTokenScopeDTO.builder().text(scopeEnum.getText())
                            .value(scopeEnum.getValue()).build();
                    scopes.add(deployTokenScopeDTO);
                }
            }
        }

        List<DepotScopeDTO> depotScopes = new ArrayList<>();
        if (!deployToken.getApplyToAllDepots()) {
            List<DeployTokenDepot> deployTokenDepots = deployTokenDepotService.getTokenById(deployToken.getId());
            List<Integer> depotIds = deployTokenDepots.stream().map(it -> it.getDepotId()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(depotIds)) {

                Map<Integer, Depot> depotMap = depotDao.getByIds(depotIds).stream()
                        .filter(Objects::nonNull)
                        .filter(d -> d.getId() != null)
                        .collect(Collectors.toMap(it -> it.getId(), Function.identity()));
                deployTokenDepots.stream().filter(e -> depotMap.get(e.getDepotId()) != null).forEach(it -> {

                    Depot depot = depotMap.get(it.getDepotId());
                    DepotScopeDTO depotScopeDTO = DepotScopeDTO.builder().depotName(depot.getName())
                            .id(depot.getId()).build();
                    List<DeployTokenDepotDTO> scopeDTOs = Arrays.stream(it.getDepotScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER))
                            .map(str -> DeployTokenScopeEnum.getWithValue(StringUtils.trim(str)))
                            .filter(Objects::nonNull)
                            .map(e -> DeployTokenDepotDTO.builder().depotId(e.getValue()).scope(e.getText()).build())
                            .collect(Collectors.toList());
                    depotScopeDTO.setScopes(scopeDTOs);
                    depotScopes.add(depotScopeDTO);
                });
            }

        }

        List<ArtifactScopeDTO> artifactScopes = Collections.emptyList();
        // 是否应用到所有制品库
        if (!deployToken.getApplyToAllArtifacts()) {
            // 查询所有制品库权限设置
            List<DeployTokenArtifacts> deployTokenArtifacts = deployTokenArtifactsService.getByTokenId(deployToken.getId());
            log.debug("deployTokenArtifacts = {}", JSON.toJson(deployTokenArtifacts));
            // 查询所有制品库
            List<Integer> artifactIdList = deployTokenArtifacts.stream().map(DeployTokenArtifacts::getArtifactId).collect(Collectors.toList());
            Map<Integer, String> reposMap = artifactRepositoryGrpcClient.getArtifactReposByIds(artifactIdList);
            // 组装返回数据
            artifactScopes = deployTokenArtifacts.stream().map(it -> {
                String[] split = it.getArtifactScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER);
                List<DeployTokenScopeDTO> scopeDTOs = Arrays.stream(split)
                        .map(str -> DeployTokenScopeEnum.getWithValue(StringUtils.trim(str)))
                        .filter(Objects::nonNull)
                        .map(s -> DeployTokenScopeDTO.builder()
                                .text(s.getText())
                                .value(s.getValue())
                                .build())
                        .collect(Collectors.toList());
                return new ArtifactScopeDTO(it.getArtifactId(), reposMap.get(it.getArtifactId()), scopeDTOs);
            }).filter(it -> StringUtils.isNotBlank(it.getName())).collect(Collectors.toList());
        }

        String token = showToken ? originToken : originToken.replaceAll("(\\w{4})(\\w{22,32})(\\w{4})", "$1" + HIDE_STAR + "$3");
        return DeployTokenDTO.builder()
                .id(deployToken.getId())
                .projectId(deployToken.getProjectId())
                .creatorId(deployToken.getCreatorId())
                .tokenName(deployToken.getTokenName())
                .expiredAt(deployToken.getExpiredAt())
                .enabled(deployToken.getEnabled())
                .applyToAllDepots(deployToken.getApplyToAllDepots())
                .applyToAllArtifacts(deployToken.getApplyToAllArtifacts())
                .createdAt(deployToken.getCreatedAt())
                .lastActivityAt(deployToken.getLastActivityAt())
                .depotScopes(depotScopes)
                .artifactScopes(artifactScopes)
                .updatedAt(deployToken.getUpdatedAt())
                .token(token)
                .userName(getGlobalKey(deployToken.getGlobalKeyId()))
                .scopes(scopes)
                .build();

    }

    private String getGlobalKey(Integer id) {

        GlobalKeyProto.GetByIdRequest request = GlobalKeyProto.GetByIdRequest.newBuilder().setId(id).build();
        GlobalKeyProto.GetByIdResponse response = globalKeyGrpcClient.getById(request);
        return response.getData().getGlobalKey();
    }

}
