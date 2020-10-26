package net.coding.app.project.http;

import com.github.pagehelper.PageInfo;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import proto.platform.user.UserProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.Result;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dto.ProjectTweetDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.CreateTweetForm;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.ProjectTweetService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "项目公告", tags = "项目公告")
@RestController
@RequestMapping("/api/platform/project/{projectId}/notice")
@ProtectedAPI(privateTokenAccess = true, oauthScope = {OAuthConstants.Scope.PROJECT_NOTICE})
public class ProjectTweetController {

    @Autowired
    private ProjectTweetService projectTweetService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private UserGrpcClient userGrpcClient;

    @ApiOperation(value = "创建公告", notes = "原api:/api/project/{project_id}/tweet post")
    @ProtectedAPI
    @EnterpriseApiProtector(function = Function.ProjectNotice, action = Action.Create)
    @RequestMapping(value = "", method = POST)
    public Result createProjectTweet(
            @PathVariable("projectId") Integer projectId,
            @Valid CreateTweetForm form,
            Errors errors) throws CoreException {
        if (!form.validate(errors)) {
            throw CoreException.of(errors);
        }
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        ProjectTweet projectTweet = projectTweetService.insert(form.getContent(), true, project);
        if (projectTweet == null) {
            return Result.failed();
        } else {
            UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
            return Result.success(new ProjectTweetDTO(projectTweet, true, project, projectPath, user));
        }
    }

    @ApiOperation(value = "编辑公告", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} put")
    @ProtectedAPI
    @EnterpriseApiProtector(function = Function.ProjectNotice, action = Action.Update)
    @RequestMapping(value = "{id}", method = PUT)
    public Result updateProjectTweet(
            @PathVariable("projectId") Integer projectId,
            @PathVariable("id") Integer id,
            @RequestParam("raw") String raw) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        ProjectTweet projectTweet = projectTweetService.update(id, raw, project);
        if (projectTweet == null) {
            return Result.failed();
        } else {
            UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
            return Result.success(new ProjectTweetDTO(projectTweet, true, project, projectPath, user));
        }
    }

    /**
     * 获取一条项目内冒泡
     */
    @ApiOperation(value = "公告详情", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} get")
    @ProtectedAPI
    @RequestMapping(value = "{id}", method = GET)
    public Result getById(
            @PathVariable("projectId") Integer projectId,
            @PathVariable("id") Integer tweetId,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        ProjectTweet projectTweet = projectTweetService.getById(tweetId);
        if (projectTweet == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_TWEET_NOT_EXISTS);
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
        return Result.success(new ProjectTweetDTO(projectTweet, withRaw, project, projectPath, user));
    }

    @ApiOperation(value = "获取最近一条公告", notes = "项目概览中的公告，目前返回的是list,改成只包含最近公告的list,lastId参数不用了")
    @ProtectedAPI
    @RequestMapping(value = "last", method = GET)
    public Result getLast (
            @PathVariable("projectId") Integer projectId,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        ProjectTweet projectTweet = projectTweetService.getLast(projectId);
        if (Objects.isNull(projectTweet)) {
            // 返回空列表而不是失败
            return Result.success();
        } else {
            List<ProjectTweetDTO> dtos = new ArrayList<>();
            UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
            dtos.add(new ProjectTweetDTO(projectTweet, withRaw, project, projectPath, user));
            return Result.success(dtos);
        }
    }

    @ApiOperation(value = "公告列表", notes = "原api:/api/project/{project_id}/notices get")
    @ProtectedAPI
    @RequestMapping(value = "", method = GET)
    public ResultPage<ProjectTweetDTO> getProjectNotices (
            @PathVariable("projectId") Integer projectId,
            @ModelAttribute LimitedPager pager,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        Integer page = pager.getPage();
        Integer pageSize = pager.getPageSize();
        if(page == null || page <= 0) {
            page = 1;
        }
        if(pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        PageInfo<ProjectTweet > pageResult = projectTweetService.findList(projectId, page, pageSize);
        List<ProjectTweet> projectNoticeList = pageResult.getList();
        List<ProjectTweetDTO> dtoList = new ArrayList<>(projectNoticeList.size());
        List<Integer> userId = projectNoticeList.stream().map(ProjectTweet::getOwnerId).distinct().collect(Collectors.toList());
        Map<Integer, UserProto.User> userMap = userGrpcClient.findUserByIds(userId).stream().collect(Collectors.toMap(UserProto.User::getId, user -> user));
        for (ProjectTweet projectNotice : projectNoticeList) {
            if (projectNotice != null) {
                ProjectTweetDTO dto = new ProjectTweetDTO(projectNotice, withRaw, project, projectPath, userMap.get(projectNotice.getOwnerId()));
                dtoList.add(dto);
            }
        }
        return new ResultPage(dtoList, page, pageSize, pageResult.getTotal());
    }

    @ApiOperation(value = "删除公告", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} delete")
    @ProtectedAPI
    @EnterpriseApiProtector(function = Function.ProjectNotice, action = Action.Update)
    @RequestMapping(value = "{id}", method = DELETE)
    public Result deleteProjectTweet(
            @PathVariable("projectId") Integer projectId,
            @PathVariable("id") Integer id) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        Integer userId = SystemContextHolder.get() != null ? SystemContextHolder.get().getId() : 0;
        return Result.of(projectTweetService.delete(id, userId, project) > 0);
    }
}
