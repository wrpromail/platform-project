package net.coding.lib.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "项目凭据关联任务")
public class ConnectionTaskDTO {
    
    @ApiModelProperty(value = "主键 id")
    private Integer id;
    @ApiModelProperty(value = "类型")
    private Integer type;
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "是否全部选择")
    private boolean selected;
}
