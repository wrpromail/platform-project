package net.coding.lib.project.dto;

import net.coding.common.util.ApplicationHelper;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceLinkService;

import lombok.Data;

/**
 * @author Jyong <jiangyong@coding.net> on @date 2021/8/21
 */
@Data
public class ResourceDetailDTO {

    private static ProjectResourceLinkService projectResourceLinkService = ApplicationHelper.get(ProjectResourceLinkService.class);

    private Integer id;

    private Integer scopeId;

    //资源范围类型 1：project 2；team
    private Integer scopeType;

    private String scopeName;

    private String scopeAvatar;

    private String targetType;

    private Integer targetId;

    private String code;

    private String title;

    private String resourceUrl;

    public ResourceDetailDTO(ProjectResource projectResource) {
        this.id = projectResource.getId();
        this.scopeId = projectResource.getProjectId();
        this.scopeType = projectResource.getScopeType();
        this.targetType = projectResource.getTargetType();
        this.targetId = projectResource.getTargetId();
        this.code = projectResource.getCode();
        this.title = projectResource.getTitle();
        this.resourceUrl = projectResource.getResourceUrl();
    }

    public ResourceDetailDTO(ProjectResource projectResource, String projectGK) {
        this.id = projectResource.getId();
        this.scopeId = projectResource.getProjectId();
        this.scopeType = projectResource.getScopeType();
        this.targetType = projectResource.getTargetType();
        this.targetId = projectResource.getTargetId();
        this.code = projectResource.getCode();
        this.title = projectResource.getTitle();
        this.resourceUrl = projectResourceLinkService.getResourceLink(projectResource, projectGK != null ? "/p/"+projectGK : null);
    }
}
