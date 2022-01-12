package net.coding.lib.project.group;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.pojo.ProjectSearchFilter;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectGroupSameNameException;
import net.coding.lib.project.grpc.client.UserGrpcClient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectGroupService {

    private final ProjectGroupDao projectGroupDao;
    private final ProjectGroupProjectDao projectGroupProjectDao;
    private final ProjectDao projectDao;
    private final UserGrpcClient userGrpcClient;
    private final LocaleMessageSource localeMessageSource;


    public ProjectGroup getById(Integer id) {
        return projectGroupDao.getById(id, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectGroup getByUserAndName(Integer userId, String name) {
        return projectGroupDao.getByUserAndName(userId, name, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectGroup getByProjectAndUser(Integer projectId, Integer userId) {
        return Optional.ofNullable(
                        projectGroupProjectDao.getByProjectIdsAndUserId(
                                Collections.singletonList(projectId),
                                userId, BeanUtils.getDefaultDeletedAt()
                        )
                ).map(Collection::stream)
                .orElse(Stream.empty())
                .findFirst()
                .map(gp -> projectGroupDao.getById(gp.getProjectGroupId(), BeanUtils.getDefaultDeletedAt()))
                .orElse(null);
    }

    public ProjectGroup createGroup(String name, Integer userId) throws CoreException {
        if (StringUtils.isEmpty(name) || userId == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        ProjectGroup projectGroup = getByUserAndName(userId, name);
        if (projectGroup != null) {
            throw new ProjectGroupSameNameException();
        }
        projectGroup = new ProjectGroup();
        projectGroup.setName(name);
        projectGroup.setOwnerId(userId);
        ProjectGroup maxSortProjectGroup = projectGroupDao
                .getMaxSortProjectGroup(userId, BeanUtils.getDefaultDeletedAt());
        projectGroup.setSort(maxSortProjectGroup == null ? 0 : maxSortProjectGroup.getSort() + 1);
        projectGroupDao.insertAndRetId(projectGroup);
        return projectGroup;
    }

    public void updateGroup(ProjectGroup projectGroup, String name, Integer userId) throws CoreException {
        if (StringUtils.isEmpty(name) || userId == null)
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        ProjectGroup pg = getByUserAndName(userId, name);
        if (pg != null)
            throw new ProjectGroupSameNameException();
        if (projectGroup == null || !projectGroup.getOwnerId().equals(userId))
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        projectGroup.setName(name);
        projectGroupDao.updateByPrimaryKeySelective(projectGroup);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(ProjectGroup projectGroup, Integer userId) throws CoreException {
        if (projectGroup == null || userId == null)
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        if (!projectGroup.getOwnerId().equals(userId))
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);

        projectGroupProjectDao.deleteGroupRelation(projectGroup.getId(), userId);

        projectGroupDao.deleteLogical(projectGroup.getId(), BeanUtils.getDefaultDeletedAt());
    }


    public ProjectGroup getGroupById(Integer groupId) {
        return projectGroupDao.getById(groupId, BeanUtils.getDefaultDeletedAt());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ProjectGroup> findAll(Integer userId) {
        initSystemGroup(userId);
        return projectGroupDao.findAllByOwnerId(userId, BeanUtils.getDefaultDeletedAt());
    }

    /**
     * 初始化系统分组
     */
    public void initSystemGroup(int ownerId) {

        if (systemGroupInit(ownerId)) {
            return;
        }

        // 全部项目
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setOwnerId(ownerId);
        projectGroup.setType(ProjectGroup.TYPE.ALL.toString());
        projectGroup.setName(localeMessageSource.getMessage("group_system_all_group"));
        projectGroup.setSort(-2);
        projectGroupDao.insertAndRetId(projectGroup);
        // 未分组
        ProjectGroup noGroup = new ProjectGroup();
        noGroup.setOwnerId(ownerId);
        noGroup.setName(localeMessageSource.getMessage("group_system_no_group"));
        noGroup.setType(ProjectGroup.TYPE.NO_GROUP.toString());
        noGroup.setSort(-1);
        projectGroupDao.insertAndRetId(noGroup);

    }

    public boolean systemGroupInit(int userId) {
        return projectGroupDao.countByUserIdAndType(
                userId,
                ProjectGroup.TYPE.ALL.toString(),
                BeanUtils.getDefaultDeletedAt()
        ) > 0;
    }


    @Transactional(rollbackFor = Exception.class)
    public void groupSort(ProjectGroup projectGroup, Integer afterId) throws CoreException {
        if (afterId == 0) {
            // 最前面
            ProjectGroup minSortProjectGroup = projectGroupDao
                    .getMinSortProjectGroup(projectGroup.getOwnerId(), BeanUtils.getDefaultDeletedAt());
            projectGroup.setSort(minSortProjectGroup == null ? 0 : minSortProjectGroup.getSort() - 1);
            projectGroupDao.updateByPrimaryKeySelective(projectGroup);
        } else {
            // 某一个之后
            ProjectGroup afterGroup = getGroupById(afterId);
            if (afterGroup == null) {
                throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
            }

            List<ProjectGroup> projectGroups = projectGroupDao
                    .findListAfterId(projectGroup.getOwnerId(), afterGroup.getSort(), BeanUtils.getDefaultDeletedAt())
                    .stream()
                    .filter(pg -> pg.getId().equals(projectGroup.getId()))
                    .peek(group -> group.setSort(group.getSort() + 1))
                    .collect(Collectors.toList());

            projectGroupDao.batchUpdate(projectGroups);

            projectGroup.setSort(afterGroup.getSort() + 1);
            projectGroupDao.updateByPrimaryKeySelective(projectGroup);

        }
    }

    /**
     * 获取分组下的项目总数
     */
    public long getProjectNum(ProjectGroup projectGroup) {
        if (projectGroup == null) {
            return 0;
        }
        UserProto.User user = userGrpcClient.getUserById(projectGroup.getOwnerId());

        ProjectSearchFilter filter = new ProjectSearchFilter();
        filter.setTeamId(user.getTeamId());
        filter.setUserId(user.getId());

        if (projectGroup.isAll()) {
            return projectDao.countProjectsByFilter(
                    filter.getTeamId(),
                    filter.getUserId(),
                    BeanUtils.getDefaultDeletedAt()
            );
        }

        filter.setCountWithGroup(true);
        if (projectGroup.isNoGroup()) {
            filter.setGroupId(ProjectGroup.NO_GROUP_ID);
        } else {
            filter.setGroupId(projectGroup.getId());
        }
        return projectGroupDao.countProjectsByFilterGroup(
                filter.getTeamId(),
                filter.getUserId(),
                filter.getGroupId(),
                BeanUtils.getDefaultDeletedAt()
        );
    }

    public void delRelationOfProAndGro(
            ProjectGroup projectGroup,
            List<Integer> projectIdList,
            Integer userId
    ) throws CoreException {
        if (projectGroup == null || userId == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        List<Project> projectList = projectDao.getProjectsByIds(
                projectIdList,
                BeanUtils.getDefaultDeletedAt(),
                BeanUtils.getDefaultArchivedAt()
        );
        if (projectList.size() != projectIdList.size()) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        // 批量读写优化掉了循环
        List<Integer> deleteProjectGroupProjectList = projectGroupProjectDao
                .getByProjectIdsAndUserId(projectIdList, userId, BeanUtils.getDefaultDeletedAt())
                .stream()
                .map(ProjectGroupProject::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(deleteProjectGroupProjectList)) {
            projectGroupProjectDao.batchDelete(deleteProjectGroupProjectList);
        }
    }

    public void addProjectToGroup(
            ProjectGroup projectGroup,
            List<Integer> projectIdList,
            Integer userId
    ) throws CoreException {
        if (projectGroup == null || userId == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        List<ProjectGroupProject> projectGroupProjectList = new ArrayList<>();
        List<ProjectGroupProject> updateProjectGroupProjectList = new ArrayList<>();

        List<Project> projectList = projectDao.getProjectsByIds(
                projectIdList,
                BeanUtils.getDefaultDeletedAt(),
                BeanUtils.getDefaultArchivedAt()
        );
        if (projectList.size() != projectIdList.size()) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        // 通过批量查询减少IO时间
        List<ProjectGroupProject> projectGroupProjects = projectGroupProjectDao
                .getByProjectIdsAndUserId(projectIdList, userId, BeanUtils.getDefaultDeletedAt());

        // 一个项目对应一条项目分组关系记录
        for (int i = 0; i < projectList.size(); i++) {
            ProjectGroupProject projectGroupProject = null;
            if (!projectGroupProjects.isEmpty()) {
                projectGroupProject = projectGroupProjects.get(i);
            }
            if (projectGroupProject == null) {
                ProjectGroupProject newProjectGroupProject = new ProjectGroupProject();
                newProjectGroupProject.setProjectGroupId(projectGroup.getId());
                newProjectGroupProject.setProjectId(projectList.get(i).getId());
                newProjectGroupProject.setOwnerId(userId);
                projectGroupProjectList.add(newProjectGroupProject);
            } else if (!projectGroupProject.getProjectGroupId().equals(projectGroup.getId())) {
                projectGroupProject.setProjectGroupId(projectGroup.getId());
                updateProjectGroupProjectList.add(projectGroupProject);
            }
        }
        if ((!CollectionUtils.isNotEmpty(projectGroupProjectList)
                || projectGroupProjectDao.batchInsert(projectGroupProjectList) <= 0)) {
            if (CollectionUtils.isNotEmpty(updateProjectGroupProjectList)) {
                projectGroupProjectDao.batchUpdate(updateProjectGroupProjectList);
            }
        }

    }
}
