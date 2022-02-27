package net.coding.lib.project.parameter;

import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class ProjectPrincipalJoinedQueryParameter {
    private Integer teamId;
    private Integer userId;
    private String keyword;
    private Set<Integer> joinedProjectIds;
}
