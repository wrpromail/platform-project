package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectTweetDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.ActivityEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TWEET_FAST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TWEET_REPEAT;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TWEET_NOT_EXISTS;

@Service
public class ProjectTweetService {

    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectTweetDao projectTweetDao;

    @Resource
    private ProjectServiceHelper projectServiceHelper;

    public ProjectTweet insert(String content, boolean doCheck, Project project) throws CoreException {
        String raw = content;
        Integer userId = 0;
        Integer teamId = 0;
        if(Objects.nonNull(SystemContextHolder.get())) {
            userId = SystemContextHolder.get().getId();
            teamId = SystemContextHolder.get().getTeamId();
        }
        content = projectServiceHelper.updateAndCheckContent(content, project, teamId);
        if(doCheck) {
            ProjectTweet lastTweet = getLastTweetInTenMinutes(userId);
            if (lastTweet != null) {
                // 十分钟内不能创建相同的项目公告
                if (lastTweet.getContent().trim().equalsIgnoreCase(content.trim())) {
                    throw CoreException.of(PROJECT_TWEET_REPEAT);
                }
                // 十秒钟之内不能再次发布项目公告
                if (lastTweet.getCreatedAt().after(new Date(System.currentTimeMillis() - 10_000))) {
                    throw CoreException.of(PROJECT_TWEET_FAST);
                }
            }
        }
        ProjectTweet record = new ProjectTweet();
        record.setProjectId(project.getId());
        record.setOwnerId(userId);
        record.setContent(content);
        record.setRaw(raw);
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedAt(DateUtil.getCurrentDate());
        record.setComments(0);
        projectTweetDao.insert(record);
        if(record.getId() <= 0) {
            return null;
        }
        // 通知项目内所有人，被 @ 的人除外
        projectServiceHelper.notifyMembers(userId, content, project, record);
        // 通知被 @ 的人
        projectServiceHelper.notifyAtMembers(userId, content, record, project, false);
        // 记录项目冒泡／公告创建动态
        projectServiceHelper.postProjectTweetCreateActivity(project, record, userId, ActivityEnums.ACTION_TWEET_CREATE, ProjectTweet.ACTION_CREATE, "create");
        return record;
    }



    public ProjectTweet getLastTweetInTenMinutes(Integer userId) {
        Date time = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
        Map<String, Object> param = new HashMap<>();
        param.put("ownerId", userId);
        param.put("updatedAt", time);
        return projectTweetDao.getLastTweetInTenMinutes(param);
    }

    public ProjectTweet update(Integer id, String raw, Project project) throws CoreException {
        ProjectTweet tweet = projectTweetDao.getById(id);
        if (tweet == null) {
            throw CoreException.of(TWEET_NOT_EXISTS);
        }
        if(!Objects.equals(tweet.getProjectId(), project.getId())) {
            return null;
        }
        Integer userId = 0;
        Integer teamId = 0;
        if(Objects.nonNull(SystemContextHolder.get())) {
            userId = SystemContextHolder.get().getId();
            teamId = SystemContextHolder.get().getTeamId();
        }
        String content = projectServiceHelper.updateAndCheckContent(raw, project, teamId);
        tweet.setRaw(raw);
        tweet.setContent(content);
        tweet.setUpdatedAt(DateUtil.getCurrentDate());
        Integer flag = projectTweetDao.update(tweet);
        if(flag <= 0) {
            return null;
        }
        // 通知项目内所有人，被 @ 的人除外
        projectServiceHelper.notifyUpdateMembers(userId, content, project, tweet);
        // 通知被 @ 的人
        projectServiceHelper.notifyAtMembers(userId, content, tweet, project, false);
        // 记录项目冒泡／公告创建动态
        projectServiceHelper.postProjectTweetCreateActivity(project, tweet, userId, ActivityEnums.ACTION_TWEET_UPDATE, ProjectTweet.ACTION_UPDATE, "update");
        return tweet;
    }

    public int delete(Integer id, Integer userId, Project project) throws CoreException {
        ProjectTweet tweet = projectTweetDao.getById(id);
        if (tweet == null) {
            return -1;
        }
        if(!Objects.equals(tweet.getProjectId(), project.getId())) {
            return -1;
        }
        ProjectTweet projectTweet = new ProjectTweet();
        projectTweet.setDeletedAt(DateUtil.getCurrentDate());
        projectTweet.setId(id);
        Integer result = projectTweetDao.update(projectTweet);
        projectServiceHelper.postProjectTweetCreateActivity(project, tweet, userId, ActivityEnums.ACTION_TWEET_DELETE, ProjectTweet.ACTION_DELETE, "delete");
        return result;
    }

    public ProjectTweet getById(Integer id) {
        return projectTweetDao.getById(id);
    }

    public PageInfo<ProjectTweet> findList(Integer projectId, Integer page, Integer pageSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        PageInfo<ProjectTweet> pageInfo = PageHelper.startPage(page, pageSize)
                .doSelectPageInfo(() -> projectTweetDao.findList(parameters));
        return pageInfo;
    }

    public ProjectTweet getLast(Integer projectId) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("deletedAt", "1970-01-01 00:00:00");
        return projectTweetDao.getLast(params);
    }
}
