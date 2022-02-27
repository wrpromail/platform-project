package net.coding.lib.project.dto.request;

import java.util.List;

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
public class ProgramMemberJoinedProjectReqDTO {

    @ApiModelProperty(value = "成员主体信息", required = true)
    private List<ProjectMemberReqDTO> principals;
}
