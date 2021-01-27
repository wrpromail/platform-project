package net.coding.e.lib.core.bean;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * project_members
 *
 * @author
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMember implements Serializable {
    public static final Short ACTION_ADD_MEMBER = 1;
    public static final Short ACTION_REMOVE_MEMBER = 2;
    public static final Short ACTION_QUIT = 3;
    /**
     * 编号
     */
    private Integer id;

    /**
     * 项目编号
     */
    private Integer projectId;

    /**
     * 用户编号
     */
    private Integer userId;

    /**
     * 类型
     */
    private Short type;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 删除时间
     */
    private Timestamp deletedAt;

    /**
     * 最后访问时间
     */
    private Timestamp lastVisitAt;

    /**
     * 成员备注
     */
    private String alias;

    private static final long serialVersionUID = 1L;
}