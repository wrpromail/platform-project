package net.coding.lib.project.service;

import net.coding.common.base.gson.JSON;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.common.util.BeanUtils;
import net.coding.common.vendor.CodingStringUtils;
import net.coding.grpc.client.platform.GlobalKeyGrpcClient;
import net.coding.lib.project.dao.ProjectTokenDepotDao;
import net.coding.lib.project.dao.ProjectTokenDao;
import net.coding.lib.project.dao.DepotDao;
import net.coding.lib.project.dao.ProjectTokenArtifactDao;
import net.coding.lib.project.dto.ArtifactScopeDTO;
import net.coding.lib.project.dto.ProjectTokenArtifactDTO;
import net.coding.lib.project.dto.ProjectTokenDTO;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;
import net.coding.lib.project.dto.ProjectTokenKeyDTO;
import net.coding.lib.project.dto.ProjectTokenScopeDTO;
import net.coding.lib.project.dto.DepotScopeDTO;
import net.coding.lib.project.entity.ProjectTokenArtifact;
import net.coding.lib.project.entity.ProjectTokenDepot;
import net.coding.lib.project.entity.ProjectToken;
import net.coding.lib.project.entity.Depot;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.AppException;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectAuthDenyException;
import net.coding.lib.project.exception.ProjectAuthTokenDisabledException;
import net.coding.lib.project.exception.ProjectAuthTokenExpiredException;
import net.coding.lib.project.form.AddProjectTokenForm;
import net.coding.lib.project.grpc.client.ArtifactRepositoryGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.parameter.DeployTokenUpdateParameter;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.globalKey.GlobalKeyProto;
import proto.platform.user.UserProto;

import static net.coding.common.constants.DeployTokenScopeEnum.getWithValue;

