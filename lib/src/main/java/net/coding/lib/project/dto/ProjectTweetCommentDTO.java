package net.coding.lib.project.dto;

import net.coding.common.annotation.QiniuCDNReplace;

import java.sql.Timestamp;

public class ProjectTweetCommentDTO {
    private Integer id;
    private Integer tweet_id;
    private Integer owner_id;
    @QiniuCDNReplace
    private String content;
    private Timestamp created_at;
}
