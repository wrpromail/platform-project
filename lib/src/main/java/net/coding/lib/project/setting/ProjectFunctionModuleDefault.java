package net.coding.lib.project.setting;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("项目功能按需选择定义")
public class ProjectFunctionModuleDefault {
    @ApiModelProperty("code")
    private String code;
    @ApiModelProperty("功能名")
    private String name;
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("功能开关code")
    private List<String> codes;
}
