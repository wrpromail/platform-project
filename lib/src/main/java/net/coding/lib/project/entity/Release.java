package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class Release implements Serializable {

    private static final long serialVersionUID = 1604392049997L;

    private Integer id;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;

    private Integer creatorId;

    private Integer projectId;

    private Integer depotId;

    private Integer iid;

    private String tagName;

    private String commitSha;

    private String targetCommitish;

    private String title;

    private String body;

    private String html;

    private String compareTagName;

    private Boolean pre;

    private Boolean draft;

    private Integer pipelineId;
}
