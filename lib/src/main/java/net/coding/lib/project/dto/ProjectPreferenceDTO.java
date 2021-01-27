package net.coding.lib.project.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chenxinyu
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class ProjectPreferenceDTO {

    /**
     * 编号.
     */
    private transient Integer id;

    /**
     * 项目编号.
     */
    private Integer projectId;

    /**
     * 偏好设置类型.
     */
    private Short type;

    /**
     * 偏好设置状态.
     */
    private Short status;

    /**
     * 创建时间.
     */
    private transient Date createdAt;

    /**
     * 更新时间.
     */
    private transient Date updatedAt;

    /**
     * 删除时间.
     */
    private transient Date deletedAt;

}
