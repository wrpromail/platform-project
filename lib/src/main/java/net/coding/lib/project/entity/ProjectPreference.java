package net.coding.lib.project.entity;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.xml.ws.BindingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPreference implements Serializable {

    private static final long serialVersionUID = 1000000000007L;

    /*-------------------------------- 偏好设置类型常量 --------------------------------*/
    /* 通知设置 */
    /** 项目公告. */
    public static final Short PREFERENCE_TYPE_PROJECT_TWEET = 1;
    /** 未受保护的分支. */
    public static final Short PREFERENCE_TYPE_UNPROTECTED_BRANCH_MERGE_REQUEST = 2;


    /*-------------------------------- 偏好设置状态常量 --------------------------------*/
    /* 通用. */
    /** 关闭. */
    public static final short PREFERENCE_STATUS_FALSE = 0;
    /** 开启. */
    public static final short PREFERENCE_STATUS_TRUE = 1;

    /**
     * 合并请求偏好设置的方式(PREFERENCE_TYPE_MR_SETTING type 7)
     * 0 默认直接合并
     * 1 默认 Squash 合并
     * 2 只能 Squash 合并
     */
    public static final short PREFERENCE_STATUS_DEFAULT = 0;
    public static final short PREFERENCE_STATUS_SQUASH = 1;
    public static final short PREFERENCE_STATUS_ONLY_SQUASH = 2;

    /**
     * 项目偏好设置的默认设置.
     */
    public static final Map<Short, Short> DEFAULT_PREFERENCES = ImmutableMap.<Short, Short>builder()
            .put(PREFERENCE_TYPE_PROJECT_TWEET, PREFERENCE_STATUS_TRUE)
            .build();

    /**
     * 编号
     */
    private Integer id;

    /**
     * 项目编号
     */
    private Integer projectId;

    /**
     * 偏好设置类型
     */
    private Integer type;

    /**
     * 偏好设置状态
     */
    private Integer status;

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
}
