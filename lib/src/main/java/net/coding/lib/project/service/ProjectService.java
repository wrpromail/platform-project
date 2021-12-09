package net.coding.lib.project.service;

import com.google.common.collect.Lists;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
import net.coding.exchange.dto.team.Team;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectRecentViewDao;
import net.coding.lib.project.dao.TeamProjectDao;
import net.coding.lib.project.dao.pojo.ProjectSearchFilter;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectRecentView;
import net.coding.lib.project.entity.TeamProject;
import net.coding.lib.project.enums.CacheTypeEnum;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.QueryProgramForm;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.group.ProjectGroupProject;
import net.coding.lib.project.group.ProjectGroupProjectDao;
import net.coding.lib.project.grpc.client.AgileTemplateGRpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.infra.PinyinService;
import net.coding.lib.project.metrics.ProjectCreateMetrics;
import net.coding.lib.project.parameter.BaseCredentialParameter;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.parameter.ProjectUpdateParameter;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.project.ProjectAdaptorFactory;
import net.coding.lib.project.setting.ProjectSettingDefault;
import net.coding.lib.project.setting.ProjectSettingFunctionService;
import net.coding.lib.project.setting.ProjectSettingService;
import net.coding.lib.project.template.ProjectTemplateDemoType;
import net.coding.lib.project.template.ProjectTemplateService;
import net.coding.lib.project.template.ProjectTemplateType;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.user.UserProto;

import static net.coding.common.constants.ProjectConstants.ACTION_ARCHIVE;
import static net.coding.common.constants.ProjectConstants.ACTION_DELETE;
import static net.coding.common.constants.ProjectConstants.ACTION_UNARCHIVE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DATE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DESCRIPTION;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_DISPLAY_NAME;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE_NAME;
import static net.coding.common.constants.ProjectConstants.ARCHIVE_PROJECT_DELETED_AT;
import static net.coding.common.constants.ProjectConstants.INFINITY_MEMBER;
import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.ACTION.ACTION_VIEW;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.createProject;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_UNARCHIVE_NAME_DUPLICATED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;


@Service
@Slf4j
@AllArgsConstructor
public class ProjectService {

    private final ProjectDao projectDao;
    private final TeamProjectDao teamProjectDao;
    private final ProjectRecentViewDao projectRecentViewDao;
    private final ProjectDTOService projectDTOService;
    private final ProjectMemberService projectMemberService;
    private final ProjectValidateService projectValidateService;
    private final ProjectServiceHelper projectServiceHelper;
    private final ProjectHandCacheService projectHandCacheService;
    private final TeamServiceGrpcClient teamServiceGrpcClient;
    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;
    private final ProjectSettingService projectSettingService;
    private final ProjectGroupProjectDao projectGroupProjectDao;
    private final ProjectPreferenceService projectPreferenceService;
    private final ProjectCredentialService projectCredentialService;
    private final TransactionTemplate transactionTemplate;
    private final AgileTemplateGRpcClient agileTemplateGRpcClient;
    private final ProjectAdaptorFactory projectAdaptorFactory;
    private final PinyinService pinyinService;

    private final ProjectPinService projectPinService;
    private final ProjectTemplateService projectTemplateService;
    private final ProjectSettingFunctionService projectSettingFunctionService;
    private final ProjectMemberInspectService projectMemberInspectService;

    public Project getById(Integer id) {
        return projectDao.getProjectById(id);
    }

    public Project getProjectWithDeleted(Integer id) {
        return projectDao.selectByPrimaryKey(id);
    }

    public Project getWithArchivedByIdAndTeamId(Integer id, Integer teamOwnerId) {
        return projectDao.getProjectNotDeleteByIdAndTeamId(id, teamOwnerId);
    }

    public Project getByIdAndTeamId(Integer id, Integer teamOwnerId) {
        return projectDao.getProjectByIdAndTeamId(id, teamOwnerId);
    }

    public Project getByNameAndTeamId(String projectName, Integer teamOwnerId) {
        return projectDao.getProjectByNameAndTeamId(projectName, teamOwnerId);
    }

    public Project getByDisplayNameAndTeamId(String displayName, Integer teamOwnerId) {
        return projectDao.getProjectByDisplayNameAndTeamId(displayName, teamOwnerId);
    }

