package net.coding.app.project.http;

import com.github.pagehelper.PageInfo;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.framework.webapp.response.annotation.RestfulApi;
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
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.DEFAULT_FAIL_EXCEPTION;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TWEET_NOT_EXISTS;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Api(value = "项目公告", tags = "项目公告")
@RestController
@RequestMapping("/api/platform/project/{projectId}/notice")
@ProtectedAPI(privateTokenAccess = true, oauthScope = {OAuthConstants.Scope.PROJECT_NOTICE})
@RestfulApi
public class ProjectTweetController {

    @Autowired
    private ProjectTweetService projectTweetService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private UserGrpcClient userGrpcClient;

    /**
     * 检测项目权限，团队权限 id : 公告id
     */
    @ModelAttribute
    public void preCheckPermission(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @PathVariable("projectId") Integer projectId,
            @PathVariable(value = "tweetId", required = false) Integer tweetId,
            Model model
    ) throws CoreException {
        Project project = projectService.getById(projectId);
        if (project == null || !teamId.equals(project.getTeamOwnerId())) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        if (Objects.nonNull(tweetId)) {
            ProjectTweet projectTweet = projectTweetService.getById(tweetId);
            if (projectTweet == null || !projectTweet.getProjectId().equals(projectId)) {
                throw CoreException.of(TWEET_NOT_EXISTS);
            }
            model.addAttribute("projectTweet", projectTweet);
        }
        model.addAttribute("project", project);
    }

    @ApiOperation(value = "创建公告", notes = "原api:/api/project/{project_id}/tweet post")
    @ProtectedAPI
    @ProjectApiProtector(function = Function.ProjectNotice, action = Action.Create)
    @RequestMapping(value = "", method = POST)
    public ProjectTweetDTO createProjectTweet(
            @ModelAttribute("project") Project project,
            @Valid CreateTweetForm form,
            Errors errors) throws CoreException {
        if (!form.validate(errors)) {
            throw CoreException.of(errors);
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        ProjectTweet projectTweet = projectTweetService.insert(form.getContent(), form.getSlateRaw(), true, project);
        if (projectTweet == null) {
            throw CoreException.of(DEFAULT_FAIL_EXCEPTION);
        }
        UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
        return projectTweetService.toBuilderTweet(projectTweet, true, project, projectPath, user);
    }

    @ApiOperation(value = "编辑公告", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} put")
    @ProtectedAPI
    @ProjectApiProtector(function = Function.ProjectNotice, action = Action.Update)
    @RequestMapping(value = "{tweetId}", method = PUT)
    public ProjectTweetDTO updateProjectTweet(
            @ModelAttribute("project") Project project,
            @ModelAttribute("projectTweet") ProjectTweet projectTweet,
            @RequestParam("raw") String raw,
            @RequestParam(value = "slateRaw", required = false, defaultValue = "") String slateRaw
    ) throws CoreException {

        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        projectTweet = projectTweetService.update(projectTweet, raw, slateRaw, project);
        if (projectTweet == null) {
            throw CoreException.of(DEFAULT_FAIL_EXCEPTION);
        } else {
            UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
            return projectTweetService.toBuilderTweet(projectTweet, true, project, projectPath, user);
        }
    }

    /**
     * 获取一条项目内冒泡
     */
    @ApiOperation(value = "公告详情", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} get")
    @ProtectedAPI
    @RequestMapping(value = "{tweetId}", method = GET)
    public ProjectTweetDTO getById(
            @ModelAttribute("project") Project project,
            @ModelAttribute("projectTweet") ProjectTweet projectTweet,
            @PathVariable("tweetId") Integer tweetId,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) throws CoreException {
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
        return projectTweetService.toBuilderTweet(projectTweet, withRaw, project, projectPath, user);
    }

    @ApiOperation(value = "获取最近一条公告", notes = "项目概览中的公告，目前返回的是list,改成只包含最近公告的list,lastId参数不用了")
    @ProtectedAPI
    @RequestMapping(value = "last", method = GET)
    public List<ProjectTweetDTO> getLast(
            @ModelAttribute("project") Project project,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) {
        List<ProjectTweetDTO> dtos = new ArrayList<>();
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        ProjectTweet projectTweet = projectTweetService.getLast(project.getId());
        if (Objects.isNull(projectTweet)) {
            // 返回空列表而不是失败
            return dtos;
        }
        UserProto.User user = userGrpcClient.getUserById(projectTweet.getOwnerId());
        dtos.add(projectTweetService.toBuilderTweet(projectTweet, withRaw, project, projectPath, user));
        return dtos;
    }

    @ApiOperation(value = "公告列表", notes = "原api:/api/project/{project_id}/notices get")
    @ProtectedAPI
    @RequestMapping(value = "", method = GET)
    public ResultPage<ProjectTweetDTO> getProjectNotices(
            @ModelAttribute("project") Project project,
            @ModelAttribute LimitedPager pager,
            @RequestParam(value = "withRaw", defaultValue = "false") boolean withRaw) {
        Integer page = pager.getPage();
        Integer pageSize = pager.getPageSize();
        if (page == null || page <= 0) {
            page = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        String projectPath = projectGrpcClient.getProjectPath(project.getId());
        PageInfo<ProjectTweet> pageResult = projectTweetService.findList(project.getId(), page, pageSize);
        List<ProjectTweet> projectNoticeList = pageResult.getList();
        List<ProjectTweetDTO> dtoList = new ArrayList<>(projectNoticeList.size());
        List<Integer> userId = projectNoticeList.stream().map(ProjectTweet::getOwnerId).distinct().collect(Collectors.toList());
        Map<Integer, UserProto.User> userMap = userGrpcClient.findUserByIds(userId).stream().collect(Collectors.toMap(UserProto.User::getId, user -> user));
        for (ProjectTweet projectNotice : projectNoticeList) {
            if (projectNotice != null) {
                ProjectTweetDTO dto = projectTweetService.toBuilderTweet(projectNotice, withRaw, project, projectPath, userMap.get(projectNotice.getOwnerId()));
                dtoList.add(dto);
            }
        }
        return new ResultPage(dtoList, page, pageSize, pageResult.getTotal());
    }

    @ApiOperation(value = "删除公告", notes = "原api:/api/project/{project_id}/tweet/{tweet_id} delete")
    @ProtectedAPI
    @ProjectApiProtector(function = Function.ProjectNotice, action = Action.Update)
    @RequestMapping(value = "{tweetId}", method = DELETE)
    public void deleteProjectTweet(
            @ModelAttribute("project") Project project,
            @ModelAttribute("projectTweet") ProjectTweet projectTweet
    ) {
        Integer userId = SystemContextHolder.get() != null ? SystemContextHolder.get().getId() : 0;
        projectTweetService.delete(projectTweet, userId, project);
    }


}
