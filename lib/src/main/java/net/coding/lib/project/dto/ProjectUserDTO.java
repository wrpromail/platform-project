package net.coding.lib.project.dto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "批量用户项目")
public class ProjectUserDTO {

    private Integer userId;
    private List<ProjectDTO> projects;
}
