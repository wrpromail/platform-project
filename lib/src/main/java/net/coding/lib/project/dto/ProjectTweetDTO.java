package net.coding.lib.project.dto;

import net.coding.common.annotation.QiniuCDNReplace;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.utils.ApplicationUtil;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import proto.platform.user.UserProto;

public class ProjectTweetDTO {
    private static final UserGrpcClient userGrpcClient = ApplicationUtil.get(UserGrpcClient.class);
    private static final ProjectServiceHelper projectServiceHelper = ApplicationUtil.get(ProjectServiceHelper.class);
    private Integer id;
    private Integer project_id;
    private Integer owner_id;
    private UserDTO owner;
    private long created_at;
    private long updated_at;
    private Integer comments;
    private List<ProjectTweetCommentDTO> comment_list;
    @QiniuCDNReplace
    private String content;
    private String path;
    @QiniuCDNReplace
    private String raw;
    private String slateRaw;
    private boolean editable;


    public ProjectTweetDTO(ProjectTweet tweet, boolean withRaw, Project project, String projectPath, UserProto.User user) {
        if (tweet == null) {
            return;
        }
        this.id = tweet.getId();
        this.project_id = tweet.getProjectId();
        this.owner_id = tweet.getOwnerId();
        this.content = tweet.getContent();
        this.created_at = tweet.getCreatedAt().getTime();
        this.updated_at = tweet.getUpdatedAt().getTime();
        this.comments = tweet.getComments();
        this.comment_list = new ArrayList<>();
        this.raw = withRaw ? tweet.getRaw() : null;
        this.slateRaw = tweet.getSlateRaw();
        this.editable = StringUtils.isNotBlank(tweet.getRaw());
        if(Objects.isNull(user)) {
            this.path = StringUtils.EMPTY;
        } else {
            this.path = projectServiceHelper.tweetPath(tweet, project, projectPath);
        }
        this.owner = new UserDTO(user);
    }
}
