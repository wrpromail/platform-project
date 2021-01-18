package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * deploy_token_artifacts
 * @author 
 */
@Data
@Builder
public class DeployTokenArtifacts implements Serializable {
    private Integer id;

    /**
     * 项目令牌id
     */
    private Integer deployTokenId;

    /**
     * 制品库id
     */
    private Integer artifactId;

    /**
     * 权限范围
     */
    private String artifactScope;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 删除时间
     */
    private Date deletedAt;

    private static final long serialVersionUID = 1L;
}