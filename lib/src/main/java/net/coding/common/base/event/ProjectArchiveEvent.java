package net.coding.common.base.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目归档，把deleted_at设置为1990-01-01 00:00:00 Created by zhengkenghong on 11/3/15.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectArchiveEvent {
    private Integer teamId;
    private Integer projectId;
    private Integer ownerId;
}
