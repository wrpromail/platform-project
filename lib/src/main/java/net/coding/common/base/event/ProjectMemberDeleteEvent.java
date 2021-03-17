package net.coding.common.base.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User: Michael Chen Email: yidongnan@gmail.com Date: 14-4-4 Time: 11:36
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectMemberDeleteEvent {
    private Integer projectId;
    private Integer userId;
}
