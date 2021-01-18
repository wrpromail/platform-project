package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Created by dengzhengping on 2019/6/14
 */
@Builder
@Data
public class ProjectFunctionDTO {

    private Integer projectId;
    private String code;
    private String value;
    private String description;

}
