package net.coding.lib.project.parameter;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ProjectPrincipalParameter {
    private String principalType;
    private Set<String> principalIds;
}
