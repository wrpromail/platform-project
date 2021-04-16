package net.coding.lib.project.dto;

import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectTokenDTO {
    private Integer id;
    private Integer projectId;
    private Integer creatorId;
    private String tokenName;
    private String userName;
    private String token;
    private List<ProjectTokenScopeDTO> scopes;
    private boolean applyToAllDepots;
    private List<DepotScopeDTO> depotScopes;
    private boolean applyToAllArtifacts;
    private List<ArtifactScopeDTO> artifactScopes;
    private Date expiredAt;
    private boolean enabled;
    private long createdAt;
    private long lastActivityAt;
    private long updatedAt;
}
