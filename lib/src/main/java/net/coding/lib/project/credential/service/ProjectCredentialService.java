package net.coding.lib.project.credential.service;

import com.google.gson.Gson;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.common.util.StringUtils;
import net.coding.common.vendor.qcloud.utilities.Base64;
import net.coding.common.vendor.qcloud.utilities.SHA1;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.credential.enums.CredentialGenerated;
import net.coding.lib.project.credential.enums.CredentialJenkinsScheme;
import net.coding.lib.project.credential.enums.CredentialType;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.credentail.AndroidCredentialDao;
import net.coding.lib.project.dao.credentail.ProjectCredentialDao;
import net.coding.lib.project.dao.credentail.ProjectCredentialTaskDao;
import net.coding.lib.project.dao.credentail.TencentServerlessCredentialsDao;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.dto.CredentialDTO;
import net.coding.lib.project.entity.AndroidCredential;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.TencentServerlessCredential;
import net.coding.lib.project.enums.VerificationMethodEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.AndroidCredentialForm;
import net.coding.lib.project.form.credential.BaseCredentialForm;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.form.credential.TencentServerlessCredentialForm;
import net.coding.lib.project.grpc.client.OauthServiceGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.pager.ResultPageFactor;
import net.coding.lib.project.service.ProjectMemberService;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;
import proto.platform.oauth.OauthProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

@Slf4j
@AllArgsConstructor
@Service
public class ProjectCredentialService {
    private final ProjectDao projectDao;
    private final ProjectCredentialDao projectCredentialDao;
    private final ProjectCredentialTaskDao projectCredentialTaskDao;
    private final AndroidCredentialDao androidCredentialDao;
    private final TencentServerlessCredentialsDao tencentServerlessCredentialsDao;
    private final ProjectCredentialTaskService projectCredentialTaskService;
    private final UserGrpcClient userGrpcClient;
    private final ProjectCredentialRsaService credentialRsaService;
    private final OauthServiceGrpcClient oauthServiceGrpcClient;
    private final TencentServerlessCredentialService tencentServerlessCredentialService;
    private final TeamGrpcClient teamGrpcClient;
    private final ProjectMemberService projectMemberService;
    private final Gson gson;

    public ResultPage<CredentialDTO> list(
            Integer projectId,
            CredentialType credentialType,
            PageRowBounds pager
    ) throws CoreException {
        Project project = getProject(projectId);
        UserProto.User currentUser = getUser();
        String type;
        if (credentialType != null) {
            type = credentialType.name();
        } else {
            type = StringUtils.EMPTY;
        }
        List<Credential> credentials = projectCredentialDao.findPage(
                project.getId(),
                currentUser.getId(),
                type,
                pager,
                BeanUtils.getDefaultDeletedAt()
        );
        List<CredentialDTO> list = credentials.stream().map(
                credential -> {
                    CredentialDTO credentialDTO = toBuildCredentialDTO(credential);
//                    List<ConnectionTaskDTO> connectionTaskDTOs =
//                            projectCredentialTaskService.taskFilterSelected(projectId, credential);
//                    credentialDTO.setSelectedTasks(connectionTaskDTOs);
//                    credentialDTO.setTaskCount(connectionTaskDTOs.size());
                    return credentialDTO;
                }
        ).collect(Collectors.toList());
        return new ResultPageFactor<CredentialDTO>().def(pager, list);
    }

