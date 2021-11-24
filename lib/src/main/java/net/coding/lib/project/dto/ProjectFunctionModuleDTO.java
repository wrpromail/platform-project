package net.coding.lib.project.dto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@ApiModel(value = "项目功能模块")
public class ProjectFunctionModuleDTO {
    @ApiModelProperty("code")
    private String code;
    @ApiModelProperty("功能名")
    private String name;
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("功能开关code")
    private List<String> codes;
}
