package net.coding.lib.project.dto;


import net.coding.lib.project.entity.ProjectGroup;

import lombok.Data;

@Data
public class ProjectGroupDTO {
    private Integer ownerId;
    private String name;
    private Integer sort;
    private Integer id;
    private Long projectNum;
    private String type;


    public ProjectGroupDTO(ProjectGroup projectGroup, boolean needProjectNum, long projectNum) {
        id = projectGroup.getId();
        ownerId = projectGroup.getOwnerId();
        name = projectGroup.getName();
        sort = projectGroup.getSort();
        type = projectGroup.getType();
        if (needProjectNum) {
            this.projectNum = projectNum;
        }
    }

}
