package net.coding.lib.project.credential.entity;

import net.coding.lib.project.credential.enums.CredentialState;
import net.coding.lib.project.credential.enums.CredentialScope;
import net.coding.lib.project.entity.AndroidCredential;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Credential {
    private Integer id;
    @Builder.Default
    private int scope = CredentialScope.PROJECT.getCode();
    private String generateBy;
    private int teamId;
    private int projectId;
    private int creatorId;
    private String name;
    private String credentialId;

    /**
     * (旧)连接类型：SSH/HTTP_BASIC_AUTH/DOCKER_REGISTRY
     */
    private String type;

    /**
     * jenkins credential type
     */
    private int scheme;

    @Deprecated
    private String host;
    @Deprecated
    @Builder.Default
    private int port = 0;
    /**
     * k8s 有用到
     */
    private String url;

    private String username;
    /**
     * 密码/私钥口令
     */
    private String password;
    /**
     * 私钥
     */
    private String privateKey;
    private String token;
    private String appId;
    private String secretId;
    private String secretKey;
    /**
     * k8s 认证方式
     */
    private String verificationMethod;
    private String kubConfig;
    private String clusterName;
    /**
     * 是否接受非认证证书
     */
    private boolean acceptUntrustedCertificates;

    private String description;
    @Builder.Default
    private int state = CredentialState.Default.value();
    @Builder.Default
    private boolean allSelect = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

    private AndroidCredential androidCredential;
}