    public List<Credential> list(Integer projectId, Integer userId, Integer id) {
        return Optional.ofNullable(
                projectCredentialDao.getCredential(projectId, userId, id, BeanUtils.getDefaultDeletedAt())
        ).orElse(new ArrayList<>());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id, Integer projectId) throws CoreException {
        getProject(projectId);
        try {
            Credential credential = get(id, projectId);
            if (credential == null) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NOT_EXIST);
            }
            deleteWithCertificate(credential);
            projectCredentialDao.delete(id);
            projectCredentialTaskDao.deleteByCredId(id);
        } catch (Exception e) {
            log.error("delete service connection fail {}", e.getMessage());
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_DELETE_ERROR);
        }
    }

    public CredentialDTO getCredential(Integer projectId, int id) {
        Credential credential = get(id, true);
        CredentialDTO credentialDTO = toBuildCredentialDTO(credential);
        //获取关联task 信息
        List<ConnectionTaskDTO> connectionTaskDTOs =
                projectCredentialTaskService.taskFilterSelected(projectId, credential);
        credentialDTO.setSelectedTasks(connectionTaskDTOs);
        credentialDTO.setTaskCount(connectionTaskDTOs.size());
        extendCredentialDTO(credential, credentialDTO);
        UserProto.User user = userGrpcClient.getUserById(credential.getCreatorId());
        credentialDTO.setAvatar(user.getAvatar());
        credentialDTO.setNickname(user.getName());
        return credentialDTO;
    }

    /**
     * 给 DTO 补全属性
     */
    private void extendCredentialDTO(Credential credential, CredentialDTO credentialDTO) {
        if (StringUtils.equals(CredentialType.ANDROID_CERTIFICATE.toString(), credential.getType())) {
            AndroidCredential androidCredential = androidCredentialDao.getByConnId(
                    credential.getId(), BeanUtils.getDefaultDeletedAt()
            );
            credentialDTO.setFileName(androidCredential.getFileName());
            credentialDTO.setAlias(androidCredential.getAlias());
        } else if (StringUtils.equals(CredentialType.TENCENT_SERVERLESS.toString(), credential.getType())) {
            TencentServerlessCredential tencentCredential = tencentServerlessCredentialsDao.getByConnId(
                    credential.getId(),
                    BeanUtils.getDefaultDeletedAt()
            );
            if (tencentCredential != null) {
                credentialDTO.setAppId(tencentCredential.getAppId().toString());
                credentialDTO.setFailure(tencentCredential.getWasted());
            }
        }
    }

    private void deleteWithCertificate(Credential credential) {
        CredentialType credentialType = CredentialType.valueOf(credential.getType());
        switch (credentialType) {
            case ANDROID_CERTIFICATE:
                androidCredentialDao.deleteByCredId(credential.getId());
                break;
            case TENCENT_SERVERLESS:
                tencentServerlessCredentialsDao.deleteByCredId(credential.getId());
                break;
            default:
                break;
        }
    }

    public Credential get(int id, int projectId) {
        Optional<Credential> credential = Optional.ofNullable(
                projectCredentialDao.get(
                        id,
                        projectId,
                        BeanUtils.getDefaultDeletedAt()
                )
        );
        return credential.orElse(null);
    }

    public Credential getById(int id) {
        Optional<Credential> credential = Optional.ofNullable(
                projectCredentialDao.selectByPrimaryKey(
                        id,
                        BeanUtils.getDefaultDeletedAt()
                )
        );
        return credential.orElse(null);
    }

    public Credential get(int id, boolean decrypt) {
        Credential credential = getById(id);
        if (credential != null) {
            credential = this.extendCredential(credential);
            if (decrypt) {
                credentialRsaService.decrypt(credential);
            }
        }
        return credential;
    }

    public Credential getByCredential(String credentialId, boolean decrypt) {
        Credential credential = getByCredentialId(credentialId);
        if (credential != null) {
            credential = this.extendCredential(credential);
            if (decrypt) {
                if (CredentialType.ANDROID_CERTIFICATE.name().equalsIgnoreCase(credential.getType())) {
                    credentialRsaService.decrypt(credential.getAndroidCredential());
                }
                credentialRsaService.decrypt(credential);
            }
        }
        return credential;
    }

    public Credential getByCredentialId(String credentialId) {
        return projectCredentialDao.getByCredentialId(credentialId, BeanUtils.getDefaultDeletedAt());
    }

    private Credential extendCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        if (credential.getType().equalsIgnoreCase(CredentialType.OAUTH.name())) {
            OauthProto.OauthAccessToken oauthAccessToken = oauthServiceGrpcClient
                    .getOauthAccessToken(Integer.parseInt(credential.getToken()));
            if (oauthAccessToken != null) {
                credential.setPassword(oauthAccessToken.getAccessToken());
            }
        } else if (credential.getType().equalsIgnoreCase(CredentialType.TENCENT_SERVERLESS.name())) {
            credential = tencentServerlessCredentialService.flushIfNeed(credential);
        } else if (credential.getType().equalsIgnoreCase(CredentialType.ANDROID_CERTIFICATE.name())) {
            AndroidCredential androidCredential =
                    androidCredentialDao.getByConnId(credential.getId(), BeanUtils.getDefaultDeletedAt());
            if (androidCredential != null) {
                credential.setAndroidCredential(androidCredential);
            }
        }
        return credential;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateCredential(
            Integer projectId,
            Integer id,
            BaseCredentialForm form
    ) throws CoreException {
        Project project = getProject(projectId);
        form.setId(id);
        form.setProjectId(projectId);
        UserProto.User currentUser = getUser();
        form.setCreatorId(currentUser.getId());
        form.setTeamId(project.getTeamOwnerId());
        int credId = updateCredential(form, true);
        if (credId != 0) {
            projectCredentialTaskService.batchToggleTaskPermission(
                    form.getProjectId(),
                    credId,
                    form.getTaskDTOS()
            );
        }
        return credId;
    }

    public int updateCredential(BaseCredentialForm form, boolean encrypt) throws CoreException {
        try {
            int connId = form.getId();
            Credential currentCredential = projectCredentialDao.selectByPrimaryKey(
                    connId,
                    BeanUtils.getDefaultDeletedAt()
            );
            if (currentCredential == null) {
                throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_NOT_EXIST);
            }
            Credential credential = toBuildCredential(form);
            if (encrypt) {
                credentialRsaService.encrypt(credential);
            }
            if (form instanceof CredentialForm) {
                projectCredentialDao.updateByPrimaryKeySelective(credential);
            }
            if (form instanceof AndroidCredentialForm) {
                projectCredentialDao.updateBaseInfo(credential);
                AndroidCredential androidCredential = toBuildAndroidCredential((AndroidCredentialForm) form);
                androidCredential.setConnId(connId);

                AndroidCredential oldAndroidCredential = androidCredentialDao.getByConnId(
                        connId,
                        BeanUtils.getDefaultDeletedAt()
                );
                androidCredential.setId(oldAndroidCredential.getId());
                if (encrypt) {
                    credentialRsaService.encrypt(androidCredential);
                }
                androidCredentialDao.updateByPrimaryKeySelective(androidCredential);
            }
            if (form instanceof TencentServerlessCredentialForm) {
                projectCredentialDao.updateBaseInfo(credential);
                TencentServerlessCredentialForm.TencentServerlessCredentialRaw rawSlsCredential =
                        ((TencentServerlessCredentialForm) form).getRawSlsCredential();
                if (rawSlsCredential != null && !StringUtils.isBlank(rawSlsCredential.getUuid())) {
                    TencentServerlessCredential tencentServerlessCredential =
                            tencentServerlessCredentialsDao.getByConnId(
                                    connId,
                                    BeanUtils.getDefaultDeletedAt()
                            );
                    tencentServerlessCredential = toBuildTencentServerlessCredential(
                            tencentServerlessCredential,
                            rawSlsCredential
                    );
                    tencentServerlessCredentialsDao.update(tencentServerlessCredential);
                    String secretKey = tencentServerlessCredentialService.toJSON(
                            ((TencentServerlessCredentialForm) form).getRawSlsCredential()
                    );
                    projectCredentialDao.updateSecretKey(connId, secretKey, BeanUtils.getDefaultDeletedAt());
                }
            }
            return connId;
        } catch (Exception e) {
            log.error("Update Credential error {}", e.getMessage());
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_UPDATE_ERROR);
        }
    }

    public int createCredential(BaseCredentialForm form, boolean encrypt) {
        String uuid = UUID.randomUUID().toString();
        form.setCredentialId(uuid);
        Credential credential = toBuildCredential(form);
        if (encrypt) {
            credentialRsaService.encrypt(credential);
        }
        projectCredentialDao.insertSelective(credential);
        if (form instanceof AndroidCredentialForm) {
            AndroidCredential androidCredential = toBuildAndroidCredential((AndroidCredentialForm) form);
            androidCredential.setConnId(credential.getId());
            if (encrypt) {
                credentialRsaService.encrypt(androidCredential);
            }
            androidCredentialDao.insert(androidCredential);
        }
        if (form instanceof TencentServerlessCredentialForm) {
            TencentServerlessCredential tencentServerlessCredential =
                    toBuildTencentServerlessCredential((TencentServerlessCredentialForm) form);
            tencentServerlessCredential.setConnId(credential.getId());
            tencentServerlessCredentialsDao.insert(tencentServerlessCredential);
            String secretKey = tencentServerlessCredentialService.toJSON(
                    ((TencentServerlessCredentialForm) form).getRawSlsCredential()
            );
            projectCredentialDao.updateSecretKey(
                    credential.getId(),
                    secretKey,
                    BeanUtils.getDefaultDeletedAt()
            );
        }
        return credential.getId();
    }

    public int updateUsernamePassword(Credential credential) {
        return projectCredentialDao.updateUsernamePassword(credential);
    }

    public String showHiddenInfo(int id, int projectId) throws CoreException {
        Credential credential = get(id, projectId);
        CredentialType credentialType = CredentialType.valueOf(credential.getType());
        // ssh 的 private_key
        if (credentialType == CredentialType.SSH ||
                credentialType == CredentialType.SSH_TOKEN) {
            return credential.getPrivateKey();
        }
        // username 的 password
        if (credentialType == CredentialType.USERNAME_PASSWORD) {
            credentialRsaService.decrypt(credential);
            return credential.getPassword();
        }
        if (credentialType == CredentialType.APP_ID_SECRET_KEY) {
            return credential.getSecretKey();
        }
        // android 的证书密码
        if (credentialType == CredentialType.ANDROID_CERTIFICATE) {
            AndroidCredential androidCredential =
                    androidCredentialDao.getByConnId(id, BeanUtils.getDefaultDeletedAt());
            credentialRsaService.decrypt(androidCredential);
            return androidCredential.getFilePassword();
        }
        if (credentialType == CredentialType.TENCENT_SERVERLESS) {
            TencentServerlessCredential tencentCredential =
                    tencentServerlessCredentialsDao.getByConnId(id, BeanUtils.getDefaultDeletedAt());
            TencentServerlessCredentialForm.TencentServerlessCredentialRaw raw =
                    new TencentServerlessCredentialForm.TencentServerlessCredentialRaw();
            raw.setAppid(tencentCredential.getAppId());
            raw.setExpired(tencentCredential.getExpired());
            raw.setSecret_id(tencentCredential.getSecretId());
            raw.setSecret_key(tencentCredential.getSecretKey());
            raw.setSignature(tencentCredential.getSignature());
            raw.setToken(tencentCredential.getToken());
            raw.setUuid(tencentCredential.getUuid());
            return gson.toJson(raw);
        }
        throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_TYPE_INVALID);
    }


    public TencentServerlessCredential toBuildTencentServerlessCredential(
            TencentServerlessCredentialForm form
    ) {
        TencentServerlessCredential.TencentServerlessCredentialBuilder builder =
                TencentServerlessCredential.builder();
        if (form.isFake()) {
            builder.wasted(true);
            builder.appId(0L);
            builder.expired(0);
            return builder.build();
        } else {
            builder.appId(form.getRawSlsCredential().getAppid());
            builder.expired(form.getRawSlsCredential().getExpired());
            builder.secretId(form.getRawSlsCredential().getSecret_id());
            builder.secretKey(form.getRawSlsCredential().getSecret_key());
            builder.signature(form.getRawSlsCredential().getSignature());
            builder.token(form.getRawSlsCredential().getToken());
            builder.uuid(form.getRawSlsCredential().getUuid());
        }
        return builder.build();
    }

    public List<Credential> listByProjectAndUser(int projectId, int userId, boolean allSelect) {
        return projectCredentialDao.listByProjectAndUser(
                projectId,
                userId,
                allSelect,
                BeanUtils.getDefaultDeletedAt()
        );
    }

    public TencentServerlessCredential toBuildTencentServerlessCredential(
            TencentServerlessCredential serverlessCredential,
            TencentServerlessCredentialForm.TencentServerlessCredentialRaw raw
    ) {
        if (serverlessCredential == null || raw == null) {
            return null;
        }

        return TencentServerlessCredential.builder()
                .id(serverlessCredential.getId())
                .wasted(false)
                .appId(raw.getAppid())
                .expired(raw.getExpired())
                .secretId(raw.getSecret_id())
                .secretKey(raw.getSecret_key())
                .signature(raw.getSignature())
                .token(raw.getToken())
                .uuid(raw.getUuid())
                .build();
    }

    public AndroidCredential toBuildAndroidCredential(AndroidCredentialForm androidCredentialForm) {
        AndroidCredential.AndroidCredentialBuilder builder = AndroidCredential.builder();
        // cert
        builder.content(androidCredentialForm.getContent());

        try {
            byte[] contentBytes = Base64.decode(androidCredentialForm.getContent());
            builder.sha1(SHA1.bytesToSHA(contentBytes));
        } catch (UnsupportedEncodingException e) {
            builder.sha1(SHA1.stringToSHA(androidCredentialForm.getContent()));
            log.error(
                    "failed to decode android credential content:{},error{}",
                    androidCredentialForm.getContent(), e.getMessage()
            );
        }

        builder.fileName(androidCredentialForm.getFileName());
        builder.filePassword(androidCredentialForm.getFilePassword());
        // alias
        builder.alias(androidCredentialForm.getAlias());
        builder.aliasPassword(androidCredentialForm.getAliasPassword());
        return builder.build();
    }

    public Credential toBuildCredential(BaseCredentialForm baseCredentialForm) {
        CredentialType credentialType = CredentialType.valueOf(baseCredentialForm.getType());
        Credential.CredentialBuilder builder = Credential.builder();
        builder.type(credentialType.name())
                .id(baseCredentialForm.getId())
                .scope(baseCredentialForm.getScope())
                .name(baseCredentialForm.getName())
                .teamId(baseCredentialForm.getTeamId())
                .projectId(baseCredentialForm.getProjectId())
                .creatorId(baseCredentialForm.getCreatorId())
                .credentialId(baseCredentialForm.getCredentialId())
                .scheme(CredentialJenkinsScheme.None.value())
                .description(baseCredentialForm.getDescription())
                .allSelect(baseCredentialForm.isAllSelect())
                .secretKey(StringUtils.EMPTY)
                .username(StringUtils.EMPTY)
                .password(StringUtils.EMPTY)
                .verificationMethod(StringUtils.EMPTY)
                .clusterName(StringUtils.EMPTY)
                .url(StringUtils.EMPTY)
                .kubConfig(StringUtils.EMPTY)
                .privateKey(StringUtils.EMPTY)
                .generateBy(
                        Optional.ofNullable(baseCredentialForm.getConnGenerateBy())
                                .map(Enum::name).orElse(CredentialGenerated.MANUAL.name())
                );
        if (baseCredentialForm instanceof CredentialForm) {
            CredentialForm credentialForm = (CredentialForm) baseCredentialForm;
            String password = credentialForm.getPassword();
            switch (credentialType) {
                case PASSWORD:
                    builder.password(StringUtils.defaultString(password));
                    break;
                case CODING_PERSONAL_CREDENTIAL:
                case USERNAME_PASSWORD:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.password(StringUtils.defaultString(password));
                    builder.scheme(CredentialJenkinsScheme.UsernamePassword.value());
                    break;
                case OAUTH:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.scheme(CredentialJenkinsScheme.UsernamePassword.value());
                    builder.token(StringUtils.defaultString(credentialForm.getToken()));
                case TOKEN:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.token(StringUtils.defaultString(credentialForm.getToken()));
                    break;
                case SECRET_KEY:
                    builder.secretKey(StringUtils.defaultString(credentialForm.getSecretKey()));
                    break;
                case APP_ID_SECRET_KEY:
                    builder.appId(credentialForm.getAppId());
                    builder.secretId(credentialForm.getSecretId());
                    builder.secretKey(StringUtils.defaultString(credentialForm.getSecretKey()));
                    builder.scheme(CredentialJenkinsScheme.CloudApi.value());
                    break;
                case SSH:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.privateKey(StringUtils.defaultString(credentialForm.getPrivateKey()));
                    builder.password(StringUtils.defaultString(password));
                    builder.scheme(CredentialJenkinsScheme.SSHUserNameWithPrivateKey.value());
                    break;
                case SSH_TOKEN:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.privateKey(StringUtils.defaultString(credentialForm.getPrivateKey()));
                    builder.password(StringUtils.defaultString(password));
                    builder.token(StringUtils.defaultString(credentialForm.getToken()));
                    break;
                case USERNAME_PASSWORD_TOKEN:
                    builder.username(StringUtils.defaultString(credentialForm.getUsername()));
                    builder.password(StringUtils.defaultString(password));
                    builder.token(StringUtils.defaultString(credentialForm.getToken()));
                    break;
                case KUBERNETES:
                    VerificationMethodEnums verificationMethod =
                            VerificationMethodEnums.valueOf(
                                    credentialForm.getVerificationMethod()
                            );
                    builder.verificationMethod(StringUtils.defaultString(verificationMethod.name()));

                    // schema
                    if (verificationMethod == VerificationMethodEnums.Kubeconfig) {
                        builder.scheme(CredentialJenkinsScheme.SecretFile.value());
                    } else if (verificationMethod == VerificationMethodEnums.ServiceAccount) {
                        builder.scheme(CredentialJenkinsScheme.SecretText.value());
                    }

                    builder.kubConfig(StringUtils.defaultString(credentialForm.getKubConfig()));
                    builder.clusterName(StringUtils.defaultString(credentialForm.getClusterName()));
                    builder.acceptUntrustedCertificates(credentialForm.isAcceptUntrustedCertificates());
                    builder.url(StringUtils.defaultString(credentialForm.getUrl()));
                    builder.secretKey(credentialForm.getSecretKey());
                    break;
                default:
                    break;
            }
        }
        if (baseCredentialForm instanceof AndroidCredentialForm) {
            builder.scheme(CredentialJenkinsScheme.Certificate.value());
        }
        if (baseCredentialForm instanceof TencentServerlessCredentialForm) {
            builder.scheme(CredentialJenkinsScheme.SecretText.value());
        }
        return builder.build();
    }

    public int addCredential(Integer projectId, BaseCredentialForm baseForm) throws CoreException {
        Project project = getProject(projectId);
        baseForm.setProjectId(projectId);
        UserProto.User currentUser = getUser();
        baseForm.setCreatorId(currentUser.getId());
        baseForm.setTeamId(project.getTeamOwnerId());
        return createCredential(baseForm);
    }

    public int createCredential(BaseCredentialForm baseForm) throws CoreException {
        int credId = createCredential(baseForm, true);
        if (credId != 0) {
            projectCredentialTaskService.batchToggleTaskPermission(
                    baseForm.getProjectId(),
                    credId,
                    baseForm.getTaskDTOS()
            );
        }
        return credId;
    }

    public List<Credential> listByIds(List<Integer> ids, boolean decrypt) {
        List<Credential> credentials = projectCredentialDao.getByIds(ids, BeanUtils.getDefaultDeletedAt());
        credentials.stream()
                .map(this::extendCredential)
                .filter(credential -> decrypt)
                .forEach(credentialRsaService::decrypt);
        return credentials;
    }

    private Project getProject(Integer projectId) throws CoreException {
        Project project = projectDao.getProjectById(projectId);
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        return project;
    }

    public List<Credential> getByProjectIdAndGenerateBy(Integer projectId, String generateBy) {
        return projectCredentialDao.getByProjectIdAndGenerateBy(projectId, generateBy, BeanUtils.getDefaultDeletedAt());
    }

    private UserProto.User getUser() throws CoreException {
        UserProto.User currentUser = SystemContextHolder.get();
        if (currentUser == null) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        return currentUser;
    }

    private CredentialDTO toBuildCredentialDTO(Credential credential) {
        if (credential == null) {
            return new CredentialDTO();
        }
        return CredentialDTO.builder()
                .id(credential.getId())
                .projectId(credential.getProjectId())
                .name(credential.getName())
                .credentialId(credential.getCredentialId())
                .created_at(credential.getCreatedAt().getTime())
                .scheme(credential.getScheme())
                .url(credential.getUrl())
                .verificationMethod(credential.getVerificationMethod())
                .kubConfig(credential.getKubConfig())
                .clusterName(credential.getClusterName())
                .acceptUntrustedCertificates(credential.isAcceptUntrustedCertificates())
                .scope(credential.getScope())
                .state(credential.getState())
                .type(credential.getType())
                .username(credential.getUsername())
                .token(credential.getToken())
                .appId(credential.getAppId())
                .secretId(credential.getSecretId())
                .secretKey(!StringUtils.equals(
                                CredentialType.APP_ID_SECRET_KEY.toString(),
                                credential.getType()
                        ) ? credential.getSecretKey() : StringUtils.EMPTY
                )
                .description(credential.getDescription())
                .updated_at(credential.getUpdatedAt().getTime())
                .allSelect(credential.isAllSelect())
                .build();
    }

    public void validParam(Integer teamId, Integer projectId, String userGK) throws CoreException {
        TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(teamId);

        if (response == null || CodeProto.Code.SUCCESS != response.getCode()
                || ObjectUtils.isEmpty(response.getData())) {

            throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
        }
        Project project = projectDao.getProjectById(projectId);
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        UserProto.User user = userGrpcClient.getUserByGlobalKey(userGK);
        if (user == null) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_EXISTS);
        }
        boolean flag = projectMemberService.isMember(user, projectId);
        if (!flag) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
    }

    public List<Credential> getCredentialsByTaskIdAndGenerateBy(
            int projectId,
            int taskId,
            int taskType,
            String generateBy,
            boolean decrypt
    ) {
        return getCredentials(decrypt, projectCredentialDao.getCredentialsByTaskIdAndGenerateBy(
                projectId, taskId, taskType, generateBy, BeanUtils.getDefaultDeletedAt()
        ));
    }

    public List<Credential> getCredentialsByTaskIdAndType(
            int projectId,
            int taskId,
            int taskType,
            String type,
            boolean decrypt
    ) {
        return getCredentials(decrypt, projectCredentialDao.getCredentialsByTaskIdAndType(
                projectId,
                taskId,
                taskType,
                type,
                BeanUtils.getDefaultDeletedAt()
        ));
    }

    public List<Credential> getCredentialsByTaskId(
            int projectId,
            int taskId,
            int taskType,
            boolean decrypt
    ) {
        return getCredentials(decrypt, projectCredentialDao.getCredentialsByTaskId(
                projectId,
                taskId,
                taskType,
                BeanUtils.getDefaultDeletedAt()
        ));
    }

    @NotNull
    private List<Credential> getCredentials(boolean decrypt, List<Credential> credentials) {
        return credentials.stream().map(
                credential -> {
                    CredentialType credentialType = CredentialType.valueOf(credential.getType());
                    if (credentialType.equals(CredentialType.ANDROID_CERTIFICATE)) {
                        AndroidCredential androidCredential =
                                androidCredentialDao.getByConnId(credential.getId(), BeanUtils.getDefaultDeletedAt());
                        if (decrypt) {
                            credentialRsaService.decrypt(androidCredential);
                        }
                        credential.setAndroidCredential(androidCredential);
                    }
                    if (decrypt) {
                        credentialRsaService.decrypt(credential);
                    }
                    return credential;
                }
        ).collect(Collectors.toList());
    }


}
