package net.coding.lib.project.form.credential;


import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class CredentialForm extends BaseCredentialForm {

    @Valid
    @Length(max = 255, message = "credential_username_too_long")
    private String username;

    /**
     * 密码/私钥口令 注意：RSA加密明文bytes最大长度有86限制，汉字占2bytes，故限制 String 长度小于40 v2 版本使用分块加密，理论上没有长度限制，设置为1000最大
     */
    @Valid
    @Length(max = 1000, message = "credential_password_too_long")
    private String password;

    @Valid
    @Length(max = 5000, message = "credential_private_key_too_long")
    private String privateKey;

    @Valid
    @Length(max = 255, message = "credential_token_too_long")
    private String token;

    private String verificationMethod;

    private String kubConfig;

    private String clusterName;

    private boolean acceptUntrustedCertificates;

    /**
     * 目前用于 k8s 服务连接地址
     */
    @Valid
    private String url;

    @Valid
    @Length(max = 255, message = "credential_app_id_too_long")
    private String appId;

    @Valid
    @Length(max = 255, message = "credential_secret_id_too_long")
    private String secretId;

    // 当初设计为 k8s 共用，不再适用于 255 的限制
    private String secretKey = "";
}