package net.coding.lib.project.dto.request;

import java.util.Set;

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
public class ProjectMemberAddProjectReqDTO {

    @ApiModelProperty(value = "项目Id", required = true)
    private Integer projectId;

    @ApiModelProperty(value = "策略ID", required = true)
    private Set<Long> policyIds;
}
