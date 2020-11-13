package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ResourceReferenceCommentRelation implements Serializable {

    private Long id;

    /**
     * 资源项目ID
     */
    private Integer projectId;

    /**
     * 资源关联表ID
     */
    private Integer resourceReferenceId;

    /**
     * 资源类型，ISSUE
     */
    private String resourceType;

    /**
     * 摘引来源，COMMENT/DESCRIPTION
     */
    private String citedSource;

    private Integer commentId;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;

    private static final long serialVersionUID = 1604903897298L;
}
