package net.coding.lib.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author liwenqiu@coding.net
 */

@Data
@Builder
@ApiModel(value = "角色")
public class RoleDTO {

    @ApiModelProperty(value = "主键 ID ")
    private Integer roleId;

    @ApiModelProperty(value = "角色名称")
    private String name;

    @ApiModelProperty(value = "角色描述")
    private String description;

    @ApiModelProperty(value = "角色类型")
    private String roleType;

    @ApiModelProperty(value = "是否可编辑")
    private Boolean isRoleCanEdit;

    @ApiModelProperty(value = "是否可删除")
    private Boolean isRoleCanDelete;

    @ApiModelProperty(value = "是否有权限删除 ")
    private Boolean isPermissionCanEdit;

    @ApiModelProperty(value = "创建时间 ")
    private long createdAt;

    @ApiModelProperty(value = "角色下用户数量")
    private Integer memberCount;


}
