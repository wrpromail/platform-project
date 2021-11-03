package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.grpc.client.template.TemplateGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectTweetDao;
import net.coding.lib.project.dto.ProjectTweetDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.ActivityEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.utils.DateUtil;
import net.coding.lib.project.utils.TextUtil;
import net.coding.lib.project.utils.UserUtil;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import lombok.AllArgsConstructor;
import proto.platform.user.UserProto;

import static net.coding.common.constants.ValidationConstants.TWEET_LIMIT_IMAGES;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TWEET_FAST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TWEET_REPEAT;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TWEET_IMAGE_LIMIT_N;

@Service
@AllArgsConstructor
public class ProjectTweetService {

    private final ProjectTweetDao projectTweetDao;

    private final ProjectServiceHelper projectServiceHelper;

    private final ProjectMemberService projectMemberService;

    private final TemplateGrpcClient templateGrpcClient;

    private final ProjectResourceLinkService projectResourceLinkService;

    public ProjectTweet insert(String content, String slateRaw, boolean doCheck, Project project) throws CoreException {
        String raw = content;
        Integer userId = 0;
        Integer teamId = 0;
        if (Objects.isNull(SystemContextHolder.get())) {
            return null;
        }
        userId = SystemContextHolder.get().getId();
        teamId = SystemContextHolder.get().getTeamId();
        content = updateAndCheckContent(content, project, teamId);
        if (doCheck) {
            ProjectTweet lastTweet = getLastTweetInTenMinutes(userId, project.getId());
            if (lastTweet != null) {
                // 十分钟内不能创建相同的项目公告
                if (lastTweet.getContent().trim().equalsIgnoreCase(content.trim())) {
                    throw CoreException.of(PROJECT_TWEET_REPEAT);
                }
                // 十秒钟之内不能再次发布项目公告
                if (lastTweet.getCreatedAt().after(
                        Date.from(LocalDateTime.now().minusSeconds(10).atZone(ZoneId.systemDefault()).toInstant())
                )) {
                    throw CoreException.of(PROJECT_TWEET_FAST);
                }
            }
        }
        ProjectTweet record = ProjectTweet.builder()
                .projectId(project.getId())
                .ownerId(userId)
                .content(content)
                .raw(raw)
                .slateRaw(StringUtils.defaultIfEmpty(slateRaw, ""))
                .createdAt(DateUtil.getCurrentDate())
                .updatedAt(DateUtil.getCurrentDate())
                .comments(0)
                .build();
        if (projectTweetDao.insert(record) <= 0) {
            return null;
        }
        // 通知项目内所有人，被 @ 的人除外
        projectServiceHelper.notifyMembers(projectMemberService.filterNotifyUserIds(userId, content, project, record), userId, project, record);
        // 通知被 @ 的人
        projectServiceHelper.notifyAtMembers(projectMemberService.parseAtUser(userId, project, content, record.getOwnerId()), userId, content, record, project, false);
        // 记录项目冒泡／公告创建动态
        projectServiceHelper.postProjectTweetCreateActivity(project, record, userId, ActivityEnums.ACTION_TWEET_CREATE, ProjectTweet.ACTION_CREATE, "create");
        return record;
    }

    public ProjectTweet getLastTweetInTenMinutes(Integer userId, Integer projectId) {
        ProjectTweet projectTweet = ProjectTweet.builder()
                .ownerId(userId)
                .projectId(projectId)
                .updatedAt(Date.from(LocalDateTime.now().minusMinutes(10).atZone(ZoneId.systemDefault()).toInstant()))
                .build();
        return projectTweetDao.getProjectTweet(projectTweet);
    }

