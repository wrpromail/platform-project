package net.coding.lib.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectTokenKeyDTO {
    @Builder.Default
    private String globalKey = "";
    @Builder.Default
    private String token = "";
}
