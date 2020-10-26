package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * project_tweets
 * @author 
 */
@Data
public class ProjectTweet implements Serializable {
    private static final long serialVersionUID = 1000000000005L;
    private Integer id;

    private Integer ownerId;

    private Integer projectId;

    private String content;

    /**
     * markdown 源文本
     */
    private String raw;

    private Integer comments;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;


}