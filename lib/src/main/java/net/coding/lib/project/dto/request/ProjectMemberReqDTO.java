package net.coding.lib.project.dto.request;

import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;

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
public class ProjectMemberReqDTO {
    @ApiModelProperty(value = "主体类型", required = true)
    private ProjectMemberPrincipalTypeEnum principalType;

    @ApiModelProperty(value = "主体Id", required = true)
    private String principalId;
}
