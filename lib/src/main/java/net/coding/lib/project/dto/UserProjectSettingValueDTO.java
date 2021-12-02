package net.coding.lib.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@ApiModel(value = "用户项目设置")
@EqualsAndHashCode(callSuper = false)
public class UserProjectSettingValueDTO {
    @ApiModelProperty(value = "编码")
    private String code;

    @ApiModelProperty(value = "编码值")
    private String value;

    @ApiModelProperty(value = "文案信息")
    private String message;

}
