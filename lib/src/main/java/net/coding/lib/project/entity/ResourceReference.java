package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * resource_references
 * @author 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceReference implements Serializable {
    private Integer id;

    private Integer selfId;

    private Integer selfProjectId;

    private Integer selfIid;

    private String selfType;

    private Integer targetId;

    private Integer targetProjectId;

    private Integer targetIid;

    private String targetType;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;

    private static final long serialVersionUID = 1000000000004L;
}