package net.coding.lib.project.service.project;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.enums.QueryType;
import net.coding.lib.project.enums.RoleType;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.parameter.ProjectPrincipalJoinedQueryParameter;
import net.coding.lib.project.parameter.ProjectPrincipalQueryPageParameter;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.service.ProjectDTOService;
import net.coding.lib.project.service.ProjectPinService;
import net.coding.lib.project.service.ProjectValidateService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static net.coding.lib.project.enums.ProgramProjectEventEnums.ACTION.ACTION_VIEW;
import static org.apache.logging.log4j.util.Strings.EMPTY;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectsService {

    private final ProjectDao projectDao;

    private final ProjectMemberDao projectMemberDao;

    private final ProjectDTOService projectDTOService;

    private final ProjectPinService projectPinService;

    private final ProjectAdaptorFactory projectAdaptorFactory;

    private final ProjectValidateService projectValidateService;

    private final ProjectMemberInspectService projectMemberInspectService;

    /**
     * 项目首页 列表查询
     */
    public ResultPage<ProjectDTO> getProjectPages(ProjectPageQueryParameter parameter) throws CoreException {
        if (parameter.getQueryType().equals(QueryType.ALL.name())) {
            projectAdaptorFactory.create(PmTypeEnums.PROJECT.getType())
                    .hasPermissionInEnterprise(parameter.getTeamId(),
                            parameter.getUserId(),
                            PmTypeEnums.PROJECT.getType(),
                            ACTION_VIEW);
        } else if (parameter.getQueryType().equals(QueryType.MANAGED.name())
                || parameter.getQueryType().equals(QueryType.JOINED.name())) {
            //参与的项目
            Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(parameter.getTeamId())
                            .userId(parameter.getUserId())
                            .pmType(PmTypeEnums.PROJECT.getType())
                            .deletedAt(parameter.getDeletedAt())
                            .build());
            if (parameter.getQueryType().equals(QueryType.MANAGED.name())) {
                Set<Integer> manageProjectIds = projectMemberInspectService.listResourcesOnUser(
                        parameter.getUserId(),
                        PmTypeEnums.PROJECT.name(),
                        joinedProjectIds,
                        parameter.getUserId().longValue(),
                        RoleType.ProjectAdmin.name()
                );
                parameter.setJoinedProjectIds(manageProjectIds);
            }
            if (parameter.getQueryType().equals(QueryType.JOINED.name())) {
                parameter.setJoinedProjectIds(joinedProjectIds);
            }
            if (CollectionUtils.isEmpty(parameter.getJoinedProjectIds())) {
                List<ProjectDTO> projectDTOList = new ArrayList<>();
                return new ResultPage<>(projectDTOList, parameter.getPage(), parameter.getPageSize(), projectDTOList.size());
            }
        }
        projectValidateService.validateGroupId(parameter);
        PageInfo<Project> pageInfo = PageHelper.startPage(parameter.getPage(), parameter.getPageSize())
                .doSelectPageInfo(() -> projectDao.getProjectPages(parameter));
        List<ProjectDTO> projectDTOList = pageInfo.getList().stream()
                .map(projectDTOService::toDetailDTO)
                .peek(p -> {
                    p.setPin(projectPinService.getByProjectIdAndUserId(p.getId(), parameter.getUserId()).isPresent());
                    p.setUn_read_activities_count(0);
                })
                .collect(Collectors.toList());
        return new ResultPage<>(projectDTOList, parameter.getPage(), parameter.getPageSize(), pageInfo.getTotal());
    }


    /**
     * 主体(用户组/用户) 所在项目列表
     */
    public ResultPage<ProjectDTO> getPrincipalProjectPages(Integer teamId,
                                                           Integer operatorId,
                                                           String principalType,
                                                           String principalId,
                                                           Long policyId,
                                                           String keyword,
                                                           LimitedPager pager) {

        Set<Integer> projectIds;
        if (Objects.nonNull(policyId) && policyId > 0) {
            projectIds = projectMemberInspectService.listResource(operatorId, principalType, principalId, policyId);
        } else {
            List<ProjectMember> joinedUserMembers = new ArrayList<>();
            ProjectMemberPrincipalQueryParameter parameter = ProjectMemberPrincipalQueryParameter.builder()
                    .teamId(teamId)
                    .principalType(principalType)
                    .principalIds(StreamEx.of(principalId).toSet())
                    .pmType(PmTypeEnums.PROJECT.getType())
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
            if (ProjectMemberPrincipalTypeEnum.USER.name().equals(principalType)) {
                parameter.setUserId(Integer.valueOf(principalId));
                joinedUserMembers = projectMemberDao.findJoinPrincipalMembers(parameter);
            }
            projectIds = StreamEx.of(joinedUserMembers, projectMemberDao.findPrincipalMembers(parameter))
                    .flatMap(Collection::stream)
                    .nonNull()
                    .map(ProjectMember::getProjectId)
                    .toSet();
        }
        if (CollectionUtils.isEmpty(projectIds)) {
            List<ProjectDTO> grantProjectDTOs = new ArrayList<>();
            return new ResultPage<>(grantProjectDTOs, pager.getPage(), pager.getPageSize(), grantProjectDTOs.size());
        }
        Set<Integer> finalProjectIds = projectIds;
        PageInfo<Project> pageInfo = PageHelper.startPage(pager.getPage(), pager.getPageSize())
                .doSelectPageInfo(() -> projectDao.getPrincipalProjects(
                        ProjectPrincipalQueryPageParameter.builder()
                                .teamId(teamId)
                                .projectIds(finalProjectIds)
                                .keyword(keyword)
                                .build()
                        )
                );
        List<ProjectDTO> grantProjectDTOs = StreamEx.of(pageInfo.getList())
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
        return new ResultPage<>(grantProjectDTOs, pager.getPage(), pager.getPageSize(), pageInfo.getTotal());
    }

    /**
     * 团队下所有项目
     */
    public List<Project> getProjects(ProjectQueryParameter parameter) {
        return projectDao.getProjects(parameter);
    }

    public List<Project> getProjectsWithDeleted(ProjectQueryParameter parameter) {
        return projectDao.getProjectsWithDeleted(parameter);
    }

    /**
     * 我参与的项目列表
     */
    public List<ProjectDTO> getUserProjectDTOs(Integer teamId, Integer userId, String keyword) {
        return StreamEx.of(getJoinedPrincipalProjects(teamId, userId, keyword))
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
    }

    /**
     * 我参与的项目列表
     */
    public List<Project> getJoinedPrincipalProjects(Integer teamId, Integer userId, String keyword) {
        Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                ProjectMemberPrincipalQueryParameter.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .pmType(PmTypeEnums.PROJECT.getType())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build()
        );
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        return projectDao.getJoinedPrincipalProjects(ProjectPrincipalJoinedQueryParameter.builder()
                .teamId(teamId)
                .keyword(keyword)
                .joinedProjectIds(joinedProjectIds)
                .build());
    }

    /**
     * 有查询全部项目权限 则所有项目否则参与的项目
     */
    public List<ProjectDTO> getJoinedProjectDTOs(Integer teamId, Integer userId, String keyword) throws CoreException {
        return StreamEx.of(getJoinedProjects(teamId, userId, keyword))
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
    }

    /**
     * 有查询全部项目权限 则所有项目否则参与的项目
     */
    public List<Project> getJoinedProjects(Integer teamId, Integer userId, String keyword) throws CoreException {
        boolean hasEnterprisePermission = projectAdaptorFactory.create(PmTypeEnums.PROJECT.getType())
                .hasEnterprisePermission(teamId, userId, PmTypeEnums.PROJECT.getType(), ACTION_VIEW);
        if (!hasEnterprisePermission) {
            return getJoinedPrincipalProjects(teamId, userId, keyword);
        }
        return getProjects(ProjectQueryParameter.builder()
                .teamId(teamId)
                .keyword(keyword)
                .build());
    }

    /**
     * 根据IDs 批量查询项目
     */
    public List<Project> getByProjectIds(Set<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return StreamEx.of(
                projectDao.getByIds(
                        StreamEx.of(ids).collect(Collectors.toList()),
                        BeanUtils.getDefaultDeletedAt())
        )
                .nonNull()
                .toList();
    }

    /**
     * 根据IDs 批量查询项目
     */
    public List<Project> getByProjectIdsWithDeleted(Set<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return StreamEx.of(
                projectDao.selectByIdList(StreamEx.of(ids).toList()))
                .nonNull()
                .toList();
    }

    /**
     * 批量IDs 查询项目，要为我参与的项目
     */
    public List<ProjectDTO> getByProjectIdDTOs(Integer teamId, Integer userId, Set<Integer> ids) throws CoreException {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return StreamEx.of(getJoinedProjects(teamId, userId, EMPTY))
                .nonNull()
                .filter(p -> ids.contains(p.getId()))
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
    }
}
