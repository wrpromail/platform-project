package net.coding.lib.project.dto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "批量用户项目")
public class ProjectUserDTO {

    @ApiModelProperty(value = "用户Id")
    private Integer userId;

    @ApiModelProperty(value = "主体类型")
    private String principalType;

    @ApiModelProperty(value = "主体Id")
    private String principalId;

    @ApiModelProperty(value = "项目")
    private List<ProjectDTO> projects;
}
