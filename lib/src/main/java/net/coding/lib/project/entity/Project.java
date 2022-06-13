package net.coding.lib.project.entity;

import net.coding.common.constants.ProjectConstants;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.utils.DateUtil;

import java.io.Serializable;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "projects")
public class Project implements Serializable, Cloneable {
    private static final long serialVersionUID = 1000000000001L;
    /**
     * 编号
     */
    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    /**
     * 项目所有者ID，这个字段需要删掉
     */
    @Builder.Default
    private Integer ownerId = 0;

    /**
     * 创建时间
     */
    @Builder.Default
    private Date createdAt = DateUtil.getCurrentDate();

    /**
     * 更新时间
     */
    @Builder.Default
    private Date updatedAt = DateUtil.getCurrentDate();
    /**
     * 删除时间
     */
    @Builder.Default
    private Date deletedAt = DateUtil.strToDate(BeanUtils.NOT_DELETED_AT);

    /**
     * 项目状态
     */
    @Builder.Default
    private Short status = 1;

    private Short recommended;

    /**
     * 是否公开仓库源代码
     */
    @Builder.Default
    private Boolean depotShared = false;

    @Builder.Default
    private Integer type = 0;

    @Builder.Default
    private Integer maxMember = 10;

    /**
     * 名称
     */
    private String name;

    /**
     * 项目名拼音（混合版）
     */
    private String namePinyin;

    /**
     * 用于显示的名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 项目图标
     */
    private String icon;

    /**
     * 版本
     */
    @Builder.Default
    private Short plan = ProjectConstants.PLAN_FREE;

    /**
     * 项目所属团队ID
     */
    private Integer teamOwnerId;

    /**
     * 项目所属用户ID
     */
    @Builder.Default
    private Integer userOwnerId = 0;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 完成日期
     */
    private Date endDate;

    /**
     * 项目文件额定容量 (单位兆)
     */
    private Integer projectFileQuota;

    /**
     * 是否隐藏
     */
    @Builder.Default
    private Boolean invisible = false;

    /**
     * 标签
     */
    private String label;

    /**
     * 0 项目 / 1 项目集
     */
    @Builder.Default
    private Integer pmType = PmTypeEnums.PROJECT.getType();


    @Override
    public Project clone()
    {
        Project project = new Project();
        org.springframework.beans.BeanUtils.copyProperties(this, project);
        return project;
    }
}