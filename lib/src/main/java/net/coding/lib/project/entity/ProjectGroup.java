package net.coding.lib.project.entity;


import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_groups")
public class ProjectGroup {
    public static final Integer NO_GROUP_ID = 0;

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    /**
     * 团队所有者Id
     */
    private Integer ownerId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 类型，全部项目ALL/未分组NO_GROUP/自定义CUSTOM
     */
    private String type;

    /**
     * 排序
     */
    private Integer sort;

    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;


    public enum TYPE{
        ALL,
        NO_GROUP,
        CUSTOM
    }

}
