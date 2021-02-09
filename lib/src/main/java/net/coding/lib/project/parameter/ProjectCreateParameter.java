package net.coding.lib.project.parameter;

import net.coding.exchange.dto.user.User;

import java.util.Date;

import io.swagger.models.auth.In;
import lombok.Builder;
import lombok.Data;

/**
 * @Description: 创建项目请求参数
 * @Date 2021/1/26 14:13 下午
 */
@Data
@Builder(toBuilder = true)
public class ProjectCreateParameter {
    private Integer teamId;

    private Integer userId;

    private String userGk;

    /**
     * 项目分组id
     */
    private Integer groupId;
    /**
     * 名称
     */
    private String name;

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
     * 是否项目隐藏 true false 0、未隐藏  1、隐藏
     */
    private Boolean invisible;

    /**
     * 标签
     */
    private String label;

    /**
     * type
     */
    private String type;
    private Boolean gitEnabled;
    private String gitReadmeEnabled;
    private String gitIgnore;
    private String gitLicense;
    private String createSvnLayout;
    private String vcsType;
    private Integer shared;
    private String template;
    private String importFrom;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 完成日期
     */
    private Date endDate;


    /**
     * 是否初始化项目，默认true
     */
    @Builder.Default
    private Boolean shouldInitDepot = Boolean.TRUE;

    private User currentUser;

    private BaseCredentialParameter baseCredentialParameter;

}
