package net.coding.lib.project.entity;

import java.sql.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResources {
    private Integer id;
    private Integer projectId;
    private String targetType;
    private Integer targetId;
    private Integer code;
    private String title;
    private Integer resourcesDeleted;
    private Date createdAt;
    private Integer createdBy;
    private Date updatedAt;
    private Integer updatedBy;
    private Date deletedAt;
    private Integer deletedBy;
}