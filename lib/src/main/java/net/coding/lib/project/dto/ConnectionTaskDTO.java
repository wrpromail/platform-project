package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionTaskDTO {

    private Integer id;
    private Integer type;
    private String name;
    private boolean selected;

}
