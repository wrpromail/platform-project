package net.coding.lib.project.group;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectGroupSameNameException;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberInspectService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private final ProjectMemberInspectService projectMemberInspectService;


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
     * ?????????????????????
     */
    public void initSystemGroup(int ownerId) {

        if (systemGroupInit(ownerId)) {
            return;
        }

        // ????????????
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setOwnerId(ownerId);
        projectGroup.setType(ProjectGroup.TYPE.ALL.toString());
        projectGroup.setName(localeMessageSource.getMessage("group_system_all_group"));
        projectGroup.setSort(-2);
        projectGroupDao.insertAndRetId(projectGroup);
        // ?????????
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
            // ?????????
            ProjectGroup minSortProjectGroup = projectGroupDao
                    .getMinSortProjectGroup(projectGroup.getOwnerId(), BeanUtils.getDefaultDeletedAt());
            projectGroup.setSort(minSortProjectGroup == null ? 0 : minSortProjectGroup.getSort() - 1);
            projectGroupDao.updateByPrimaryKeySelective(projectGroup);
        } else {
            // ???????????????
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
     * ??????????????????????????????
     */
    public long getProjectNum(ProjectGroup projectGroup) {
        if (projectGroup == null) {
            return 0;
        }
        UserProto.User user = userGrpcClient.getUserById(projectGroup.getOwnerId());
        Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                ProjectMemberPrincipalQueryParameter.builder()
                        .teamId(user.getTeamId())
                        .userId(user.getId())
                        .pmType(PmTypeEnums.PROJECT.getType())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build());
        if (projectGroup.isAll()) {
            return joinedProjectIds.size();
        }
        return projectGroupProjectDao.listByOwner(
                user.getTeamId(),
                projectGroup.isNoGroup()? ProjectGroup.NO_GROUP_ID : projectGroup.getId(),
                BeanUtils.getDefaultDeletedAt()
        )
                .stream()
                .map(ProjectGroupProject::getProjectId)
                .filter(joinedProjectIds::contains)
                .count();
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
        // ??????????????????????????????
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
        // ????????????????????????IO??????
        List<ProjectGroupProject> projectGroupProjects = projectGroupProjectDao
                .getByProjectIdsAndUserId(projectIdList, userId, BeanUtils.getDefaultDeletedAt());

        // ????????????????????????????????????????????????
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
