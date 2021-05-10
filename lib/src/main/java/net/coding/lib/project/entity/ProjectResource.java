package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(value = "项目资源")
public class ProjectResource implements Serializable {

    private static final long serialVersionUID = 1000000000002L;

    @ApiModelProperty(value = "主键 ID ")
    private Integer id;

    @ApiModelProperty(value = "项目 ID ")
    private Integer projectId;

    @ApiModelProperty(value = "删除时间")
    private Date deletedAt;

    @ApiModelProperty(value = "资源类型")
    private String targetType;

    @ApiModelProperty(value = "资源编号")
    private Integer targetId;

    @ApiModelProperty(value = "资源序号")
    private Integer code;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "创建时间")
    private Date createdAt;

    @ApiModelProperty(value = "创建人")
    private Integer createdBy;

    @ApiModelProperty(value = "修改时间")
    private Date updatedAt;

    @ApiModelProperty(value = "修改人")
    private Integer updatedBy;

    @ApiModelProperty(value = "删除人")
    private Integer deletedBy;

    @ApiModelProperty(value = "资源地址")
    private String resourceUrl;
}