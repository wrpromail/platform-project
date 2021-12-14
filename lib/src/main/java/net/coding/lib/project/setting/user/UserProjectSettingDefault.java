package net.coding.lib.project.setting.user;

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
@ApiModel("用户项目配置定义")
public class UserProjectSettingDefault {

    @ApiModelProperty("代码")
    private String code;
    @ApiModelProperty("配置名")
    private String name;
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("默认值")
    private String defaultValue;
}
