package net.coding.lib.project.dto;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chenxinyu
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "项目偏好")
public final class ProjectPreferenceDTO {

    @ApiModelProperty(value = "主键 ID ")
    private transient Integer id;

    @ApiModelProperty(value = "项目 ID ")
    private Integer projectId;

    @ApiModelProperty(value = "偏好设置类型")
    private Short type;

    @ApiModelProperty(value = "偏好设置状态")
    private Short status;

    @ApiModelProperty(value = "创建时间")
    private transient Date createdAt;

    @ApiModelProperty(value = "更新时间")
    private transient Date updatedAt;

    @ApiModelProperty(value = "删除时间")
    private transient Date deletedAt;

}
