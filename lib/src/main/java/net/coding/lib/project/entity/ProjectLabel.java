package net.coding.lib.project.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;


@Data
@Table(name = "project_labels")
public class ProjectLabel {

    /**
     * 新增标签.
     */
    public static final Short ACTION_CREATE_LABEL = 1;
    /**
     * 修改标签.
     */
    public static final Short ACTION_UPDATE_LABEL = 2;
    /**
     * 删除标签.
     */
    public static final Short ACTION_DELETE_LABEL = 3;

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    private Integer projectId;

    private String name;

    private String color;

    private Integer ownerId;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp deletedAt;


}
