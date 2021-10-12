package net.coding.lib.project.dto;


import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by liuying on 2021-01-28.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "项目成员")
public class ProjectMemberDTO {

    @ApiModelProperty(value = "主键 ID ")
    private int id;

    @ApiModelProperty(value = "项目 ID ")
    private int project_id;

    @ApiModelProperty(value = "成员 ID ")
    private int user_id;

    @ApiModelProperty(value = "成员类型 ")
    private Short type;

    @ApiModelProperty(value = "成员别名 ")
    private String alias;

    @ApiModelProperty(value = "团队别名 ")
    private String team_alias;

    @ApiModelProperty(value = "创建时间 ")
    private long created_at;

    @ApiModelProperty(value = "最近访问时间 ")
    private long last_visit_at;

    @ApiModelProperty(value = "成员信息 ")
    private UserDTO user;

    @ApiModelProperty(value = "项目信息 ")
    private ProjectDTO project;

    @ApiModelProperty(value = "成员角色信息 ")
    private List<RoleDTO> roles;


}