/**
 * @Author liuying
 * @Date 2021/1/7 10:56 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class ProjectTokenService {

    private static final short TYPE_USER = 0;        // 用户生成

    private static final String HIDE_STAR = "**********";

    private static final String DEPLOY_TOKEN_SCOPE_DELIMITER = ",";

    private static final Short TYPE_DEPLOY_TOKEN = 2;

    private final GlobalKeyGrpcClient globalKeyGrpcClient;

    private final ProjectTokenDao projectTokenDao;

    private final ProjectTokenDepotService projectTokenDepotService;

    private final DepotDao depotDao;

    private final ProjectTokenArtifactService projectTokenArtifactService;

    private final ArtifactRepositoryGrpcClient artifactRepositoryGrpcClient;

    private final ProjectTokenValidateService projectTokenValidateService;

    private final TeamGrpcClient teamGrpcClient;

    private final ProjectTokenDepotDao projectTokenDepotDao;

    private final ProjectTokenArtifactDao projectTokenArtifactDao;

    private static final Timestamp KEY_NEVER_EXPIRE = Timestamp.valueOf("9999-12-31 00:00:00");


    public List<ProjectTokenDTO> getUserProjectToken(Integer projectId) {
        List<ProjectToken> projectTokens = selectUserProjectToken(projectId);
        if (!CollectionUtils.isEmpty(projectTokens)) {
            return projectTokens.stream()
                    .map(dt -> toProjectTokenDTO(dt, false))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();

    }

    public List<ProjectToken> selectUserProjectToken(Integer projectId) {
        return projectTokenDao.selectByProjectToken(projectId, TYPE_USER);
    }

    public ProjectToken getProjectToken(Integer id) {
        return projectTokenDao.selectByPrimaryKey(id, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectToken getProjectToken(String token) {
        return projectTokenDao.selectByToken(token, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectToken getProjectToken(Integer projectId, Integer id) throws CoreException {

        ProjectToken deployToken = getProjectToken(id);
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
    public ProjectTokenDTO toProjectTokenDTO(ProjectToken projectToken, boolean showToken) {

        String originToken = projectToken.getToken();
        List<ProjectTokenScopeDTO> scopes = new ArrayList<>();
        String combinedScopes = projectToken.getScope();
        if (StringUtils.isNotBlank(combinedScopes)) {
            String[] scopeArr = combinedScopes.split(DEPLOY_TOKEN_SCOPE_DELIMITER);
            DeployTokenScopeEnum scopeEnum;
            for (String scope : scopeArr) {
                scopeEnum = getWithValue(StringUtils.trim(scope));
                if (scopeEnum != null) {
                    ProjectTokenScopeDTO projectTokenScopeDTO = ProjectTokenScopeDTO.builder().text(scopeEnum.getText())
                            .value(scopeEnum.getValue()).build();
                    scopes.add(projectTokenScopeDTO);
                }
            }
        }

        List<DepotScopeDTO> depotScopes = new ArrayList<>();
        if (!projectToken.getApplyToAllDepots()) {
            List<ProjectTokenDepot> deployTokenDepots = projectTokenDepotService.getTokenById(projectToken.getId());
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
                    List<ProjectTokenDepotDTO> scopeDTOs = Arrays.stream(it.getDepotScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER))
                            .map(str -> getWithValue(StringUtils.trim(str)))
                            .filter(Objects::nonNull)
                            .map(e -> ProjectTokenDepotDTO.builder().depotId(e.getValue()).scope(e.getText()).build())
                            .collect(Collectors.toList());
                    depotScopeDTO.setScopes(scopeDTOs);
                    depotScopes.add(depotScopeDTO);
                });
            }

        }

        List<ArtifactScopeDTO> artifactScopes = Collections.emptyList();
        // 是否应用到所有制品库
        if (!projectToken.getApplyToAllArtifacts()) {
            // 查询所有制品库权限设置
            List<ProjectTokenArtifact> deployTokenArtifacts = projectTokenArtifactService.getByTokenId(projectToken.getId());
            log.debug("ProjectTokenArtifacts = {}", JSON.toJson(deployTokenArtifacts));
            // 查询所有制品库
            List<Integer> artifactIdList = deployTokenArtifacts.stream().map(ProjectTokenArtifact::getArtifactId).collect(Collectors.toList());
            Map<Integer, String> reposMap = artifactRepositoryGrpcClient.getArtifactReposByIds(artifactIdList);
            // 组装返回数据
            artifactScopes = deployTokenArtifacts.stream().map(it -> {
                String[] split = it.getArtifactScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER);
                List<ProjectTokenScopeDTO> scopeDTOs = Arrays.stream(split)
                        .map(str -> getWithValue(StringUtils.trim(str)))
                        .filter(Objects::nonNull)
                        .map(s -> ProjectTokenScopeDTO.builder()
                                .text(s.getText())
                                .value(s.getValue())
                                .build())
                        .collect(Collectors.toList());
                return new ArtifactScopeDTO(it.getArtifactId(), reposMap.get(it.getArtifactId()), scopeDTOs);
            }).filter(it -> StringUtils.isNotBlank(it.getName())).collect(Collectors.toList());
        }

        String token = showToken ? originToken : originToken.replaceAll("(\\w{4})(\\w{22,32})(\\w{4})", "$1" + HIDE_STAR + "$3");
        return ProjectTokenDTO.builder()
                .id(projectToken.getId())
                .projectId(projectToken.getProjectId())
                .creatorId(projectToken.getCreatorId())
                .tokenName(projectToken.getTokenName())
                .expiredAt(projectToken.getExpiredAt())
                .enabled(projectToken.getEnabled())
                .applyToAllDepots(projectToken.getApplyToAllDepots())
                .applyToAllArtifacts(projectToken.getApplyToAllArtifacts())
                .createdAt(projectToken.getCreatedAt().getTime())
                .lastActivityAt(projectToken.getLastActivityAt().getTime())
                .depotScopes(depotScopes)
                .artifactScopes(artifactScopes)
                .updatedAt(projectToken.getUpdatedAt().getTime())
                .token(token)
                .userName(getGlobalKeyById(projectToken.getGlobalKeyId()))
                .scopes(scopes)
                .build();

    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProjectToken(Integer projectId, Integer deployTokenId) throws CoreException {
        ProjectToken deployToken = this.getProjectToken(projectId, deployTokenId);
        if (deployToken == null) {
            return false;
        }
        int result = projectTokenDao.deleteProjectToken(deployToken.getId());
        projectTokenDepotService.deleteByTokenId(deployToken.getId());
        projectTokenArtifactService.deleteByTokenId(deployToken.getId());
        return result > 0;
    }


    public boolean enableProjectToken(Integer projectId, Integer id, Boolean enable) throws CoreException {
        ProjectToken deployToken = this.getProjectToken(projectId, id);
        if (enable.equals(deployToken.getEnabled())) {
            return true;
        }
        int result = projectTokenDao.updateEnableProjectToken(deployToken.getId(), enable);
        return result > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean modifyProjectTokenScope(Integer projectId, Integer id, AddProjectTokenForm form) throws CoreException {
        ProjectToken deployToken = this.getProjectToken(projectId, id);
        projectTokenDepotService.deleteByTokenId(deployToken.getId());
        //更改项目令牌权限
        if (!form.isApplyToAllDepots() && !CollectionUtils.isEmpty(form.getDepotScopes())) {
            insertProjectTokenDepot(deployToken.getId(), form.getDepotScopes());
        }
        projectTokenArtifactService.deleteByTokenId(deployToken.getId());
        //更改项目令牌权限
        if (!form.isApplyToAllArtifacts() && !CollectionUtils.isEmpty(form.getArtifactScopes())) {
            insertProjectTokenArtifact(deployToken.getId(), form.getArtifactScopes());
        }
        DeployTokenUpdateParameter parameter = DeployTokenUpdateParameter.builder()
                .applyToAllDepots(form.isApplyToAllDepots())
                .applyToAllArtifacts(form.isApplyToAllArtifacts())
                .expiredAt(getExpirationDate(form.getExpiredAt()))
                .scope(form.getScope())
                .id(deployToken.getId())
                .build();
        Integer result = projectTokenDao.update(parameter);
        return result > 0;
    }

    private void insertProjectTokenDepot(Integer id, List<ProjectTokenDepotDTO> depotScopes) {
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        for (ProjectTokenDepotDTO projectTokenDepotDTO : depotScopes) {
            ProjectTokenDepot deployTokenDepot = ProjectTokenDepot.builder()
                    .deployTokenId(id)
                    .depotId(Integer.valueOf(projectTokenDepotDTO.getDepotId()))
                    .depotScope(projectTokenDepotDTO.getScope())
                    .createdAt(init_at)
                    .updatedAt(init_at)
                    .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                    .build();
            projectTokenDepotDao.insert(deployTokenDepot);
        }
    }

    private void insertProjectTokenArtifact(Integer tokenId, List<ProjectTokenArtifactDTO> artifactScopes) {
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        for (ProjectTokenArtifactDTO deployTokenArtifactDTO : artifactScopes) {
            ProjectTokenArtifact deployTokenArtifact = ProjectTokenArtifact.builder()
                    .artifactId(deployTokenArtifactDTO.getArtifactId())
                    .deployTokenId(tokenId)
                    .artifactScope(deployTokenArtifactDTO.getScope())
                    .createdAt(init_at)
                    .updatedAt(init_at)
                    .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                    .build();

            projectTokenArtifactDao.insert(deployTokenArtifact);
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

    public ProjectTokenDTO addDeployToken(Integer projectId, UserProto.User user,
                                          AddProjectTokenForm form, short type) throws CoreException {

        return toProjectTokenDTO(saveProjectToken(projectId, user, form, null, type), true);

    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectToken saveProjectToken(
            Integer projectId,
            UserProto.User user,
            AddProjectTokenForm form,
            Integer associatedId,
            short type
    ) throws CoreException {

        Integer teamId = user.getTeamId();
        int userId = user.getId();
        if (teamId == null) {
            throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
        }
        int gkId = getGlobalKey(teamId);
        if (gkId <= 0) {
            throw CoreException.of(CoreException.ExceptionType.GLOBAL_KEY_INVALID);
        }
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        ProjectToken projectToken = ProjectToken.builder()
                .projectId(projectId)
                .creatorId(userId)
                .tokenName(StringUtils.trim(form.getTokenName()))
                .globalKeyId(gkId)
                .token(this.generateProjectToken())
                .expiredAt(getExpirationDate(form.getExpiredAt()))
                .scope(StringUtils.trim(form.getScope()))
                .applyToAllDepots(form.isApplyToAllDepots())
                .applyToAllArtifacts(form.isApplyToAllArtifacts())
                .type(type)
                .enabled(true)
                .createdAt(init_at)
                .updatedAt(init_at)
                .lastActivityAt(init_at)
                .associatedId(associatedId)
                .deletedAt(Timestamp.valueOf(BeanUtils.NOT_DELETED_AT))
                .build();

        Integer id = projectTokenDao.insert(projectToken);
        if (id < 0) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_CREATE_FAIL);
        }
        //关联token与每个仓库的权限
        if (!form.isApplyToAllDepots() && !CollectionUtils.isEmpty(form.getDepotScopes())) {
            List<ProjectTokenDepotDTO> depotScopes = form.getDepotScopes();
            insertProjectTokenDepot(projectToken.getId(), depotScopes);
        }
        //关联token与每个制品库的权限
        if (!form.isApplyToAllArtifacts() && !CollectionUtils.isEmpty(form.getArtifactScopes())) {
            List<ProjectTokenArtifactDTO> artifactScopes = form.getArtifactScopes();
            insertProjectTokenArtifact(projectToken.getId(), artifactScopes);
        }
        return projectToken;

    }

    private String generateProjectToken() {
        return DigestUtils.sha1Hex(UUID.randomUUID().toString());
    }

    private String getGlobalKeyById(Integer id) {

        GlobalKeyProto.GetByIdRequest request = GlobalKeyProto.GetByIdRequest.newBuilder().setId(id).build();
        GlobalKeyProto.GetByIdResponse response = globalKeyGrpcClient.getById(request);
        return response.getData().getGlobalKey();
    }

    public void validateCreateForm(AddProjectTokenForm form) throws CoreException {

        valitedDepotAndArtifactScope(form);

        projectTokenValidateService.validate(form);

    }

    private void valitedDepotAndArtifactScope(AddProjectTokenForm form) throws CoreException {
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

    public void validateUpdateForm(AddProjectTokenForm form) throws CoreException {
        valitedDepotAndArtifactScope(form);
    }

    private Boolean checkScopeIsLegal(String scope) {
        if (CodingStringUtils.isBlank(scope)) {
            return false;
        }
        return Stream.of(scope.split(DEPLOY_TOKEN_SCOPE_DELIMITER))
                .anyMatch(s -> getWithValue(s) != null);
    }

    private Boolean checkArtifactScopeIsLegal(List<ProjectTokenArtifactDTO> scope) {
        if (CollectionUtils.isEmpty(scope)) {
            return false;
        }
        Stream<String> stream = scope.stream().map(ProjectTokenArtifactDTO::getScope)
                .flatMap(s -> Stream.of(s.split(DEPLOY_TOKEN_SCOPE_DELIMITER)));
        return stream.anyMatch(auth -> getWithValue(auth) != null);
    }

    private Boolean checkDepotScopeIsLegal(List<ProjectTokenDepotDTO> scope) {
        if (CollectionUtils.isEmpty(scope)) {
            return false;
        }
        Stream<String> stream = scope.stream().map(ProjectTokenDepotDTO::getScope)
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

    public ProjectToken refreshInternalToken(Project project, short tokenType) throws CoreException {

        String tokenName;
        switch (tokenType) {
            case ProjectToken.TYPE_CODEDOG:
                tokenName = ProjectToken.CODEDOG_TOKEN_NAME;
                break;
            case ProjectToken.TYPE_QTA:
                tokenName = ProjectToken.QTA_TOKEN_NAME;
                break;
            case ProjectToken.TYPE_QCI:
                tokenName = ProjectToken.QCI_TOKEN_NAME;
                break;
            default:
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }

        ProjectToken token = projectTokenDao.selectProjectToken(
                project.getId(),
                tokenType,
                tokenName,
                BeanUtils.getDefaultDeletedAt()
        );
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(1);
        if (token == null) {
            AddProjectTokenForm form = new AddProjectTokenForm();
            form.setTokenName(tokenName);
            form.setExpiredAt(DateUtil.dateTimeToStr(expiredAt, "yyyy-MM-dd HH:mm:ss"));
            form.setScope(DeployTokenScopeEnum.DEPOT_READ.getValue());
            UserProto.User user = teamGrpcClient.getTeam(project.getTeamOwnerId()).getData().getOwner();
            return saveProjectToken(project.getId(), user, form, null, tokenType);
        }
        token.setExpiredAt(Timestamp.valueOf(expiredAt));
        int result = projectTokenDao.updateExpired(
                token.getId(),
                Timestamp.valueOf(expiredAt),
                BeanUtils.getDefaultDeletedAt()
        );
        if (result != 1) {
            log.error("refresh internal access token failed. id={}", token.getId());
        }
        return token;
    }

    public boolean checkPermission(Integer tokenId, String token, Integer projectId, Set<String> supportScopes) throws AppException {
        ProjectToken projectToken;
        if (tokenId != null && tokenId > 0) {
            projectToken = projectTokenDao.selectByPrimaryKey(tokenId, BeanUtils.getDefaultDeletedAt());
        } else {
            projectToken = getTokenByTokenAndProjectId(token, projectId);
        }

        if (projectToken == null || CollectionUtils.isEmpty(supportScopes)) {
            return false;
        }

        if (!projectToken.getProjectId().equals(projectId)) {
            throw new ProjectAuthDenyException();
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (!projectToken.getEnabled()) {
            throw new ProjectAuthTokenDisabledException();
        }

        if (now.after(projectToken.getExpiredAt())) {
            throw new ProjectAuthTokenExpiredException();
        }

        return Stream.of(projectToken.getScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER))
                .anyMatch(supportScopes::contains);
    }

    public boolean checkCiAgentPermission(String deployToken) {
        if (StringUtils.isEmpty(deployToken)) {
            return false;
        }
        ProjectToken token = getProjectToken(deployToken);
        if (token == null) {
            return false;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        boolean isExpired = now.after(token.getExpiredAt());
        boolean containsCiAgent = containsCiAgentRegister(token.getScope());
        return token.getEnabled() && !isExpired && containsCiAgent;
    }

    private boolean containsCiAgentRegister(String tokenScope) {
        List<DeployTokenScopeEnum> scopes = Stream.of(StringUtils.split(tokenScope, ","))
                .map(DeployTokenScopeEnum::getWithValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return scopes.stream().anyMatch(e -> StringUtils.equals(DeployTokenScopeEnum.CI_AGENT_REGISTER.getValue(), e.getValue()));
    }

    public ProjectToken getTokenByTokenAndProjectId(String token, Integer projectId) {
        return projectTokenDao.selectByTokenAndProjectId(token, projectId, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectToken getByTokenAndGlobalKeyId(String token, Integer gkId) {
        return projectTokenDao.selectByTokenAndGkId(token, gkId, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectTokenKeyDTO getOrGenerateTokenString(Project project, short deployTokenType) {

        try {
            ProjectToken token = getOrGenerateToken(project, deployTokenType);
            String key = getGlobalKeyById(token.getGlobalKeyId());
            return ProjectTokenKeyDTO.builder()
                    .globalKey(key)
                    .token(token.getToken())
                    .build();
        } catch (AppException | CoreException e) {
            log.error("Generate ci deploy token fail:", e.getMessage());
        }
        return null;
    }

    public ProjectToken getOrGenerateToken(Project project, short deployTokenType) throws AppException, CoreException {
        final String scope = Arrays.stream(DeployTokenScopeEnum.values())
                .map(DeployTokenScopeEnum::getValue)
                .collect(Collectors.joining(","));
        final String tokenName;
        switch (deployTokenType) {
            case ProjectToken.TYPE_SYSTEM_CI:
                tokenName = "CI-TOKEN-" + project.getId();
                break;
            case ProjectToken.TYPE_SYSTEM_AUTO_DEPLOY:
                tokenName = "AUTODEPLOY-TOKEN-" + project.getId();
                break;
            default:
                throw new IllegalArgumentException("unknown deploy token type");
        }

        AddProjectTokenForm form = new AddProjectTokenForm();
        form.setScope(scope);
        form.setTokenName(tokenName);
        form.setExpiredAt(StringUtils.EMPTY);
        UserProto.User user = teamGrpcClient.getTeam(project.getTeamOwnerId()).getData().getOwner();
        return getOrGenerateToken(project, user, deployTokenType, form);
    }

    public ProjectToken getOrGenerateToken(Project project, UserProto.User currentUser, short deployTokenType, AddProjectTokenForm form) throws AppException, CoreException {
        ProjectToken projectToken = projectTokenDao.selectProjectToken(
                project.getId(),
                deployTokenType,
                form.getTokenName(),
                BeanUtils.getDefaultDeletedAt()
        );
        if (projectToken == null) {
            projectToken = saveProjectToken(project.getId(), currentUser, form, null, deployTokenType);
        } else if (!projectToken.getEnabled()
                || !KEY_NEVER_EXPIRE.equals(projectToken.getExpiredAt())
                || !form.getScope().equals(projectToken.getScope())) {
            projectTokenDao.updateScopeById(
                    projectToken.getId(),
                    StringUtils.trim(form.getScope()),
                    BeanUtils.getDefaultDeletedAt()
            );
        }
        return projectToken;
    }
}
