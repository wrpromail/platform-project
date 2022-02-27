package net.coding.lib.project.dto.response;


import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class ProjectMemberQueryPageRespDTO {

    @ApiModelProperty("主体类型")
    private String principalType;

    @ApiModelProperty("主体ID")
    private String principalId;

    @ApiModelProperty("主体名称")
    private String principalName;

    @JsonIgnore
    @ApiModelProperty("主体排序")
    private Integer principalSort;

    @JsonIgnore
    @ApiModelProperty("加入时间")
    private Long createdAt;

    @ApiModelProperty("头像")
    private String avatar;
}
