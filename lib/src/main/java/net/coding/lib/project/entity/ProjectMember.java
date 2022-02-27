package net.coding.lib.project.entity;

import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

import javax.persistence.Table;

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
@Table(name = "project_members")
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
     * 主体类型 （user,user_group,department
     */
    private String principalType;

    /**
     * 主体ID
     */
    private String principalId;

    /**
     * 主体排序
     */
    private Integer principalSort;

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

    /**
     * 兼容逻辑 防止未迁移前 principalType , principalId 为空
     */
    public String getPrincipalType() {
        if (StringUtils.isBlank(principalType)) {
            principalType = ProjectMemberPrincipalTypeEnum.USER.name();
        }
        return principalType;
    }

    public String getPrincipalId() {
        if (StringUtils.isBlank(principalId)) {
            principalId = String.valueOf(userId);
        }
        return principalId;
    }

    public Integer getPrincipalSort() {
        if (Objects.isNull(principalSort)) {
            principalSort = ProjectMemberPrincipalTypeEnum.USER.getSort();
        }
        return principalSort;
    }
}