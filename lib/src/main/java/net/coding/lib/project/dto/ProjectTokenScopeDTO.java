package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectTokenScopeDTO {
    private String value;
    private String text;
}
