package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.Result;
import net.coding.common.util.ResultSerializeNull;
import net.coding.lib.project.dao.pojo.ProjectSearchFilter;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectGroupNameNullException;
import net.coding.lib.project.exception.ProjectGroupNameTooLongException;
import net.coding.lib.project.exception.ProjectGroupNotExistException;
import net.coding.lib.project.exception.ProjectGroupSystemNotAllowOperateException;
import net.coding.lib.project.group.ProjectGroupMoveForm;
import net.coding.lib.project.group.ProjectGroup;
import net.coding.lib.project.group.ProjectGroupDTO;
import net.coding.lib.project.group.ProjectGroupDTOService;
import net.coding.lib.project.group.ProjectGroupService;
import net.coding.lib.project.service.ProjectService;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;


@Slf4j
@RestController
@Api(value = "项目分组", tags = "项目分组")
@AllArgsConstructor
@RequestMapping(value = "/api/platform/project/groups/group")
public class ProjectGroupController {

    private final ProjectService projectService;
    private final ProjectGroupService projectGroupService;
    private final ProjectGroupDTOService projectGroupDTOService;

    @ApiOperation(value = "list-project-group", notes = "项目分组列表")
    @GetMapping
    public List<ProjectGroupDTO> list(
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam(name = "needProjectNum", value = "需要项目数量")
            @RequestParam(value = "needProjectNum", required = false, defaultValue = "false")
                    boolean needProjectNum
    ) {
        return projectGroupDTOService.toDTO(
                projectGroupService.findAll(userId),
                projectGroupService::getProjectNum
        );
    }

    @ApiOperation(value = "create-project-group", notes = "创建项目分组")
    @PostMapping
    public ProjectGroupDTO create(
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam(name = "name", value = "项目分组名称", required = true)
            @RequestParam String name
    ) throws CoreException {
        checkNameLength(name);
        ProjectGroup group = projectGroupService.createGroup(name, userId);
        return projectGroupDTOService.toDTO(group, projectGroupService::getProjectNum);
    }

