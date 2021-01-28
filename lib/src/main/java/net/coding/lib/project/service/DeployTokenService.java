package net.coding.lib.project.service;

import net.coding.common.base.gson.JSON;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.common.util.BeanUtils;
import net.coding.common.vendor.CodingStringUtils;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.DeployTokenArtifactsDao;
import net.coding.lib.project.dao.DeployTokenDepotsDao;
import net.coding.lib.project.dao.DeployTokensDao;
import net.coding.lib.project.dao.DepotDao;
import net.coding.lib.project.dto.ArtifactScopeDTO;
import net.coding.lib.project.dto.DeployTokenArtifactDTO;
import net.coding.lib.project.dto.DeployTokenDTO;
import net.coding.lib.project.dto.DeployTokenDepotDTO;
import net.coding.lib.project.dto.DeployTokenScopeDTO;
import net.coding.lib.project.dto.DepotScopeDTO;
import net.coding.lib.project.entity.DeployTokenArtifacts;
import net.coding.lib.project.entity.DeployTokenDepot;
import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.entity.Depot;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddDeployTokenForm;
import net.coding.lib.project.grpc.client.ArtifactRepositoryGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.parameter.DeployTokenUpdateParameter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.globalKey.GlobalKeyProto;
import proto.platform.team.TeamProto;

