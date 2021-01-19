package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProjectLabelDTO {

    private int id;
    private int project_id;
    private String name;
    private String color;
    private int owner_id;
    private Short type;
    private long merge_request_count;
}