    private void checkNameLength(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new ProjectGroupNameNullException();
        }
        if (name.length() > 32) {
            throw new ProjectGroupNameTooLongException();
        }
    }

    @ApiOperation(value = "update-project-group", notes = "更新项目分组信息")
    @PutMapping(value = "/{id}")
    public Result update(
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam(name = "id", value = "项目分组标识", required = true)
            @PathVariable(value = "id") Integer id,
            @ApiParam(name = "name", value = "项目分组名称", required = true)
            @RequestParam(value = "name", defaultValue = "") String name
    ) throws CoreException {
        checkNameLength(name);
        ProjectGroup projectGroup = projectGroupService.getGroupById(id);
        if (projectGroup == null || !userId.equals(projectGroup.getOwnerId())) {
            throw new ProjectGroupNotExistException();
        }
        if (projectGroup.isSystem()) {
            throw new ProjectGroupSystemNotAllowOperateException();
        }
        if (!StringUtils.isEmpty(name)) {
            projectGroupService.updateGroup(projectGroup, name, userId);
        }
        return ResultSerializeNull.success();
    }

    @ApiOperation(value = "delete-project-group", notes = "删除项目分组")
    @DeleteMapping(value = "/{id}")
    public Result delete(
            @ApiParam(name = "id", value = "项目分组标识", required = true)
            @PathVariable(value = "id") Integer id,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId
    ) throws CoreException {
        ProjectGroup projectGroup = projectGroupService.getGroupById(id);
        if (projectGroup == null || !userId.equals(projectGroup.getOwnerId())) {
            throw new ProjectGroupNotExistException();
        }
        if (projectGroup.isSystem()) {
            throw new ProjectGroupSystemNotAllowOperateException();
        }
        projectGroupService.deleteGroup(projectGroup, userId);

        return ResultSerializeNull.success();
    }

    @ApiOperation(value = "sort-project-group", notes = "对项目分组进行排序")
    @PatchMapping(value = "/{id}/sort")
    public Result sort(
            @ApiParam(name = "id", value = "移动分组标识", required = true)
            @PathVariable(value = "id") Integer id,
            @ApiParam(name = "afterId", value = "参照分组标识", required = true)
            @RequestParam(value = "afterId") Integer afterId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId
    ) throws CoreException {
        if (afterId == null) {
            throw new ProjectGroupNotExistException();
        }

        if (afterId != 0) {
            ProjectGroup projectGroup = projectGroupService.getGroupById(id);
            if (projectGroup == null || !userId.equals(projectGroup.getOwnerId())) {
                throw new ProjectGroupNotExistException();
            }
        }

        ProjectGroup projectGroup = projectGroupService.getGroupById(id);
        if (projectGroup == null || !userId.equals(projectGroup.getOwnerId())) {
            throw new ProjectGroupNotExistException();
        }

        projectGroupService.groupSort(projectGroup, afterId);

        return ResultSerializeNull.success();
    }

    @ApiOperation(value = "move-project to group", notes = "移动项目到分组")
    @PutMapping(value = "/{groupId}/project")
    public Result moveProject2Group(
            @ApiParam(name = "projectGroupMoveForm", value = "移动项目表单信息", required = true)
                    ProjectGroupMoveForm projectGroupMoveForm,
            @ApiParam(name = "groupId", value = "分组标识", required = true)
            @PathVariable("groupId") Integer groupId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId
    ) throws CoreException {
        List<Integer> projectIdList = solveIdsToProjectIds(projectGroupMoveForm, teamId, userId);
        if (CollectionUtils.isEmpty(projectIdList)) {
            return Result.success();
        }

        ProjectGroup projectGroup = projectGroupService.getGroupById(groupId);
        if (projectGroup == null)
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        if (!projectGroup.getOwnerId().equals(userId))
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        // 移动到未分组，即删除分组
        if (projectGroup.isNoGroup()) {
            projectGroupService.delRelationOfProAndGro(projectGroup, projectIdList, userId);
        } else {
            projectGroupService.addProjectToGroup(projectGroup, projectIdList, userId);
        }
        return Result.success();
    }

    private List<Integer> solveIdsToProjectIds(
            ProjectGroupMoveForm form,
            Integer teamId,
            Integer userId
    ) throws CoreException {
        if (form.isSelectAll()) {
            ProjectSearchFilter selectAllUnderGroupCondition = new ProjectSearchFilter();
            selectAllUnderGroupCondition.setTeamId(teamId);
            selectAllUnderGroupCondition.setUserId(userId);
            selectAllUnderGroupCondition.setGroupId(form.getFromGroupId());
            if (!StringUtils.isEmpty(form.getKeyword())) {
                selectAllUnderGroupCondition.setKeyword(form.getKeyword());
            }
            selectAllUnderGroupCondition.setPageSize(1000);
            checkData(selectAllUnderGroupCondition);
            List<Integer> projectIds = projectService.getSimpleProjectsByFilter(selectAllUnderGroupCondition)
                    .stream()
                    .filter(Objects::nonNull)
                    .map(Project::getId)
                    .collect(Collectors.toList());
            // 排除掉需要排除的
            if (!StringUtils.isEmpty(form.getExcludeIds())) {
                List<Integer> excludeIds = idStringToInt(form.getExcludeIds());
                projectIds.removeAll(excludeIds);
            }
            return projectIds;
        } else {
            return idStringToInt(form.getIds());
        }
    }

    private List<Integer> idStringToInt(String ids) {
        return StringUtils.commaDelimitedListToSet(ids)
                .stream()
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    private void checkData(ProjectSearchFilter filter) throws CoreException {
        Integer groupId = filter.getGroupId();
        Integer userId = filter.getUserId();
        if (groupId != null && groupId > 0) {
            ProjectGroup projectGroup = projectGroupService.getById(groupId);
            if (projectGroup == null || (userId != null && !userId.equals(projectGroup.getOwnerId()))) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            if (ProjectGroup.TYPE.ALL.toString().equals(projectGroup.getType())) {
                // 全部项目，将groupId置空
                filter.setGroupId(null);
            } else if (ProjectGroup.TYPE.NO_GROUP.toString().equals(projectGroup.getType())) {
                // 未分组项目 置0
                filter.setGroupId(ProjectGroup.NO_GROUP_ID);
            }
        }
    }
}
