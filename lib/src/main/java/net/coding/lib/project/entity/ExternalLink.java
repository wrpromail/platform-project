package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ExternalLink implements Serializable {

    private static final long serialVersionUID = 1604385041173L;

    /**
     * id
     */
    private Integer id;

    /**
     * 创建人编号
     */
    private Integer creatorId;

    /**
     * 项目编号
     */
    private Integer projectId;

    /**
     * 标题
     */
    private String title;

    /**
     * 链接
     */
    private String link;

    private Integer iid;

    /**
     * 创建日期
     */
    private Date createdAt;

    /**
     * 更新日期
     */
    private Date updatedAt;

    private Date deletedAt;
}
