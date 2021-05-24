package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TencentServerlessCredential implements Serializable {
    private static final long serialVersionUID = -5135017676815380415L;
    private Integer id;

    private Integer connId;

    /**
     * 腾讯云 secret id
     */
    private String secretId;

    /**
     * 腾讯云 secret key
     */
    private String secretKey;

    /**
     * 腾讯云 token 这三个字段一起可以验证身份
     */
    private String token;

    /**
     * 腾讯云 appId，代表一个人的身份
     */
    private Long appId;

    /**
     * 更新临时秘钥的签名
     */
    private String signature;

    /**
     * 超时的 unix time，更新临时秘钥使用
     */
    private Integer expired;

    /**
     * 更新临时秘钥的签名
     */
    private String uuid;

    /**
     * 是否已无用：超时且无法刷新
     */
    private Boolean wasted;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;

}