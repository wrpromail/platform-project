package net.coding.lib.project.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployTokens implements Serializable {
    private Integer id;

    /**
     * 项目编号
     */
    private Integer projectId;

    /**
     * 创建人编号
     */
    private Integer creatorId;

    /**
     * 令牌名称
     */
    private String tokenName;

    /**
     * GK 编号
     */
    private Integer globalKeyId;

    /**
     * 密码
     */
    private String token;

    /**
     * 权限标识
     */
    private String scope;

    /**
     * 过期时间
     */
    private Date expiredAt;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private Short type;

    /**
     * 权限是否应用所有仓库，0-否，1-是
     */
    private Boolean applyToAllDepots;

    /**
     * 是否将此角色下的权限应用于项目下所有制品库，0-否，1-是
     */
    private Boolean applyToAllArtifacts;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 上次活动时间
     */
    private Timestamp lastActivityAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;

    private Timestamp deletedAt;

    private static final long serialVersionUID = 1L;

    public static final short TYPE_CODEDOG = 5;
    public static final short TYPE_QTA = 6;
    public static final short TYPE_QCI = 7;
    public static final String CODEDOG_TOKEN_NAME = "CODEDOG_AUTO_GEN";
    public static final String QTA_TOKEN_NAME = "QTA_AUTO_GEN";
    public static final String QCI_TOKEN_NAME = "QCI_AUTO_GEN";

}