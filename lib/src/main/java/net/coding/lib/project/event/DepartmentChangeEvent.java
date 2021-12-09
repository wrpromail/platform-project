package net.coding.lib.project.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentChangeEvent {

    /**
     * teamId
     */
    private Integer teamId;

    /**
     * 操作人
     */
    private Integer operator;

    /**
     * 部门id
     */
    private Integer id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门描述id
     */
    private String describeId;

    /**
     * 部门变化类型
     */
    private ChangeType changeType;

    /**
     * 部门中的用户变化
     */
    private List<DepartmentUserChange> userChanges;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepartmentUserChange {

        /**
         * 用户id
         */
        private int userId;

        /**
         * 成员名称
         */
        private String name;

        /**
         * 用户refId
         */
        private int refId;

        /**
         * 用户的改变类型
         */
        private ChangeType changeType;


    }


    public enum ChangeType {
        /**
         * 新增 （用户、部门）
         */
        ADDED,
        /**
         * 删除 （用户、部门）
         */
        DELETED,
        /**
         * 用户变化 （部门）
         */
        USER_CHANGE
    }


}