package net.coding.lib.project.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectActionPermissionEnums {
    VIEW_PROJECT_MEMBER("viewProjectMember"),
    CREATE_PROJECT_MEMBER("createProjectMember"),
    DELETE_PROJECT_MEMBER("deleteProjectMember");

    private String action;

    public String getAction() {
        return this.action;
    }
}
