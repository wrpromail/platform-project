package net.coding.lib.project.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder(toBuilder = true)
@ApiModel(value = "项目集查询响应信息")
@AllArgsConstructor
@NoArgsConstructor
public class ProgramDTO implements Serializable {

    @ApiModelProperty(value = "项目集Id")
    private Integer id;

    @ApiModelProperty(value = "项目集标识")
    private String name;

    @ApiModelProperty(value = "项目集名称")
    private String displayName;

    @ApiModelProperty("项目集描述")
    private String description;

    @ApiModelProperty(value = "封面")
    private String icon;

    @ApiModelProperty(value = "开始时间")
    private Date startDate;

    @ApiModelProperty(value = "结束时间")
    private Date endDate;

    @ApiModelProperty(value = "参与的项目")
    private List<ProjectDTO> projects;

    @ApiModelProperty(value = "负责人")
    private List<ProgramUserDTO> programUser;

}
