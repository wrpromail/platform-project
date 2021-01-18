package net.coding.common.base.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
public class ProjectNameChangeEvent {
    private Integer projectId;
    private String newName;
}
