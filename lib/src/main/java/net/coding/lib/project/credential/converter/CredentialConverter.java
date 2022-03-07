package net.coding.lib.project.credential.converter;

import com.google.common.collect.ImmutableMap;

import net.coding.lib.project.entity.AndroidCredential;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.credential.enums.CredentialGenerated;
import net.coding.lib.project.credential.enums.CredentialScope;
import net.coding.lib.project.credential.enums.CredentialType;
import net.coding.lib.project.enums.VerificationMethodEnums;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.proto.platform.project.ProjectCredentialProto;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public class CredentialConverter {

    private static final Map<ProjectCredentialProto.CredentialType, CredentialType> TYPE_MAP =
            new ImmutableMap.Builder<ProjectCredentialProto.CredentialType, CredentialType>()
                    .put(ProjectCredentialProto.CredentialType.PASSWORD, CredentialType.PASSWORD)
                    .put(ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TYPE, CredentialType.USERNAME_PASSWORD)
                    .put(ProjectCredentialProto.CredentialType.TOKEN, CredentialType.TOKEN)
                    .put(ProjectCredentialProto.CredentialType.SECRET_KEY, CredentialType.SECRET_KEY)
                    .put(ProjectCredentialProto.CredentialType.APP_ID_SECRET_KEY, CredentialType.APP_ID_SECRET_KEY)
                    .put(ProjectCredentialProto.CredentialType.SSH, CredentialType.SSH)
                    .put(ProjectCredentialProto.CredentialType.SSH_TOKEN, CredentialType.SSH_TOKEN)
                    .put(ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TOKEN, CredentialType.USERNAME_PASSWORD_TOKEN)
                    .put(ProjectCredentialProto.CredentialType.OAUTH, CredentialType.OAUTH)
                    .put(ProjectCredentialProto.CredentialType.KUBERNETES, CredentialType.KUBERNETES)
                    .put(ProjectCredentialProto.CredentialType.ANDROID_CERTIFICATE, CredentialType.ANDROID_CERTIFICATE)
                    .put(ProjectCredentialProto.CredentialType.IOS_CERTIFICATE, CredentialType.IOS_CERTIFICATE)
                    .put(ProjectCredentialProto.CredentialType.TLS_CERTIFICATE, CredentialType.TLS_CERTIFICATE)
                    .build();

    private static final Map<CredentialType, ProjectCredentialProto.CredentialType> MODEL_TO_PROTO_TYPE_MAP =
            new ImmutableMap.Builder<CredentialType, ProjectCredentialProto.CredentialType>()
                    .put(CredentialType.PASSWORD, ProjectCredentialProto.CredentialType.PASSWORD)
                    .put(CredentialType.USERNAME_PASSWORD, ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TYPE)
                    .put(CredentialType.TOKEN, ProjectCredentialProto.CredentialType.TOKEN)
                    .put(CredentialType.SECRET_KEY, ProjectCredentialProto.CredentialType.SECRET_KEY)
                    .put(CredentialType.APP_ID_SECRET_KEY, ProjectCredentialProto.CredentialType.APP_ID_SECRET_KEY)
                    .put(CredentialType.SSH, ProjectCredentialProto.CredentialType.SSH)
                    .put(CredentialType.SSH_TOKEN, ProjectCredentialProto.CredentialType.SSH_TOKEN)
                    .put(CredentialType.USERNAME_PASSWORD_TOKEN, ProjectCredentialProto.CredentialType.USERNAME_PASSWORD_TOKEN)
                    .put(CredentialType.OAUTH, ProjectCredentialProto.CredentialType.OAUTH)
                    .put(CredentialType.KUBERNETES, ProjectCredentialProto.CredentialType.KUBERNETES)
                    .put(CredentialType.ANDROID_CERTIFICATE, ProjectCredentialProto.CredentialType.ANDROID_CERTIFICATE)
                    .put(CredentialType.IOS_CERTIFICATE, ProjectCredentialProto.CredentialType.IOS_CERTIFICATE)
                    .put(CredentialType.TLS_CERTIFICATE, ProjectCredentialProto.CredentialType.TLS_CERTIFICATE)
                    .put(CredentialType.TENCENT_SERVERLESS, ProjectCredentialProto.CredentialType.TENCENT_SERVERLESS)
                    .put(CredentialType.CODING_PERSONAL_CREDENTIAL,ProjectCredentialProto.CredentialType.CODING_PERSONAL_CREDENTIAL)
                    .build();

    private static final Map<ProjectCredentialProto.CredentialScope, CredentialScope> SCOPE_MAP =
            new ImmutableMap.Builder<ProjectCredentialProto.CredentialScope, CredentialScope>()
                    .put(ProjectCredentialProto.CredentialScope.PROJECT, CredentialScope.PROJECT)
                    .put(ProjectCredentialProto.CredentialScope.PRIVATE, CredentialScope.PRIVATE)
                    .build();

    private static final Map<CredentialScope, ProjectCredentialProto.CredentialScope> MODEL_TO_PROTO_SCOPE_MAP =
            new ImmutableMap.Builder<CredentialScope, ProjectCredentialProto.CredentialScope>()
                    .put(CredentialScope.PROJECT, ProjectCredentialProto.CredentialScope.PROJECT)
                    .put(CredentialScope.PRIVATE, ProjectCredentialProto.CredentialScope.PRIVATE)
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
                .connGenerateBy(CredentialGenerated.valueOf(form.getGeneratedBy().name()))
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
                        CredentialScope.of(credential.getScope()))
                )
                .setType(MODEL_TO_PROTO_TYPE_MAP.get(
                        CredentialType.of(credential.getType()))
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

        if (CredentialType.ANDROID_CERTIFICATE.name().equals(credential.getType())) {
            AndroidCredential androidCredential = credential.getAndroidCredential();
            ProjectCredentialProto.AndroidCredential.Builder androidCredentialBuilder
                    = ProjectCredentialProto.AndroidCredential.newBuilder();
            androidCredentialBuilder
                    .setId(androidCredential.getId())
                    .setAlias(androidCredential.getAlias())
                    .setAliasPassword(androidCredential.getAliasPassword())
                    .setConnId(androidCredential.getConnId())
                    .setContent(androidCredential.getContent())
                    .setSha1(androidCredential.getSha1())
                    .setFileName(androidCredential.getFileName())
                    .setFilePassword(androidCredential.getFilePassword())
                    .setCreatedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getCreatedAt().getTime()))
                    .setDeletedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getDeletedAt().getTime()))
                    .setUpdatedAt(com.google.protobuf.util.Timestamps.fromMillis(credential.getUpdatedAt().getTime()));

            builder.setAndroidCredential(androidCredentialBuilder.build());
        }

        if (StringUtils.isNotEmpty(credential.getGenerateBy())) {
            builder.setGeneratedBy(
                    ProjectCredentialProto.ConnGenerateBy.valueOf(
                            CredentialGenerated.valueOf(credential.getGenerateBy()).name()
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
