package net.coding.lib.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@ApiModel(value = "项目标签")
public class ProjectLabelDTO {

    @ApiModelProperty(value = "主键 ID ")
    private int id;

    @ApiModelProperty(value = "项目 ID ")
    private int project_id;

    @ApiModelProperty(value = "标签名称")
    private String name;

    @ApiModelProperty(value = "标签颜色")
    private String color;

    @ApiModelProperty(value = "标签创建人")
    private int owner_id;

    @ApiModelProperty(value = "标签类型")
    private Short type;

    @ApiModelProperty(value = "mergeRequest 数量 ")
    private long merge_request_count;
}
