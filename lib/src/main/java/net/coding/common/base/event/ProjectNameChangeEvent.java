package net.coding.common.base.event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProjectNameChangeEvent {
    private Integer projectId;
    private String newName;
}
