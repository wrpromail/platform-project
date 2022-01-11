package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProjectDeleteEvent;
import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.group.ProjectGroupDao;
import net.coding.lib.project.group.ProjectGroupProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.group.ProjectGroup;
import net.coding.lib.project.group.ProjectGroupProject;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDeleteEventListener {
    private final ProjectGroupProjectDao projectGroupProjectDao;
    private final ProjectGroupDao projectGroupDao;
    private final ProjectDao projectDao;
    private final UserGrpcClient userGrpcClient;

    @Subscribe
    public void handleProjectDeleteEvent(ProjectDeleteEvent event) {
        List<ProjectGroupProject> projectGroupProjectList = projectGroupProjectDao
                .getByProjectId(event.getProjectId(), BeanUtils.getDefaultDeletedAt());
        for (ProjectGroupProject projectGroupProject : projectGroupProjectList) {
            delRelationOfProAndGro(event.getProjectId(), projectGroupProject);
        }
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
