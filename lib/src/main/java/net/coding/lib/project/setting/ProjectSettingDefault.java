package net.coding.lib.project.setting;

import javax.validation.constraints.NotNull;

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
@ApiModel("项目配置定义")
public class ProjectSettingDefault {
    @NotNull
    @ApiModelProperty("域")
    private String scope;
    @NotNull
    @ApiModelProperty("代码")
    private String code;
    @ApiModelProperty("配置名")
    private String name;
    @NotNull
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("默认值")
    @NotNull
    private String defaultValue;
    @ApiModelProperty("排序")
    @NotNull
    private String order;
}
