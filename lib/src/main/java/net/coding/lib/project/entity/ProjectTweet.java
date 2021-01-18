package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * project_tweets
 * @author 
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTweet implements Serializable {
    private static final long serialVersionUID = 1000000000005L;

    public static final Short ACTION_CREATE = 1;
    public static final Short ACTION_UPDATE = 2;
    public static final Short ACTION_DELETE = 3;

    private Integer id;

    private Integer ownerId;

    private Integer projectId;

    private String content;

    /**
     * markdown 源文本
     */
    private String raw;


    /**
     * 新版markdown 源文本
     */
    private String slateRaw;

    private Integer comments;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;


}