    public ProjectTweet update(ProjectTweet tweet, String raw, String slateRaw, Project project) throws CoreException {
        Integer userId = 0;
        Integer teamId = 0;
        if (Objects.nonNull(SystemContextHolder.get())) {
            userId = SystemContextHolder.get().getId();
            teamId = SystemContextHolder.get().getTeamId();
        }
        String content = updateAndCheckContent(raw, project, teamId);
        tweet.setRaw(raw);
        tweet.setContent(content);
        if (StringUtils.isNoneEmpty(slateRaw)) {
            tweet.setSlateRaw(slateRaw);
        }
        tweet.setUpdatedAt(DateUtil.getCurrentDate());
        Integer flag = projectTweetDao.update(tweet);
        if (flag <= 0) {
            return null;
        }
        // 通知项目内所有人，被 @ 的人除外
        projectServiceHelper.notifyUpdateMembers(projectMemberService.filterNotifyUserIds(userId, content, project, tweet), userId, project, tweet);
        // 通知被 @ 的人
        projectServiceHelper.notifyAtMembers(projectMemberService.parseAtUser(userId, project, content, tweet.getOwnerId()), userId, content, tweet, project, false);
        // 记录项目冒泡／公告创建动态
        projectServiceHelper.postProjectTweetCreateActivity(project, tweet, userId, ActivityEnums.ACTION_TWEET_UPDATE, ProjectTweet.ACTION_UPDATE, "update");
        return tweet;
    }

    public int delete(ProjectTweet tweet, Integer userId, Project project) {
        ProjectTweet projectTweet = ProjectTweet.builder()
                .id(tweet.getId())
                .deletedAt(DateUtil.getCurrentDate())
                .build();
        Integer result = projectTweetDao.update(projectTweet);
        if (result != null && result > 0) {
            projectServiceHelper.postProjectTweetCreateActivity(project, tweet, userId, ActivityEnums.ACTION_TWEET_DELETE, ProjectTweet.ACTION_DELETE, "delete");
        }
        return result;
    }

    public ProjectTweet getById(Integer id) {
        return projectTweetDao.getById(id);
    }

    public PageInfo<ProjectTweet> findList(Integer projectId, Integer page, Integer pageSize) {
        ProjectTweet projectTweet = ProjectTweet.builder()
                .projectId(projectId)
                .build();
        return PageHelper.startPage(page, pageSize)
                .doSelectPageInfo(() -> projectTweetDao.findList(projectTweet));
    }

    public ProjectTweet getLast(Integer projectId) {
        ProjectTweet projectTweet = ProjectTweet.builder()
                .projectId(projectId)
                .build();
        return projectTweetDao.getProjectTweet(projectTweet);
    }

    /**
     * 解析 {@code content} 并检查内容是否合法.
     *
     * @param content 文本
     * @param project 项目
     * @return 解析后的文本
     * @throws CoreException 图片数目超出限制/包含限制词
     */
    public String updateAndCheckContent(String content, Project project, Integer teamId) throws CoreException {
        String newContent = projectResourceLinkService.linkize(content, project);
        newContent = templateGrpcClient.markdownWithMonkey(teamId, newContent, false);
        newContent = TextUtil.filterUserInputContent(newContent);
        //newContent = downloadImageService.filterHTML(newContent);

        // 图片数目超过限制
        if (limitTweetImages(newContent, TWEET_LIMIT_IMAGES)) {
            throw CoreException.of(TWEET_IMAGE_LIMIT_N, TWEET_LIMIT_IMAGES);
        }
        return newContent;
    }

    /**
     * 判断用户冒泡的图片数量是否超限
     */
    public boolean limitTweetImages(String content, int amount) {
        Document doc = Jsoup.parse(content);
        Elements eles = doc.select("img.bubble-markdown-image");
        return eles.size() > amount;
    }

    public ProjectTweetDTO toBuilderTweet(ProjectTweet tweet, boolean withRaw, Project project, String projectPath, UserProto.User user) {
        return ProjectTweetDTO.builder()
                .id(tweet.getId())
                .project_id(tweet.getProjectId())
                .owner_id(tweet.getOwnerId())
                .content(tweet.getContent())
                .created_at(tweet.getCreatedAt().getTime())
                .updated_at(tweet.getUpdatedAt().getTime())
                .comments(tweet.getComments())
                .comment_list(new ArrayList<>())
                .raw(withRaw ? tweet.getRaw() : null)
                .slateRaw(tweet.getSlateRaw())
                .editable(StringUtils.isNotBlank(tweet.getRaw()))
                .path(Objects.isNull(user) ? StringUtils.EMPTY : projectServiceHelper.tweetPath(tweet, project, projectPath))
                .owner(UserUtil.toBuilderUser(user, false))
                .build();

    }
}
