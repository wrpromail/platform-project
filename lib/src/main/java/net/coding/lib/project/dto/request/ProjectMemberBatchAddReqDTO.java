package net.coding.lib.project.dto.request;


import java.util.List;

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
public class ProjectMemberBatchAddReqDTO {
    private List<ProjectMemberReqDTO> principals;

    private List<ProjectMemberAddProjectReqDTO> projects;

}
