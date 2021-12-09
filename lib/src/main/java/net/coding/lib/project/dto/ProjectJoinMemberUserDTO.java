package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectJoinMemberUserDTO {
    private Integer userId;
    private String userName;
    private String avatar;
}
