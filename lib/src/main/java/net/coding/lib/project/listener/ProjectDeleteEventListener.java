package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProjectDeleteEvent;
import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.group.ProjectGroup;
import net.coding.lib.project.group.ProjectGroupDao;
import net.coding.lib.project.group.ProjectGroupProject;
import net.coding.lib.project.group.ProjectGroupProjectDao;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectService;
import net.coding.lib.project.service.member.ProjectMemberAdaptorFactory;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.project.ProjectAdaptorFactory;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static net.coding.common.constants.ProjectConstants.ACTION_DELETE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDeleteEventListener {
    private final ProjectGroupProjectDao projectGroupProjectDao;
    private final ProjectGroupDao projectGroupDao;
    private final ProjectDao projectDao;
    private final ProjectMemberDao projectMemberDao;
    private final UserGrpcClient userGrpcClient;
    private final ProjectService projectService;
    private final ProjectMemberInspectService projectMemberInspectService;
    private final ProjectAdaptorFactory projectAdaptorFactory;
    private final ProjectMemberAdaptorFactory projectMemberAdaptorFactory;

    @Transactional
    @Subscribe
    public void handleProjectDeleteEvent(ProjectDeleteEvent event) {
        Project project = projectService.getProjectWithDeleted(event.getProjectId());
        if (Objects.isNull(project)) {
            return;
        }
        List<ProjectMember> members = projectMemberDao.findListByProjectId(project.getId(), BeanUtils.getDefaultDeletedAt());
        if (CollectionUtils.isEmpty(members)) {
            return;
        }
        List<ProjectGroupProject> projectGroupProjectList = projectGroupProjectDao
                .getByProjectId(event.getProjectId(), BeanUtils.getDefaultDeletedAt());
        for (ProjectGroupProject projectGroupProject : projectGroupProjectList) {
            delRelationOfProAndGro(event.getProjectId(), projectGroupProject);
        }

        Set<Integer> userIds = projectMemberInspectService.getPrincipalMemberUserIds(members);
        projectAdaptorFactory.create(project.getPmType())
                .sendProjectNotification(event.getUserId(), userIds,
                        project, ACTION_DELETE);

        projectMemberDao.batchDelete(members);
        projectMemberAdaptorFactory.create(project.getPmType())
                .postDeleteMemberUserEvent(project, event.getUserId(), StreamEx.of(userIds).toList());
    }

    private void delRelationOfProAndGro(Integer projectId, ProjectGroupProject projectGroupProject) {
        ProjectGroup projectGroup = projectGroupDao
                .getById(projectGroupProject.getProjectGroupId(), BeanUtils.getDefaultDeletedAt());
        try {
            deleteRelation(projectGroup, new ArrayList<Integer>() {{
                add(projectId);
            }}, userGrpcClient.getUserById(projectGroup.getOwnerId()).getId());
        } catch (CoreException e) {
            log.error("deleteProjectToGroup exec failed with {}", e.getMessage());
        }
    }

    /**
     * 处理删除项目时删除该用户对于该项目的分组信息
     */
    @Subscribe
    public void handleProjectMemberDeleteEvent(ProjectMemberDeleteEvent event) {
        Integer projectId = event.getProjectId();
        Project project = projectDao.getProjectById(projectId);
        if (project == null) {
            return;
        }
        projectGroupProjectDao
                .getByProjectIdsAndUserId(Collections.singletonList(projectId), event.getUserId(), BeanUtils.getDefaultDeletedAt())
                .stream().findFirst()
                .ifPresent(projectGroupProject
                        -> delRelationOfProAndGro(event.getProjectId(), projectGroupProject));
    }

    public boolean deleteRelation(ProjectGroup projectGroup, List<Integer> projectIdList,
                                  Integer userId) throws CoreException {
        if (projectGroup == null || userId == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        List<Project> projectList = projectDao
                .getProjectsByIds(projectIdList, BeanUtils.getDefaultDeletedAt(), BeanUtils.getDefaultArchivedAt());
        if (projectList.size() != projectIdList.size()) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }

        List<Integer> deleteProjectGroupProjectList = projectGroupProjectDao
                .getByProjectIdsAndUserId(projectIdList, userId, BeanUtils.getDefaultDeletedAt())
                .stream()
                .map(ProjectGroupProject::getId)
                .collect(Collectors.toList());

        return CollectionUtils.isNotEmpty(deleteProjectGroupProjectList)
                && projectGroupProjectDao.batchDelete(deleteProjectGroupProjectList) > 0;
    }
}
