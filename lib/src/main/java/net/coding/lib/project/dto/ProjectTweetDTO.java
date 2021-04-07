package net.coding.lib.project.dto;

import net.coding.common.annotation.QiniuCDNReplace;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.utils.ApplicationUtil;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
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

}