    public List<Project> getUserProjects(ProjectQueryParameter parameter) {
        Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                ProjectMemberPrincipalQueryParameter.builder()
                        .teamId(parameter.getTeamId())
                        .userId(parameter.getUserId())
                        .pmType(PmTypeEnums.PROJECT.getType())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build());
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        parameter.setJoinedProjectIds(joinedProjectIds);
        return projectDao.getUserProjects(parameter);
    }

    public ResultPage<Project> getProjects(ProjectQueryParameter parameter, LimitedPager pager) {
        PageInfo<Project> pageInfo = PageHelper.startPage(pager.getPage(), pager.getPageSize())
                .doSelectPageInfo(() -> projectDao.getProjects(parameter));
        return new ResultPage<>(pageInfo.getList(), pager.getPage(), pager.getPageSize(), pageInfo.getTotal());
    }

    public ResultPage<ProjectDTO> getProjectPages(ProjectPageQueryParameter parameter) throws CoreException {
        if (parameter.getQueryType().equals(QueryProgramForm.QueryType.ALL.name())) {
            projectAdaptorFactory.create(PmTypeEnums.PROJECT.getType())
                    .hasPermissionInEnterprise(parameter.getTeamId(),
                            parameter.getUserId(),
                            PmTypeEnums.PROJECT.getType(),
                            ACTION_VIEW);
        }
        projectValidateService.validateGroupId(parameter);
        PageInfo<Project> pageInfo = PageHelper.startPage(parameter.getPage(), parameter.getPageSize())
                .doSelectPageInfo(() -> projectDao.getProjectPages(parameter));
        List<ProjectDTO> programDTOList = pageInfo.getList().stream()
                .map(projectDTOService::toDetailDTO)
                .peek(p -> {
                    p.setPin(projectPinService.getByProjectIdAndUserId(p.getId(), parameter.getUserId()).isPresent());
                    p.setUn_read_activities_count(0);
                })
                .collect(Collectors.toList());
        return new ResultPage<>(programDTOList, parameter.getPage(), parameter.getPageSize(), pageInfo.getTotal());
    }

    public Project createProject(ProjectCreateParameter parameter) throws Exception {
        Team team = teamServiceGrpcClient.getTeam(parameter.getTeamId());
        if (Objects.isNull(team)) {
            throw CoreException.of(CoreException.ExceptionType.TEAM_NOT_EXIST);
        }
        if (!teamServiceGrpcClient.isMember(team.getId(), parameter.getUserId())) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        parameter.setTeamOwnerId(team.getOwner_id());
        parameter.setShouldInitDepot(
                projectTemplateService.isInitDepot(
                        ProjectTemplateType.valueFrom(parameter.getProjectTemplate()),
                        ProjectTemplateDemoType.valueFrom(parameter.getTemplate())
                )
        );
        //校验创建项目相关参数
        projectValidateService.validateCreateProjectParameter(parameter);

        long prevTime = System.currentTimeMillis();

        Project project = initializeProject(parameter);

        ProjectCreateMetrics.setInitProjectData(System.currentTimeMillis() - prevTime);

        Credential credential = createCredential(project, parameter);

        projectServiceHelper.postProjectCreateEvent(project, parameter, credential);
        // 根据模板类型控制功能开关的初始化
        List<ProjectSettingDefault> functions = projectSettingFunctionService.getFunctions();
        projectSettingService.initCreateProjectSetting(project.getId(), parameter, functions);
        // demo模版初始化数据
        if (Objects.nonNull(ProjectTemplateDemoType.valueFrom(parameter.getTemplate()))) {
            agileTemplateGRpcClient.dataInitByProjectTemplate(
                    project.getId(),
                    parameter.getUserId(),
                    parameter.getProjectTemplate(),
                    parameter.getTemplate()
            );
        }
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public Project initializeProject(ProjectCreateParameter parameter) throws Exception {
        Project project = transactionTemplate.execute(status -> {
            int r = RandomUtils.nextInt(14) + 1;
            String icon = StringUtils.defaultIfBlank(parameter.getIcon(),
                    "/static/project_icon/scenery-version-2-" + r + ".svg");
            Project insertProject = Project.builder()
                    .userOwnerId(0)
                    .ownerId(0)
                    .teamOwnerId(parameter.getTeamId())
                    .type(NumberUtils.toInt(parameter.getType()))
                    .depotShared(parameter.getShared() == 1)
                    .name(parameter.getName())
                    .displayName(parameter.getDisplayName())
                    .namePinyin(pinyinService.getPinYin(
                            parameter.getDisplayName(),
                            parameter.getName()))
                    .maxMember(INFINITY_MEMBER)
                    .description(parameter.getDescription())
                    .icon(icon)
                    .invisible(parameter.getInvisible())
                    .label(StringUtils.defaultString(parameter.getLabel()))
                    .build();
            projectDao.insertSelective(insertProject);
            teamProjectDao.insertSelective(TeamProject.builder()
                    .projectId(insertProject.getId())
                    .teamId(parameter.getTeamId())
                    .createdAt(DateUtil.getCurrentDate())
                    .updatedAt(DateUtil.getCurrentDate())
                    .deletedAt(DateUtil.strToDate(BeanUtils.NOT_DELETED_AT))
                    .build());
            // 项目分组
            if (parameter.getGroupId() != null && parameter.getGroupId() > 0) {
                projectGroupProjectDao.insertSelective(ProjectGroupProject.builder()
                        .ownerId(parameter.getUserId())
                        .projectGroupId(parameter.getGroupId())
                        .projectId(insertProject.getId())
                        .build());
            }
            return insertProject;
        });
        if (Objects.isNull(project)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_CREATION_ERROR);
        }
        // 通知 企业所有者
        if (!project.getInvisible()) {
            projectServiceHelper.sendCreateProjectNotification(
                    parameter.getTeamOwnerId(),
                    parameter.getUserId(),
                    project,
                    createProject);
        }

        // 初始化新项目的内置角色，这个必须在 addMember 之前
        advancedRoleServiceGrpcClient.initProjectPredefinedRoles(project.getId(), parameter.getTeamId());

        projectMemberService.doAddMember(parameter.getUserId(), Collections.singletonList(parameter.getUserId()),
                ADMIN, project, false);

        // 创建项目默认的偏好设置 由于创建项目较慢这里 7 次 insert 把这个转移到异步
        projectPreferenceService.initProjectPreferences(project.getId());

        return project;
    }

    private Credential createCredential(Project project, ProjectCreateParameter parameter) throws CoreException {
        BaseCredentialParameter credentialParameter = parameter.getBaseCredentialParameter();
        if (credentialParameter == null || StringUtils.isBlank(credentialParameter.getType())) {
            return null;
        }
        projectCredentialService.validParam(parameter.getTeamId(), project.getId(), parameter.getUserGk());
        try {
            CredentialForm credentialForm = buildForm(credentialParameter, parameter, project.getId());
            int connId = projectCredentialService.createCredential(credentialForm, true);
            return projectCredentialService.get(connId, false);
        } catch (Exception e) {
            log.warn("create credential failed", e);
        }
        return null;
    }

    public void delete(Integer userId, Integer teamId, Integer projectId) throws CoreException {
        Project project = getByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }

        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_DELETE);

        project.setDeletedAt(DateUtil.getCurrentDate());
        projectDao.updateByPrimaryKeySelective(project);

        //删除项目集下关联项目
        projectAdaptorFactory.create(project.getPmType())
                .deleteProgramMember(teamId, userId, project);

        //清除缓存
        projectHandCacheService.handleProjectCache(project, CacheTypeEnum.DELETE);

        projectAdaptorFactory.create(project.getPmType())
                .postProjectDeleteEvent(userId, project, ACTION_DELETE);
    }

    public ProjectDTO updateIcon(Integer teamId, Integer userId, Integer projectId, String icon) throws CoreException {
        Project project = getByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }

        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_UPDATE);

        icon = projectValidateService.validateIcon(icon);
        project.setIcon(icon);
        projectDao.updateIcon(project.getId(), icon);

        projectHandCacheService.handleProjectCache(project, CacheTypeEnum.UPDATE);
        //发送事件通知
        projectAdaptorFactory.create(project.getPmType())
                .postActivityEvent(userId, project, ACTION_UPDATE);
        return projectDTOService.toDetailDTO(project);
    }

    public ProjectDTO update(Integer teamId, Integer userId, UpdateProjectForm form) throws CoreException, MilestoneException {
        Project project = getByIdAndTeamId(NumberUtils.toInt(form.getId()), teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_UPDATE);

        updateProject(form, project, userId);
        return projectDTOService.toDetailDTO(getByIdAndTeamId(project.getId(), teamId));
    }

    public boolean updateVisit(Integer projectId) throws CoreException {
        UserProto.User currentUser = SystemContextHolder.get();
        if (Objects.isNull(currentUser)) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        Project project = getByIdAndTeamId(projectId, currentUser.getTeamId());
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(projectId, currentUser.getId());
        if (!projectMemberService.isMember(currentUser, projectId)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        // TODO: henry: member type
        boolean result = projectMemberService.updateVisitTime(projectMember.getId());
        if (result) {
            projectHandCacheService.handleUnReadCache(projectId, currentUser.getId());
        }
        return result;
    }

    public void archive(Integer teamId, Integer userId, Integer projectId) throws CoreException {
        Project project = getByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }

        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_ARCHIVE);

        project.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        project.setDeletedAt(DateUtil.strToDate(ARCHIVE_PROJECT_DELETED_AT));
        projectDao.updateByPrimaryKeySelective(project);
        Optional.ofNullable(
                teamProjectDao.selectOne(TeamProject.builder()
                        .projectId(projectId)
                        .teamId(teamId)
                        .build()))
                .ifPresent(tp -> {
                    tp.setDeletedAt(DateUtil.strToDate(ARCHIVE_PROJECT_DELETED_AT));
                    teamProjectDao.updateByPrimaryKeySelective(tp);
                });
        //清除缓存
        projectHandCacheService.handleProjectCache(project, CacheTypeEnum.UPDATE);
        projectAdaptorFactory.create(project.getPmType())
                .postProjectArchiveEvent(userId, project, ACTION_ARCHIVE);
    }

    public void unarchive(Integer teamId, Integer userId, Integer projectId) throws CoreException {
        Project project = projectDao.getProjectArchiveByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }

        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_UNARCHIVE);

        Project existProjectName = getByNameAndTeamId(project.getName(), project.getTeamOwnerId());
        if (Objects.nonNull(existProjectName)) {
            throw CoreException.of(PROJECT_UNARCHIVE_NAME_DUPLICATED);
        }
        Project existProjectDisplayName = getByDisplayNameAndTeamId(project.getName(), project.getTeamOwnerId());
        if (Objects.nonNull(existProjectDisplayName)) {
            throw CoreException.of(PROJECT_UNARCHIVE_NAME_DUPLICATED);
        }
        project.setDeletedAt(DateUtil.strToDate(BeanUtils.NOT_DELETED_AT));
        projectDao.updateByPrimaryKeySelective(project);
        Optional.ofNullable(
                teamProjectDao.selectOne(TeamProject.builder()
                        .projectId(projectId)
                        .teamId(teamId)
                        .build()))
                .ifPresent(tp -> {
                    tp.setDeletedAt(DateUtil.strToDate(BeanUtils.NOT_DELETED_AT));
                    teamProjectDao.updateByPrimaryKeySelective(tp);
                });
        //清除缓存
        projectHandCacheService.handleProjectCache(project, CacheTypeEnum.UPDATE);
        projectAdaptorFactory.create(project.getPmType())
                .postProjectUnArchiveEvent(userId, project, ACTION_UNARCHIVE);
    }

    public ProjectDTO getJoinProjectByName(Integer teamId, Integer userId, String projectName) throws CoreException {
        Project project = getByNameAndTeamId(projectName, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        if (Objects.isNull(projectMemberService.getByProjectIdAndUserId(project.getId(), userId))) {
            throw CoreException.of(PERMISSION_DENIED);
        }

        projectAdaptorFactory.create(project.getPmType())
                .checkProgramPay(teamId);

        Optional.ofNullable(
                projectRecentViewDao.selectOne(ProjectRecentView.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .projectId(project.getId())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build()))
                .map(projectRecentView -> {
                    projectRecentView.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    return projectRecentViewDao.updateByPrimaryKeySelective(projectRecentView);
                })
                .orElseGet(() -> projectRecentViewDao.insertSelective(ProjectRecentView.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .projectId(project.getId())
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build()
                ));
        return projectDTOService.toDetailDTO(project);
    }

    public ProjectDTO getProjectViewByName(Integer teamId, Integer userId, String projectName) throws CoreException {
        Project project = getByNameAndTeamId(projectName, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        projectAdaptorFactory.create(project.getPmType())
                .hasPermissionInEnterprise(teamId, userId, project.getPmType(), ACTION_VIEW);
        return projectDTOService.toDetailDTO(project);
    }

    public void updateProject(UpdateProjectForm form, Project project, Integer userId) throws CoreException, MilestoneException {
        //校验参数
        projectValidateService.validateUpdateProjectParameter(form);

        boolean postProjectNameFlag = false;
        boolean postDisplayNameFlag = false;
        boolean postDescriptionFlag = false;
        boolean postDateFlag = false;
        String oldDisplayName = project.getDisplayName();
        if (!project.getName().equals(form.getName())) {
            if (Objects.nonNull(getByNameAndTeamId(form.getName(), project.getTeamOwnerId()))) {
                throw CoreException.of(PROJECT_NAME_EXISTS);
            }
            project.setName(form.getName());
            postProjectNameFlag = true;
        }
        if (!project.getDisplayName().equals(form.getDisplayName())) {
            if (Objects.nonNull(getByDisplayNameAndTeamId(form.getDisplayName(), project.getTeamOwnerId()))) {
                throw CoreException.of(PROJECT_DISPLAY_NAME_EXISTS);
            }
            project.setDisplayName(form.getDisplayName());
            postDisplayNameFlag = true;
        }

        if (!project.getDescription().equals(TextUtils.htmlEscape(form.getDescription()))) {
            project.setDescription(TextUtils.htmlEscape(form.getDescription()));
            postDescriptionFlag = true;
        }

        if ((!Objects.equals(DateUtil.formatDateToStr(project.getStartDate()), form.getStartDate())
                || !Objects.equals(DateUtil.formatDateToStr(project.getEndDate()), form.getEndDate()))) {
            project.setStartDate(projectValidateService.getStartDate(form.getStartDate()));
            project.setEndDate(projectValidateService.getEndDate(form.getEndDate()));
            projectAdaptorFactory.create(project.getPmType())
                    .checkProgramTime(project);
            postDateFlag = true;
        }

        project.setNamePinyin(pinyinService.getPinYin(project.getDisplayName(), project.getName()));
        projectDao.updateBasicInfo(ProjectUpdateParameter.builder()
                .id(project.getId())
                .description(project.getDescription())
                .displayName(project.getDisplayName())
                .name(project.getName())
                .namePinyin(project.getNamePinyin())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate()).build());

        //清除缓存
        projectHandCacheService.handleProjectCache(project, CacheTypeEnum.UPDATE);

        if (postProjectNameFlag) {
            projectAdaptorFactory.create(project.getPmType())
                    .postProjectNameChangeEvent(project);
            projectAdaptorFactory.create(project.getPmType())
                    .postActivityEvent(userId, project, ACTION_UPDATE_NAME);
        }

        if (postDisplayNameFlag) {
            projectAdaptorFactory.create(project.getPmType())
                    .postActivityEvent(userId, project, ACTION_UPDATE_DISPLAY_NAME);
        }
        if (postDescriptionFlag) {
            projectAdaptorFactory.create(project.getPmType())
                    .postActivityEvent(userId, project, ACTION_UPDATE_DESCRIPTION);
        }
        if (postDateFlag) {
            projectAdaptorFactory.create(project.getPmType())
                    .postActivityEvent(userId, project, ACTION_UPDATE_DATE);
        }

        projectAdaptorFactory.create(project.getPmType())
                .postProjectUpdateEvent(userId, project, ACTION_UPDATE,
                        postProjectNameFlag, postDisplayNameFlag, oldDisplayName);
    }

    public List<Project> getContainArchivedProjects(Integer teamId) {
        List<TeamProject> teamProjects = teamProjectDao.getContainArchivedProjects(teamId, BeanUtils.getDefaultDeletedAt(), BeanUtils.getDefaultArchivedAt());
        if (CollectionUtils.isEmpty(teamProjects)) {
            return Lists.newArrayList();
        }
        return StreamEx.of(
                projectDao.getProjectsByIds(
                        StreamEx.of(teamProjects).map(TeamProject::getProjectId).nonNull().toList(),
                        BeanUtils.getDefaultDeletedAt(),
                        BeanUtils.getDefaultArchivedAt()
                )
        ).nonNull().toList();
    }

    public CredentialForm buildForm(
            BaseCredentialParameter credentialParameter,
            ProjectCreateParameter parameter,
            Integer projectId
    ) {
        return CredentialForm.builder()
                .type(parameter.getType())
                .scope(credentialParameter.getScope())
                .connGenerateBy(ConnGenerateByEnums.valueOf(credentialParameter.getConnGenerateBy().name()))
                .id(credentialParameter.getId())
                .teamId(parameter.getTeamId())
                .projectId(projectId)
                .creatorId(parameter.getUserId())
                .credentialId(credentialParameter.getCredentialId())
                .name(credentialParameter.getName())
                .description(credentialParameter.getDescription())
                .allSelect(credentialParameter.isAllSelect())
                .build();
    }

    public List<Project> getSimpleProjectsByFilter(ProjectSearchFilter filter) {
        Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                ProjectMemberPrincipalQueryParameter.builder()
                        .teamId(filter.getTeamId())
                        .userId(filter.getUserId())
                        .pmType(PmTypeEnums.PROJECT.getType())
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build());
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        return projectDao.findByUserProjects(
                filter.getTeamId(),
                filter.getUserId(),
                filter.getKeyword(),
                filter.getGroupId(),
                joinedProjectIds,
                filter.getOffset(),
                filter.getPageSize(),
                BeanUtils.getDefaultDeletedAt()
        );
    }
}
