package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class MergeRequest implements Serializable {

    private static final long serialVersionUID = 1604390432747L;

    private Integer id;

    /**
     * 仓库编号
     */
    private Integer depotId;

    /**
     * 源分支
     */
    private String sourceBranch;

    /**
     * 目标分支
     */
    private String targetBranch;

    /**
     * 作者编号
     */
    private Integer authorId;

    /**
     * 合并者或者拒绝者编号
     */
    private Integer actionAuthor;

    /**
     * 指派者编号
     */
    private Integer assigneeId;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String body;

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
     * 合并或者拒绝时间
     */
    private Date actionAt;

    /**
     * 合并状态
     */
    private String mergeStatus;

    /**
     * 项目内编号
     */
    private Integer iid;

    /**
     * 源分支sha
     */
    private String sourceSha;

    /**
     * 目标分支sha
     */
    private String targetSha;

    private Boolean granted;

    private Integer grantedBy;

    private Integer pipelineId;

    private String bodyPlan;

    private String conflicts;

    private String mergedSha;
}
