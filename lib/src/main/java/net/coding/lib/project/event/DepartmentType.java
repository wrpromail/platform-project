package net.coding.lib.project.event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DepartmentType {
    private Short type;
    private Short typeExt;
    private Integer version;
}
