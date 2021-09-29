package net.coding.lib.project.dto;

import net.coding.lib.project.entity.ProjectResource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProjectResourceDTO {

    private Integer targetProjectId;

    private String targetProjectName;

    private String targetProjectDisplayName;

    private String code;

    private String targetType;

    private Integer targetId;

    private String title;

    private String link;

    private String img;

    private Integer status;

    private Boolean hasCommentRelated;

    public ProjectResourceDTO(ProjectResource projectResource, String projectName, String projectDisplayName) {
        if (null == projectResource) {
            return;
        }
        targetProjectId = projectResource.getProjectId();
        targetProjectName = projectName;
        targetProjectDisplayName = projectDisplayName;
        code = projectResource.getCode();
        targetType = projectResource.getTargetType();
        targetId = projectResource.getTargetId();
        title = projectResource.getTitle();
        link = projectResource.getResourceUrl();
        status = 0;//此字段无用
    }
}
