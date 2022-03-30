package net.coding.common.base.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by jack on 14-4-4.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectDeleteEvent {
    private int teamId;
    private int projectId;
    private int userId;
}
