package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectResource implements Serializable {

    private static final long serialVersionUID = 1000000000002L;

    /**
     * 编号
     */
    private Integer id;

    private Integer projectId;

    /**
     * 删除时间
     */
    private Date deletedAt;

    /**
     * 资源类型
     */
    private String targetType;

    /**
     * 资源编号
     */
    private Integer targetId;

    /**
     * 资源序号
     */
    private Integer code;

    private String title;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 创建人
     */
    private Integer createdBy;

    /**
     * 修改时间
     */
    private Date updatedAt;

    /**
     * 修改人
     */
    private Integer updatedBy;

    /**
     * 删除人
     */
    private Integer deletedBy;

    /**
     * 资源地址
     */
    private String resourceUrl;
}