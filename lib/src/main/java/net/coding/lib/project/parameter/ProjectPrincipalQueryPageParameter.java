package net.coding.lib.project.parameter;

import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder(toBuilder = true)
@Accessors(chain = true)
public class ProjectPrincipalQueryPageParameter {
    private Integer teamId;
    private Set<Integer> projectIds;
    private String keyword;
}
