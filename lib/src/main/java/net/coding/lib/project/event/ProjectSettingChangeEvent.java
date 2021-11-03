package net.coding.lib.project.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("项目设置项变更通知")
public class ProjectSettingChangeEvent {
    @ApiModelProperty("项目编号")
    private Integer projectId;
    @ApiModelProperty("团队编号")
    private Integer teamId;
    @ApiModelProperty("操作用户编号")
    private Integer operatorId;
    @ApiModelProperty("配置项")
    private String code;
    @ApiModelProperty("值（修改前，首次为空）")
    private String beforeValue;
    @ApiModelProperty("值（修改后）")
    private String afterValue;
}