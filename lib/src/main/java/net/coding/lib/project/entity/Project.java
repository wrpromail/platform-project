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
public class Project implements Serializable {
    /**
     * 编号
     */
    private Integer id;

    /**
     * 项目所有者ID，这个字段需要删掉
     */
    private Integer ownerId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 删除时间
     */
    private Date deletedAt;

    /**
     * 项目状态
     */
    private Byte status;

    private Byte recommended;

    /**
     * 是否公开仓库源代码
     */
    private Byte depotShared;

    private Boolean type;

    private Short maxMember;

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
    private Byte plan;

    /**
     * 项目所属团队ID
     */
    private Integer teamOwnerId;

    /**
     * 项目所属用户ID
     */
    private Integer userOwnerId;

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
     * 是否隐藏  TCB为1
     */
    private Byte invisible;

    /**
     * 标签  TCB
     */
    private String label;

    private static final long serialVersionUID = 1000000000001L;
}