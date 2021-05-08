package net.coding.lib.project.entity;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.utils.DateUtil;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_groups")
public class ProjectGroup {

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

    @Builder.Default
    private Date createdAt = DateUtil.getCurrentDate();
    @Builder.Default
    private Date updatedAt = DateUtil.getCurrentDate();
    @Builder.Default
    private Date deletedAt = DateUtil.strToDate(BeanUtils.NOT_DELETED_AT);

}
