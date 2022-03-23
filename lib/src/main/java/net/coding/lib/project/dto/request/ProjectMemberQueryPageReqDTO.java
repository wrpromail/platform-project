package net.coding.lib.project.dto.request;

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
public class ProjectMemberQueryPageReqDTO {
    @ApiModelProperty(value = "团队Id")
    private Integer teamId;
    @ApiModelProperty(value = "操作人Id")
    private Integer userId;
    @ApiModelProperty(value = "项目Id")
    private Integer projectId;
    @ApiModelProperty(value = "权限组Id")
    private Long policyId;
    @ApiModelProperty(value = "关键字搜索")
    private String keyword;
}
