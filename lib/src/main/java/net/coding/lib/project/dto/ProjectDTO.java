package net.coding.lib.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;


/**
 * Created by jack on 14-4-25.
 */
@Data
@Builder
@ApiModel(value = "项目")
public class ProjectDTO {
    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "主键 ID ")
    private Integer id;

    @ApiModelProperty(value = "项目名称")
    private String name;

    @ApiModelProperty(value = "项目名称（拼音）")
    private String name_pinyin;

    @ApiModelProperty(value = "项目描述名称")
    private String display_name;

    @ApiModelProperty(value = "开始时间")
    private String start_date;

    @ApiModelProperty(value = "结束时间")
    private String end_date;

    @ApiModelProperty(value = "项目图标地址")
    private String icon;

    @ApiModelProperty(value = "0 项目 / 1 项目集")
    private Integer pmType;

    @ApiModelProperty(value = "项目 URL")
    private String project_path;

    @ApiModelProperty(value = "团队域名")
    public String owner_user_name;

    @ApiModelProperty(value = "是否是成员")
    public boolean is_member;

    @ApiModelProperty(value = "是否已归档")
    public boolean archived;

    @ApiModelProperty(value = "是否是 demo 项目")
    public boolean isDemo;

    @ApiModelProperty(value = "是否对外不可见")
    public boolean invisible;

    @ApiModelProperty(value = "成员数")
    public Integer memberCount;

    @ApiModelProperty(value = "星标")
    private Boolean pin;

    @ApiModelProperty(value = "未读的项目动态数")
    private Integer un_read_activities_count;

    @ApiModelProperty(value = "创建时间")
    public Timestamp created_at;

    @JsonIgnore
    @ApiModelProperty(value = "删除时间", hidden = true)
    public Timestamp deleted_at;
}
