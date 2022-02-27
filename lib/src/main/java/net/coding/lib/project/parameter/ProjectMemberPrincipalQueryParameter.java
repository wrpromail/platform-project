package net.coding.lib.project.parameter;

import java.sql.Timestamp;
import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class ProjectMemberPrincipalQueryParameter {
    private Integer teamId;
    private Integer userId;
    private String principalType;
    private Set<String> principalIds;
    private Integer pmType;
    private Timestamp deletedAt;
    private Timestamp archivedAt;
}
