package net.coding.lib.project.parameter;

import net.coding.lib.project.form.CreateProjectForm;

import java.util.List;

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
     * 团队所有者Id
     */
    private Integer teamOwnerId;

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
    @Builder.Default
    private Boolean invisible = Boolean.FALSE;

    /**
     * 标签
     */
    private String label;

    /**
     * type
     */
    @Builder.Default
    private String type = "2";
    @Builder.Default
    private Boolean gitEnabled = Boolean.TRUE;
    @Builder.Default
    private String gitReadmeEnabled = Boolean.FALSE.toString();
    @Builder.Default
    private String gitIgnore = "no";
    @Builder.Default
    private String gitLicense = "no";
    @Builder.Default
    private String createSvnLayout = "no";
    @Builder.Default
    private String vcsType = "git";
    @Builder.Default
    private Integer shared = 0;
    private String template;
    private String projectTemplate;

    /**
     * 是否初始化项目，默认true
     */
    @Builder.Default
    private Boolean shouldInitDepot = Boolean.TRUE;

    private BaseCredentialParameter baseCredentialParameter;

    private List<CreateProjectForm.ProjectFunction> functionModule;

}
