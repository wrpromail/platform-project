package net.coding.common.base.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目成员用户组更改事件
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectMemberRoleChangeEvent {
    private int projectId;
    private int targetUserId;
    private int roleId;

    /**
     * 操作类型 1：添加用户组 -1：删除用户组
     */
    private int operate;
}
