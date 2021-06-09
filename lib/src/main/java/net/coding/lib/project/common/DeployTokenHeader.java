package net.coding.lib.project.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeployTokenHeader {
    private String teamId;
    private String userId;
    private String requestId;
    private String deployTokenId;
    private String deployTokenProjectId;

}