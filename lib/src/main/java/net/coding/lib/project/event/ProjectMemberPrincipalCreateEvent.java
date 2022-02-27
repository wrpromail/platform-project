package net.coding.lib.project.event;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectMemberPrincipalCreateEvent {

    private Integer teamId;

    private Integer operatorId;

    private Integer projectId;

    private List<Principal> principals;

}
