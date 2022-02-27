package net.coding.lib.project.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectJoinProjectMemberDTO {
    private Integer projectId;
    private String projectName;
    private String projectDisplayName;
    private String projectIcon;
    private List<Integer> userIds;
}
