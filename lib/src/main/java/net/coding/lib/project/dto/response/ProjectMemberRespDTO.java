package net.coding.lib.project.dto.response;


import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ProjectMemberRespDTO {

    @ApiModelProperty("权限组Id")
    private Long policyId;

    @ApiModelProperty("权限组名称")
    private String name;

    @ApiModelProperty("别名")
    private String alias;

    @ApiModelProperty("描述")
    private String description;

}
