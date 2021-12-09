package net.coding.lib.project.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部门类型切换事件
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTypeChangeEvent {
    /**
     * 团队Id
     */
    private Integer teamId;

    /**
     * 操作人
     */
    private Integer operator;
    /**
     * 之前的部门类型
     */
    private DepartmentType before;
    /**
     * 之后的部门类型
     */
    private DepartmentType now;

    /**
     * 部门中所有的成员
     */
    private List<Integer> userIds;
}
