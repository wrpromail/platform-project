package net.coding.lib.project.dto;

import net.coding.lib.project.entity.ProjectResource;

import lombok.Data;

/**
 * @author Jyong <jiangyong@coding.net> on @date 2021/8/21
 */
@Data
public class ResourceDTO {
    private Integer id;

    private Integer scopeId;

    //资源范围类型 1：project 2；team
    private Integer scopeType;

    private String targetType;

    private Integer targetId;

    private String code;

    private String title;

    private String resourceUrl;

    private String projectGK;

    public ResourceDTO(ProjectResource projectResource, String projectGK) {
        this.id = projectResource.getId();
        this.scopeId = projectResource.getProjectId();
        this.scopeType = projectResource.getScopeType();
        this.targetType = projectResource.getTargetType();
        this.targetId = projectResource.getTargetId();
        this.code = projectResource.getCode();
        this.title = projectResource.getTitle();
        this.resourceUrl = projectResource.getResourceUrl();
        this.projectGK = projectGK;
    }
}
