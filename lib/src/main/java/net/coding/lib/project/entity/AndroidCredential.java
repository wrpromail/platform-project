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
public class AndroidCredential implements Serializable {
    private static final long serialVersionUID = -7181348715699326027L;

    private Integer id;

    /**
     * ci_connections id
     */
    private Integer connId;

    /**
     * 证书 sha1 值
     */
    private String sha1;

    /**
     * 证书或密钥内容,Base64 编码的 String
     */
    private String content;

    /**
     * 证书文件名
     */
    private String fileName;

    /**
     * 证书密码
     */
    private String filePassword;

    /**
     * 证书别名
     */
    private String alias;

    /**
     * 别名密码
     */
    private String aliasPassword;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;
}