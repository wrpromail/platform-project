package net.coding.lib.project.converter;

import com.google.common.collect.ImmutableMap;

import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.enums.CredentialScopeEnums;
import net.coding.lib.project.enums.CredentialTypeEnums;
import net.coding.lib.project.enums.VerificationMethodEnums;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.proto.platform.project.ProjectCredentialProto;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public class CredentialConverter {

    private static final Map<ProjectCredentialProto.CredentialType, CredentialTypeEnums> TYPE_MAP =
            new ImmutableMap.Builder<ProjectCredentialProto.CredentialType, CredentialTypeEnums>()
                    .put(ProjectCredentialProto.CredentialType.PASSWORD, CredentialTypeEnums.PASSWORD)
                    .put(ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TYPE, CredentialTypeEnums.USERNAME_PASSWORD)
                    .put(ProjectCredentialProto.CredentialType.TOKEN, CredentialTypeEnums.TOKEN)
                    .put(ProjectCredentialProto.CredentialType.SECRET_KEY, CredentialTypeEnums.SECRET_KEY)
                    .put(ProjectCredentialProto.CredentialType.APP_ID_SECRET_KEY, CredentialTypeEnums.APP_ID_SECRET_KEY)
                    .put(ProjectCredentialProto.CredentialType.SSH, CredentialTypeEnums.SSH)
                    .put(ProjectCredentialProto.CredentialType.SSH_TOKEN, CredentialTypeEnums.SSH_TOKEN)
                    .put(ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TOKEN, CredentialTypeEnums.USERNAME_PASSWORD_TOKEN)
                    .put(ProjectCredentialProto.CredentialType.OAUTH, CredentialTypeEnums.OAUTH)
                    .put(ProjectCredentialProto.CredentialType.KUBERNETES, CredentialTypeEnums.KUBERNETES)
                    .put(ProjectCredentialProto.CredentialType.ANDROID_CERTIFICATE, CredentialTypeEnums.ANDROID_CERTIFICATE)
                    .put(ProjectCredentialProto.CredentialType.IOS_CERTIFICATE, CredentialTypeEnums.IOS_CERTIFICATE)
                    .put(ProjectCredentialProto.CredentialType.TLS_CERTIFICATE, CredentialTypeEnums.TLS_CERTIFICATE)
                    .build();

    private static final Map<CredentialTypeEnums, ProjectCredentialProto.CredentialType> MODEL_TO_PROTO_TYPE_MAP =
            new ImmutableMap.Builder<CredentialTypeEnums, ProjectCredentialProto.CredentialType>()
                    .put(CredentialTypeEnums.PASSWORD, ProjectCredentialProto.CredentialType.PASSWORD)
                    .put(CredentialTypeEnums.USERNAME_PASSWORD, ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TYPE)
                    .put(CredentialTypeEnums.TOKEN, ProjectCredentialProto.CredentialType.TOKEN)
                    .put(CredentialTypeEnums.SECRET_KEY, ProjectCredentialProto.CredentialType.SECRET_KEY)
                    .put(CredentialTypeEnums.APP_ID_SECRET_KEY, ProjectCredentialProto.CredentialType.APP_ID_SECRET_KEY)
                    .put(CredentialTypeEnums.SSH, ProjectCredentialProto.CredentialType.SSH)
                    .put(CredentialTypeEnums.SSH_TOKEN, ProjectCredentialProto.CredentialType.SSH_TOKEN)
                    .put(CredentialTypeEnums.USERNAME_PASSWORD_TOKEN, ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TOKEN)
                    .put(CredentialTypeEnums.OAUTH, ProjectCredentialProto.CredentialType.OAUTH)
                    .put(CredentialTypeEnums.KUBERNETES, ProjectCredentialProto.CredentialType.KUBERNETES)
                    .put(CredentialTypeEnums.ANDROID_CERTIFICATE, ProjectCredentialProto.CredentialType.ANDROID_CERTIFICATE)
                    .put(CredentialTypeEnums.IOS_CERTIFICATE, ProjectCredentialProto.CredentialType.IOS_CERTIFICATE)
                    .put(CredentialTypeEnums.TLS_CERTIFICATE, ProjectCredentialProto.CredentialType.TLS_CERTIFICATE)
                    .put(CredentialTypeEnums.TENCENT_SERVERLESS, ProjectCredentialProto.CredentialType.TENCENT_SERVERLESS)
                    .build();

    private static final Map<ProjectCredentialProto.CredentialScope, CredentialScopeEnums> SCOPE_MAP =
            new ImmutableMap.Builder<ProjectCredentialProto.CredentialScope, CredentialScopeEnums>()
                    .put(ProjectCredentialProto.CredentialScope.PROJECT, CredentialScopeEnums.PROJECT)
                    .put(ProjectCredentialProto.CredentialScope.PRIVATE, CredentialScopeEnums.PRIVATE)
                    .build();

    private static final Map<CredentialScopeEnums, ProjectCredentialProto.CredentialScope> MODEL_TO_PROTO_SCOPE_MAP =
            new ImmutableMap.Builder<CredentialScopeEnums, ProjectCredentialProto.CredentialScope>()
                    .put(CredentialScopeEnums.PROJECT, ProjectCredentialProto.CredentialScope.PROJECT)
                    .put(CredentialScopeEnums.PRIVATE, ProjectCredentialProto.CredentialScope.PRIVATE)
                    .build();

    private static final Map<ProjectCredentialProto.VerificationMethod, VerificationMethodEnums> VERIFICATION_METHOD_MAP =
            new ImmutableMap.Builder<ProjectCredentialProto.VerificationMethod, VerificationMethodEnums>()
                    .put(ProjectCredentialProto.VerificationMethod.KUBECONFIG, VerificationMethodEnums.Kubeconfig)
                    .put(ProjectCredentialProto.VerificationMethod.SERVICE_ACCOUNT, VerificationMethodEnums.ServiceAccount)
                    .build();

    private static final Map<VerificationMethodEnums, ProjectCredentialProto.VerificationMethod> MODEL_TO_PROTO_VERIFICATION_METHOD_MAP =
            new ImmutableMap.Builder<VerificationMethodEnums, ProjectCredentialProto.VerificationMethod>()
                    .put(VerificationMethodEnums.Kubeconfig, ProjectCredentialProto.VerificationMethod.KUBECONFIG)
                    .put(VerificationMethodEnums.ServiceAccount, ProjectCredentialProto.VerificationMethod.SERVICE_ACCOUNT)
                    .build();


    public static CredentialForm builderCredentialForm(ProjectCredentialProto.CredentialForm form) {
        return CredentialForm.builder()
                .id(form.getId())
                .teamId(form.getTeamId())
                .projectId(form.getProjectId())
                .creatorId(form.getCreatorId())
                .credentialId(form.getCredentialId())
                .type(TYPE_MAP.get(form.getType()).toString())
                .name(form.getName())
                .description(form.getDescription())
                .allSelect(form.getAllSelect())
                .connGenerateBy(ConnGenerateByEnums.valueOf(form.getGeneratedBy().name()))
                .scope(SCOPE_MAP.get(form.getScope()).getCode())
                .username(form.getUsername())
                .password(form.getPassword())
                .token(form.getToken())
                .secretId(form.getSecretId())
                .secretKey(form.getSecretKey())
                .appId(form.getAppId())
                .privateKey(form.getPrivateKey())
                .verificationMethod(VERIFICATION_METHOD_MAP.get(form.getVerificationMethod()).toString())
                .kubConfig(form.getKubConfig())
                .clusterName(form.getClusterName())
                .acceptUntrustedCertificates(form.getAcceptUntrustedCertificates())
                .url(form.getUrl())
                .build();
    }

    public static ProjectCredentialProto.Credential toBuildCredential(Credential credential) {
        ProjectCredentialProto.Credential.Builder builder =
                ProjectCredentialProto.Credential.newBuilder();
        if (credential == null) {
            return null;
        }
        builder.setId(Optional.ofNullable(credential.getId()).orElse(0))
                .setName(StringUtils.defaultString(credential.getName()))
                .setTeamId(Optional.of(credential.getTeamId()).orElse(0))
                .setProjectId(Optional.of(credential.getProjectId()).orElse(0))
                .setScope(MODEL_TO_PROTO_SCOPE_MAP.get(
                        CredentialScopeEnums.of(credential.getScope()))
                )
                .setType(MODEL_TO_PROTO_TYPE_MAP.get(
                        CredentialTypeEnums.of(credential.getType()))
                )
                .setCreatorId(credential.getCreatorId())
                .setCredentialId(StringUtils.defaultString(credential.getCredentialId()))
                .setScheme(Optional.of(credential.getScheme()).orElse(0))
                .setUsername(StringUtils.defaultString(credential.getUsername()))
                .setPassword(StringUtils.defaultString(credential.getPassword()))
                .setPrivateKey(StringUtils.defaultString(credential.getPrivateKey()))
                .setToken(StringUtils.defaultString(credential.getToken()))
                .setAppId(StringUtils.defaultString(credential.getAppId()))
                .setSecretId(StringUtils.defaultString(credential.getSecretId()))
                .setSecretKey(StringUtils.defaultString(credential.getSecretKey()))
                .setDescription(StringUtils.defaultString(credential.getDescription()))
                .setKubeType(StringUtils.defaultString(credential.getVerificationMethod()))
                .setKubeConfig(StringUtils.defaultString(credential.getKubConfig()))
                .setKubeUrl(StringUtils.defaultString(credential.getUrl()))
                .setClusterName(StringUtils.defaultString(credential.getClusterName()))
                .setAcceptUntrustedCertificates(credential.isAcceptUntrustedCertificates())
                .setState(credential.getState())
                .setAllSelect(credential.isAllSelect())
                .setCreatedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getCreatedAt().getTime()))
                .setUpdatedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getUpdatedAt().getTime()))
                .setDeletedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getDeletedAt().getTime()));
        if (StringUtils.isNotEmpty(credential.getGenerateBy())) {
            builder.setGeneratedBy(
                    ProjectCredentialProto.ConnGenerateBy.valueOf(
                            ConnGenerateByEnums.valueOf(credential.getGenerateBy()).name()
                    )
            );
        }
        if (StringUtils.isNotEmpty(credential.getVerificationMethod())) {
            builder.setVerificationMethod(
                    MODEL_TO_PROTO_VERIFICATION_METHOD_MAP.get(
                            VerificationMethodEnums.valueOf(credential.getVerificationMethod())
                    )
            );
        }
        return builder.build();
    }
}
