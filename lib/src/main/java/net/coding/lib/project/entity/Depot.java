package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * depots
 * @author 
 */
@Data
public class Depot implements Serializable {
    /**
     * 编号
     */
    private Integer id;

    /**
     * 项目编号
     */
    private Integer projectId;

    /**
     * 仓库名称
     */
    private String name;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 是否默认仓库
     */
    private Boolean isDefault;

    /**
     * 父项目编号
     */
    private Integer parentId;

    /**
     * 根编号
     */
    private Integer rootId;

    /**
     * 远程 fetch 路径
     */
    private String originUrl;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 删除时间
     */
    private Date deletedAt;

    private String rpcServer;

    private Byte status;

    private String originVcs;

    private Integer quota;

    private Integer codeInsightMaxValue;

    private String languages;

    private Date languagedAt;

    private String location;

    private String worker;

    private Integer size;

    private String pagesBranch;

    private Boolean svnEnabled;

    private Byte flowType;

    private String vcsType;

    /**
     * 是否公开仓库源代码
     */
    private Byte shared;

    /**
     * 是否支持 svn http 协议
     */
    private Boolean isSvnHttp;

    /**
     * 研发流程规范 ID
     */
    private Integer devFlowId;

    private static final long serialVersionUID = 1604547645707L;
}