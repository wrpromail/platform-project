package net.coding.lib.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProjectJoinMemberDTO {
    private Integer projectId;
    private String projectName;
    private String projectDisplayName;
    private Integer userId;
    private String userName;
    private String avatar;
}
