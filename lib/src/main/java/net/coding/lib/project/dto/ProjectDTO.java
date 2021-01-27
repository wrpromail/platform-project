package net.coding.lib.project.dto;

import net.coding.common.util.TextUtils;
import net.coding.lib.project.entity.Project;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;

import javax.xml.ws.BindingType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by jack on 14-4-25.
 */
@Builder
@ApiModel(value = "项目")
public class ProjectDTO {

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "主键 ID ")
    private Integer id;

    @ApiModelProperty(value = "项目名称")
    private String name;

    @ApiModelProperty(value = "项目描述名称")
    private String display_name;

    @ApiModelProperty(value = "开始时间")
    private String start_date;

    @ApiModelProperty(value = "结束时间")
    private String end_date;

    @ApiModelProperty(value = "项目图标地址")
    private String icon;

}
