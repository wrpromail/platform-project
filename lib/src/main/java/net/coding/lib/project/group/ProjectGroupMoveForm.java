package net.coding.lib.project.group;

import lombok.Data;

@Data
public class ProjectGroupMoveForm {
    private String ids;
    private String excludeIds;
    private boolean selectAll = false;
    private Integer fromGroupId;
    private String keyword;
}
