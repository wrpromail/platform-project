package net.coding.lib.project.setting;

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
    @ApiModelProperty("域")
    private String scope;
    @ApiModelProperty("代码")
    private String code;
    @ApiModelProperty("菜单代码")
    private String featureCode;
    @ApiModelProperty("配置名")
    private String name;
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("默认值")
    private String defaultValue;
    @ApiModelProperty("排序")
    private String order;
}