import static net.coding.common.constants.DeployTokenScopeEnum.getWithValue;

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

    private static final Short TYPE_DEPLOY_TOKEN = 2;

    private final GlobalKeyGrpcClient globalKeyGrpcClient;

    private final DeployTokensDao deployTokensDao;

    private final DeployTokenDepotService deployTokenDepotService;

    private final DepotDao depotDao;

    private final DeployTokenArtifactsService deployTokenArtifactsService;

    private final ArtifactRepositoryGrpcClient artifactRepositoryGrpcClient;

    private final DeployTokenValidateService deployTokenValidateService;

    private final TeamGrpcClient teamGrpcClient;

    private final DeployTokenDepotsDao deployTokenDepotDao;

    private final DeployTokenArtifactsDao deployTokenArtifactDao;

    private static final Timestamp KEY_NEVER_EXPIRE = Timestamp.valueOf("9999-12-31 00:00:00");


    public List<DeployTokenDTO> getUserDeployTokens(Integer projectId) {

        List<DeployTokens> deployTokens = deployTokensDao.selectByDeployTokens(projectId, TYPE_USER);
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
                scopeEnum = getWithValue(StringUtils.trim(scope));
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
                            .map(str -> getWithValue(StringUtils.trim(str)))
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
                        .map(str -> getWithValue(StringUtils.trim(str)))
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
                .createdAt(deployToken.getCreatedAt().getTime())
                .lastActivityAt(deployToken.getLastActivityAt().getTime())
                .depotScopes(depotScopes)
                .artifactScopes(artifactScopes)
                .updatedAt(deployToken.getUpdatedAt().getTime())
                .token(token)
                .userName(getGlobalKeyById(deployToken.getGlobalKeyId()))
                .scopes(scopes)
                .build();

    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeployToken(Integer projectId, Integer deployTokenId) throws CoreException {
        DeployTokens deployToken = this.getDeployToken(projectId, deployTokenId);
        if (deployToken == null) {
            return false;
        }
        int result = deployTokensDao.deleteDeployTokens(deployToken.getId());
        deployTokenDepotService.deleteByTokenId(deployToken.getId());
        deployTokenArtifactsService.deleteByTokenId(deployToken.getId());
        return result > 0;
    }


    public boolean enableDeployToken(Integer projectId, Integer id, Boolean enable) throws CoreException {
        DeployTokens deployToken = this.getDeployToken(projectId, id);
        if (enable.equals(deployToken.getEnabled())) {
            return true;
        }
        int result = deployTokensDao.updateEnableDeployToken(deployToken.getId(), enable);
        return result > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean modifyDeployTokenScope(Integer projectId, Integer id, AddDeployTokenForm form) throws CoreException {
        DeployTokens deployToken = this.getDeployToken(projectId, id);
        deployTokenDepotService.deleteByTokenId(deployToken.getId());
        //更改项目令牌权限
        if (!form.isApplyToAllDepots() && !CollectionUtils.isEmpty(form.getDepotScopes())) {
            insertDeployTokenDepot(null, form.getDepotScopes());
        }
        deployTokenArtifactsService.deleteByTokenId(deployToken.getId());
        //更改项目令牌权限
        if (!form.isApplyToAllArtifacts() && !CollectionUtils.isEmpty(form.getArtifactScopes())) {
            insertDeployArtifact(deployToken.getId(), form.getArtifactScopes());
        }
        DeployTokenUpdateParameter parameter = DeployTokenUpdateParameter.builder()
                .applyToAllDepots(form.isApplyToAllDepots())
                .applyToAllArtifacts(form.isApplyToAllArtifacts())
                .expiredAt(getExpirationDate(form.getExpiredAt()))
                .scope(form.getScope())
                .id(deployToken.getId())
                .build();
        Integer result = deployTokensDao.update(parameter);
        return result > 0;
    }

    private void insertDeployTokenDepot(Integer id, List<DeployTokenDepotDTO> depotScopes) {
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        for (DeployTokenDepotDTO deployTokenDepotDTO : depotScopes) {
            DeployTokenDepot deployTokenDepot = DeployTokenDepot.builder()
                    .deployTokenId(Objects.nonNull(id) ? id : new Integer(deployTokenDepotDTO.getDepotId()))
                    .depotId(Integer.valueOf(deployTokenDepotDTO.getDepotId()))
                    .depotScope(deployTokenDepotDTO.getScope())
                    .createdAt(init_at)
                    .updatedAt(init_at)
                    .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                    .build();
            deployTokenDepotDao.insert(deployTokenDepot);
        }
    }

    private void insertDeployArtifact(Integer tokenId, List<DeployTokenArtifactDTO> artifactScopes) {
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        for (DeployTokenArtifactDTO deployTokenArtifactDTO : artifactScopes) {
            DeployTokenArtifacts deployTokenArtifact = DeployTokenArtifacts.builder()
                    .artifactId(deployTokenArtifactDTO.getArtifactId())
                    .deployTokenId(tokenId)
                    .artifactScope(deployTokenArtifactDTO.getScope())
                    .createdAt(init_at)
                    .updatedAt(init_at)
                    .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                    .build();

            deployTokenArtifactDao.insert(deployTokenArtifact);
        }
    }

    private int getGlobalKey(Integer teamGlobalKey) {
        GlobalKeyProto.GetAutoLowerNumericGKRequest request = GlobalKeyProto.GetAutoLowerNumericGKRequest.newBuilder()
                .setHasEnterpriseGK(false)
                .setEnterpriseGlobalKey(String.valueOf(teamGlobalKey))
                .setGkPrefix("pt")
                .build();
        GlobalKeyProto.GetAutoLowerNumericGKResponse response = globalKeyGrpcClient.getAutoLowerNumericGK(request);
        String autoGK = response.getData();
        GlobalKeyProto.AddGlobalKeyRequest addGlobalKeyRequest = GlobalKeyProto.AddGlobalKeyRequest.newBuilder()
                .setGlobalKey(autoGK)
                .setTargetType(TYPE_DEPLOY_TOKEN)
                .build();
        return globalKeyGrpcClient.addGlobalKey(addGlobalKeyRequest).getGlobalKey().getId();

    }

    @Transactional(rollbackFor = Exception.class)
    public DeployTokenDTO addDeployToken(Integer projectId,
                                         AddDeployTokenForm form) throws CoreException {

        Integer teamId;
        int userId = 0;
        if (!Objects.nonNull(SystemContextHolder.get())) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        teamId = SystemContextHolder.get().getTeamId();
        userId = SystemContextHolder.get().getId();
        if (teamId == null) {
            throw CoreException.of(CoreException.ExceptionType.ENTERPRISE_NOT_EXISTS);
        }
        int gkId = getGlobalKey(teamId);
        if (gkId <= 0) {
            throw CoreException.of(CoreException.ExceptionType.GLOBAL_KEY_INVALID);
        }
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        DeployTokens deployToken = DeployTokens.builder()
                .projectId(projectId)
                .creatorId(userId)
                .tokenName(StringUtils.trim(form.getTokenName()))
                .globalKeyId(gkId)
                .token(this.generateDeployToken())
                .expiredAt(getExpirationDate(form.getExpiredAt()))
                .scope(StringUtils.trim(form.getScope()))
                .applyToAllDepots(form.isApplyToAllDepots())
                .applyToAllArtifacts(form.isApplyToAllArtifacts())
                .type((short) 0)
                .enabled(true)
                .createdAt(init_at)
                .updatedAt(init_at)
                .lastActivityAt(init_at)
                .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                .build();

        Integer id = deployTokensDao.insert(deployToken);
        if (id < 0) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_CREATE_FAIL);
        }
        //关联token与每个仓库的权限
        if (!form.isApplyToAllDepots() && !CollectionUtils.isEmpty(form.getDepotScopes())) {
            List<DeployTokenDepotDTO> depotScopes = form.getDepotScopes();
            insertDeployTokenDepot(deployToken.getId(), depotScopes);
        }
        //关联token与每个制品库的权限
        if (!form.isApplyToAllArtifacts() && !CollectionUtils.isEmpty(form.getArtifactScopes())) {
            List<DeployTokenArtifactDTO> artifactScopes = form.getArtifactScopes();
            insertDeployArtifact(deployToken.getId(), artifactScopes);
        }
        return toDeployTokenDto(deployToken, true);

    }

    private String generateDeployToken() {
        return DigestUtils.sha1Hex(UUID.randomUUID().toString());
    }

    private String getGlobalKeyById(Integer id) {

        GlobalKeyProto.GetByIdRequest request = GlobalKeyProto.GetByIdRequest.newBuilder().setId(id).build();
        GlobalKeyProto.GetByIdResponse response = globalKeyGrpcClient.getById(request);
        return response.getData().getGlobalKey();
    }

    public void validateCreateForm(AddDeployTokenForm form) throws CoreException {

        valitedDepotAndArtifactScope(form);

        deployTokenValidateService.validate(form);

    }

    private void valitedDepotAndArtifactScope(AddDeployTokenForm form) throws CoreException {
        if (!CodingStringUtils.isBlank(form.getScope()) && !checkScopeIsLegal(form.getScope())) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_INVALID);
        }

        //校验仓库权限参数
        if (!CollectionUtils.isEmpty(form.getDepotScopes()) && !form.isApplyToAllDepots()
                && !checkDepotScopeIsLegal(form.getDepotScopes())) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_INVALID);
        }
        //校验制品库权限参数
        if (!CollectionUtils.isEmpty(form.getArtifactScopes()) && !form.isApplyToAllArtifacts()
                && !checkArtifactScopeIsLegal(form.getArtifactScopes())) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_INVALID);
        }
    }

    public void validateUpdateForm(AddDeployTokenForm form) throws CoreException {
        valitedDepotAndArtifactScope(form);
    }

    private Boolean checkScopeIsLegal(String scope) {
        if (CodingStringUtils.isBlank(scope)) {
            return false;
        }
        return Stream.of(scope.split(DEPLOY_TOKEN_SCOPE_DELIMITER))
                .anyMatch(s -> getWithValue(s) != null);
    }

    private Boolean checkArtifactScopeIsLegal(List<DeployTokenArtifactDTO> scope) {
        if (CollectionUtils.isEmpty(scope)) {
            return false;
        }
        Stream<String> stream = scope.stream().map(DeployTokenArtifactDTO::getScope)
                .flatMap(s -> Stream.of(s.split(DEPLOY_TOKEN_SCOPE_DELIMITER)));
        return stream.anyMatch(auth -> getWithValue(auth) != null);
    }

    private Boolean checkDepotScopeIsLegal(List<DeployTokenDepotDTO> scope) {
        if (CollectionUtils.isEmpty(scope)) {
            return false;
        }
        Stream<String> stream = scope.stream().map(DeployTokenDepotDTO::getScope)
                .flatMap(s -> Stream.of(s.split(DEPLOY_TOKEN_SCOPE_DELIMITER)));
        return stream.anyMatch(auth -> getWithValue(auth) != null);
    }

    public Timestamp getExpirationDate(String expiredAt) {
        if (StringUtils.isEmpty(expiredAt)) {
            return KEY_NEVER_EXPIRE;
        } else {
            return Timestamp.valueOf(expiredAt.trim());
        }
    }
}
