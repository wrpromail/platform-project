package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ProjectMember implements Serializable {

    private static final long serialVersionUID = 1000000000007L;

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
    private Date createdAt;

    /**
     * 删除时间
     */
    private Date deletedAt;

    /**
     * 最后访问时间
     */
    private Date lastVisitAt;

    /**
     * 成员备注
     */
    private String alias;
}
