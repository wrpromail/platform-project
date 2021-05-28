package net.coding.lib.project.service;

import com.google.common.collect.Lists;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.cache.evict.constant.CacheType;
import net.coding.common.cache.evict.constant.TableMapping;
import net.coding.common.cache.evict.definition.MappingSerialize;
import net.coding.common.cache.evict.manager.EvictCacheManager;
import net.coding.common.redis.api.JedisManager;
import net.coding.common.util.BeanUtils;
import net.coding.exchange.dto.team.Team;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dao.ProjectGroupDao;
import net.coding.lib.project.dao.ProjectGroupProjectDao;
import net.coding.lib.project.dao.TeamProjectDao;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.ProjectGroup;
import net.coding.lib.project.entity.ProjectGroupProject;
import net.coding.lib.project.entity.ProjectSetting;
import net.coding.lib.project.entity.TeamProject;
import net.coding.lib.project.enums.CacheTypeEnum;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.enums.DemoProjectTemplateEnums;
import net.coding.lib.project.enums.ProjectTemplateEnums;
import net.coding.lib.project.enums.TemplateEnums;
import net.coding.lib.project.form.CreateProjectForm;
import net.coding.lib.project.grpc.client.AgileTemplateGRpcClient;
import net.coding.lib.project.metrics.ProjectCreateMetrics;
import net.coding.lib.project.parameter.BaseCredentialParameter;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.parameter.ProjectUpdateParameter;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.lib.project.service.download.CodingSettings;
import net.coding.common.storage.support.Storage;
import net.coding.common.storage.support.bean.ImageInfo;
import net.coding.common.storage.support.exception.StorageUploadException;
import net.coding.common.storage.support.internal.StorageUploadStream;
import net.coding.common.util.TextUtils;
import net.coding.grpc.client.permission.RoleServiceGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.user.UserProto;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_LIMIT_SIZE;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MAX_HEIGHT;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MAX_WIDTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MIN_HEIGHT;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MIN_WIDTH;
import static net.coding.common.constants.ProjectConstants.INFINITY_MEMBER;
import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.common.constants.RoleConstants.GUEST;
import static net.coding.common.constants.RoleConstants.MEMBER;
import static net.coding.common.constants.RoleConstants.MEMBER_NO_CODE;
import static net.coding.common.constants.RoleConstants.OWNER;
import static net.coding.common.constants.RoleConstants.VISITOR;
import static net.coding.lib.project.entity.ProjectSetting.Code.DEMO_TEMPLATE_TYPE;
import static net.coding.lib.project.entity.ProjectSetting.Code.PROJECT_TEMPLATE_TYPE;
import static net.coding.lib.project.exception.CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TEMPLATE_NOT_EXIST;


@Service
@Slf4j
@AllArgsConstructor
public class ProjectService {

    private final ProjectDao projectDao;

    private final Storage storage;

    private final RoleServiceGrpcClient roleServiceGrpcClient;

    private final TeamServiceGrpcClient teamServiceGrpcClient;

    private final ProjectServiceHelper projectServiceHelper;

    private final ProjectMemberService projectMemberService;

    private final CodingSettings codingSettings;

    private final ProjectValidateService projectValidateService;

    private final ProfanityWordService profanityWordService;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final ProjectSettingService projectSettingService;

    private final TeamProjectDao teamProjectDao;

    private final ProjectGroupProjectDao projectGroupProjectDao;

    private final ProjectPreferenceService projectPreferenceService;

    private final ProjectCredentialService projectCredentialService;

    private final String TABLE_NAME = "projects";

    private final String ICON_REGX_STR = ".+\\.(jpg|bmp|gif|png|jpeg)$";

    private final JedisManager jedisManager;

    private final ProjectGroupDao projectGroupDao;

    protected final TransactionTemplate transactionTemplate;

    private final AgileTemplateGRpcClient agileTemplateGRpcClient;


    public Project getById(Integer id) {
        return projectDao.getProjectById(id);
    }

    public ProjectDTO getProjectDtoById(Integer id) {
        return buildProjectDTO(getById(id));
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
        return projectDao.getUserProjects(parameter);
    }

    public ResultPage<Project> getProjects(ProjectQueryParameter parameter, LimitedPager pager) {
        PageInfo<Project> pageInfo = PageHelper.startPage(pager.getPage(), pager.getPageSize())
                .doSelectPageInfo(() -> projectDao.getProjects(parameter));
        return new ResultPage<>(pageInfo.getList(), pager.getPage(), pager.getPageSize(), pageInfo.getTotal());
    }

    public void validateUpDate(UpdateProjectForm updateProjectForm, Errors errors) throws CoreException {
        Project project = getById(projectValidateService.getId(updateProjectForm.getId()));
        if (project == null) {
            throw CoreException.of(PARAMETER_INVALID);
        }
        projectValidateService.validate(project, updateProjectForm, errors);
    }

    public int update(UpdateProjectForm form) throws CoreException {
        Project project = getById(projectValidateService.getId(form.getId()));
        if (project == null) {
            throw CoreException.of(PARAMETER_INVALID);
        }
        String nameProfanityWord = projectServiceHelper.checkContent(form.getName());
        if (StringUtils.isNotEmpty(nameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, nameProfanityWord);
        }
        String displayNameProfanityWord = projectServiceHelper.checkContent(form.getDisplayName());
        if (StringUtils.isNotBlank(displayNameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, displayNameProfanityWord);
        }
        String descriptionProfanityWord = projectServiceHelper.checkContent(form.getDescription());
        if (StringUtils.isNotEmpty(descriptionProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, descriptionProfanityWord);
        }


        // 假设 APP 还未更新，DisplayName 就是 NULL (不想用判断 UA 的方法)
        // 这时就要同步更新 DisplayName
        if (StringUtils.isBlank(form.getDisplayName()) && !StringUtils.equals(project.getName(), form.getName())) {
            form.setDisplayName(form.getName());
        } else if (StringUtils.isNotBlank(form.getDisplayName())) {
            form.setDisplayName(form.getDisplayName().trim());
        }

        boolean postProjectNameFlag = false;
        boolean postDisplayNameFlag = false;
        boolean postDescriptionFlag = false;
        boolean postDateFlag = false;
        Integer userId = 0;
        if (Objects.nonNull(SystemContextHolder.get())) {
            userId = SystemContextHolder.get().getId();
        }
        if (!project.getName().equals(form.getName())) {
            project.setName(form.getName());
            postProjectNameFlag = true;
        }

        // DisplayName NotNull 的时候要执行验证逻辑
        if (StringUtils.isNotBlank(form.getDisplayName())) {
            projectValidateService.validateDisplayName(form, project);

            if (!project.getDisplayName().equals(form.getDisplayName())) {
                project.setDisplayName(form.getDisplayName());
                postDisplayNameFlag = true;
            }
        }

        if (!project.getDescription().equals(TextUtils.htmlEscape(form.getDescription()))) {
            project.setDescription(TextUtils.htmlEscape(form.getDescription()));
            postDescriptionFlag = true;
        }
        Date formStartDate = projectValidateService.getStartDate(form.getStartDate());
        Date formEndDate = projectValidateService.getEndDate(form.getEndDate());
        if ((project.getStartDate() == null && form.getStartDate() != null)
                || (project.getEndDate() == null && form.getEndDate() != null)
                || (project.getStartDate() != null && project.getStartDate() != formStartDate)
                || (project.getEndDate() != null && project.getEndDate() != formEndDate)) {
            project.setStartDate(formStartDate);
            project.setEndDate(formEndDate);
            postDateFlag = true;

        }

        project.setNamePinyin(projectServiceHelper.getPinYin(project.getDisplayName(), project.getName()));

        ProjectUpdateParameter projectUpdateParameter = ProjectUpdateParameter.builder()
                .id(project.getId())
                .description(project.getDescription())
                .displayName(project.getDisplayName())
                .name(project.getName())
                .namePinyin(project.getNamePinyin())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate()).build();
        int result = projectDao.updateBasicInfo(projectUpdateParameter);

        if (result > 0) {
            //清除缓存
            handleCache(project, CacheTypeEnum.UPDATE);

            if (postProjectNameFlag) {
                projectServiceHelper.postProjectNameChangeEvent(project);
                projectServiceHelper.postNameActivityEvent(userId, project);
            }

            if (postDisplayNameFlag) {
                projectServiceHelper.postDisplayNameActivityEvent(userId, project);
            }
            if (postDescriptionFlag) {
                projectServiceHelper.postDescriptionActivityEvent(userId, project);
            }
            if (postDateFlag) {
                projectServiceHelper.postDateActivityEvent(userId, project);
            }
        }
        return result;

    }


    /**
     * 更新项目图标
     *
     * @return
     */
    public ProjectDTO updateProjectIcon(Integer projectId, StorageUploadStream form) throws CoreException {
        String icon = valiteIconInfo(form);
        Project project = getById(projectId);
        UserProto.User currentUser = null;
        if (Objects.nonNull(SystemContextHolder.get())) {
            currentUser = SystemContextHolder.get();
        }

        if (!roleServiceGrpcClient.checkRole(getUserRole(project, currentUser), ADMIN)) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        project.setIcon(icon);
        Integer result = projectDao.updateIcon(project.getId(), icon);
        if (result > 0) {
            handleCache(project, CacheTypeEnum.UPDATE);
            //发送事件通知
            projectServiceHelper.postIconActivityEvent(currentUser.getId(), project);

        }
        return buildProjectDTO(project);
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
        parameter.setShouldInitDepot((shouldInitDepot(parameter.getProjectTemplate(), parameter.getTemplate())));
        //校验创建项目相关参数
        validateCreateProjectParameter(parameter);

        long prevTime = System.currentTimeMillis();

        Project project = initializeProject(parameter);

        ProjectCreateMetrics.setInitProjectData(System.currentTimeMillis() - prevTime);

        Credential credential = createCredential(project, parameter);

        projectServiceHelper.postProjectCreateEvent(project, parameter, credential);
        // 根据模板类型控制功能开关的初始化
        initProjectSetting(project.getId(), parameter);
        return project;
    }

    public void validateCreateProjectParameter(ProjectCreateParameter parameter) throws CoreException {
        if (!projectValidateService.validateProjectTemplate(parameter.getProjectTemplate())) {
            throw CoreException.of(PROJECT_TEMPLATE_NOT_EXIST);
        }
        projectValidateService.validateTemplate(parameter.getTemplate());
        // 校验项目分组
        if (Objects.nonNull(parameter.getGroupId())) {
            ProjectGroup projectGroup = projectGroupDao.selectByPrimaryKey(parameter.getGroupId());
            if (Objects.isNull(projectGroup) || !projectGroup.getOwnerId().equals(parameter.getUserId())) {
                throw CoreException.of(PARAMETER_INVALID);
            }
        }
        if (!projectValidateService.checkCloudProjectName(parameter.getName())) {
            throw CoreException.of(PROJECT_NAME_ERROR);
        }
        Project existProjectDisplayName = getByDisplayNameAndTeamId(parameter.getDisplayName(), parameter.getTeamId());
        if (Objects.nonNull(existProjectDisplayName)) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_EXISTS);
        }
        Project existProjectName = getByNameAndTeamId(parameter.getName(), parameter.getTeamId());
        if (Objects.nonNull(existProjectName)) {
            throw CoreException.of(PROJECT_NAME_EXISTS);
        }
        String nameProfanityWord = profanityWordService.checkContent(parameter.getName());
        if (StringUtils.isNotEmpty(nameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, nameProfanityWord);
        }
        String displayNameProfanityWord = profanityWordService.checkContent(parameter.getDisplayName());
        if (StringUtils.isNotEmpty(displayNameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, displayNameProfanityWord);
        }
        String descriptionProfanityWord = profanityWordService.checkContent(parameter.getDescription());
        if (StringUtils.isNotEmpty(descriptionProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, descriptionProfanityWord);
        }
        if (1024 < parameter.getDescription().length()) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_DESCRIPTION_TOO_LONG);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Project initializeProject(ProjectCreateParameter parameter) throws Exception {
        Project project = transactionTemplate.execute(status -> {
            String icon = StringUtils.defaultIfBlank(parameter.getIcon(),
                    "/static/project_icon/scenery-version-2-" +
                            RandomUtils.nextInt(14) + 1 + ".svg");
            Project insertProject = Project.builder()
                    .userOwnerId(0)
                    .ownerId(0)
                    .teamOwnerId(parameter.getTeamId())
                    .type(NumberUtils.toInt(parameter.getType()))
                    .depotShared(parameter.getShared() == 1)
                    .name(parameter.getName())
                    .displayName(parameter.getDisplayName())
                    .namePinyin(projectServiceHelper.getPinYin(
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
                    project);
        }

        // 初始化新项目的内置角色，这个必须在 addMember 之前
        advancedRoleServiceGrpcClient.initProjectPredefinedRoles(project.getId(), parameter.getTeamId());

        projectMemberService.doAddMember(parameter.getUserId(), Collections.singletonList(parameter.getUserId()),
                ADMIN, project, false);

        // 创建项目默认的偏好设置 由于创建项目较慢这里 7 次 insert 把这个转移到异步
        projectPreferenceService.initProjectPreferences(project.getId());

        //新的项目隐藏掉 task
        projectSettingService.updateProjectTaskHide(project.getId(), true);

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

    public void initProjectSetting(Integer projectId, ProjectCreateParameter parameter) throws CoreException {
        if (!TemplateEnums.getTencentServerless().contains(parameter.getTemplate())) {
            ProjectTemplateEnums projectTemplateEnums = ProjectTemplateEnums.valueOf(parameter.getProjectTemplate());
            DemoProjectTemplateEnums demoProjectTemplateEnums = DemoProjectTemplateEnums.string2enum(parameter.getTemplate());
            projectSettingService.updateProjectSetting(projectId, PROJECT_TEMPLATE_TYPE.getCode(), parameter.getProjectTemplate());

            if (ProjectTemplateEnums.DEMO_BEGIN.equals(projectTemplateEnums)) {
                projectSettingService.updateProjectSetting(projectId, DEMO_TEMPLATE_TYPE.getCode(), parameter.getTemplate());
            }
            Set<ProjectSetting.Code> ownFunctions = projectTemplateEnums.getFunctions(demoProjectTemplateEnums);
            if (Objects.isNull(ownFunctions)) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            List<CreateProjectForm.ProjectFunction> FunctionModule =
                    Optional.ofNullable(parameter.getFunctionModule()).orElseGet(ArrayList::new);
            // 根据模版类型初始化部分项目开关
            ProjectSetting.TOTAL_PROJECT_FUNCTION.stream()
                    .filter(e -> !ownFunctions.contains(e))
                    .filter(e -> !FunctionModule.contains(CreateProjectForm.ProjectFunction.codeOf(e.getCode())))
                    .forEach(e -> projectSettingService.updateProjectSetting(projectId, e.getCode(), ProjectSetting.valueFalse));
            // demo模版初始化数据
            if (Objects.nonNull(demoProjectTemplateEnums)) {
                agileTemplateGRpcClient.dataInitByProjectTemplate(projectId, parameter.getUserId(),
                        parameter.getProjectTemplate(),
                        parameter.getTemplate());
            }
        }
    }

    /**
     * 删除项目
     */
    public void delete(Integer userId, Integer teamId, Integer projectId) throws CoreException {
        Project project = getByIdAndTeamId(projectId, teamId);
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        int result = projectDao.updateByPrimaryKeySelective(
                Project.builder()
                        .id(projectId)
                        .deletedAt(DateUtil.getCurrentDate())
                        .build());
        if (result < 0) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        //清除缓存
        handleCache(project, CacheTypeEnum.DELETE);
        projectServiceHelper.postProjectDeleteEvent(userId, project);
    }

    public boolean updateVisitProject(Integer projectId) throws CoreException {
        UserProto.User currentUser = SystemContextHolder.get();
        if (Objects.isNull(currentUser)) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(projectId, currentUser.getId());
        if (!projectMemberService.isMember(currentUser, projectId)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        boolean result = projectMemberService.updateVisitTime(projectMember.getId());
        if (result) {
            handleUnReadCache(projectId, currentUser.getId());
        }
        return result;
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

    public ProjectDTO getProjectByNameAndTeamId(String projectName, Integer teamOwnerId) {
        return buildProjectDTO(getByNameAndTeamId(projectName, teamOwnerId));
    }

    public List<Project> getByIds(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return projectDao.getByIds(ids, BeanUtils.getDefaultDeletedAt());
    }

    public ProjectDTO buildProjectDTO(Project project) {
        if (project == null) {
            return null;
        }

        return ProjectDTO.builder()
                .id(Optional.ofNullable(project.getId()).orElse(0))
                .description(TextUtils.htmlEscape(project.getDescription()))
                .name(project.getName())
                .display_name(project.getDisplayName())
                .start_date(formatDate(project.getStartDate()))
                .end_date(formatDate(project.getEndDate()))
                .icon(project.getIcon()).build();
    }


    private String formatDate(java.util.Date date) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE;
        if (date != null) {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.format(dateFormat);
        }
        return null;
    }

    public Short getUserRole(Project project, UserProto.User user) {
        if (user == null) {
            return GUEST;
        }

        // enterprise: do not allow none team projects
        if (codingSettings.getEnterprise().getEnable()) {
            Team team = teamServiceGrpcClient.getTeam(project.getTeamOwnerId());
            if (team == null || team.getLocked() || team.getAdmin_locked() || team.getId() != user.getTeamId()) {
                return GUEST;
            }
        }
        if ((null == project.getTeamOwnerId() || project.getTeamOwnerId() <= 0) && user.getId() == (project.getUserOwnerId().intValue())) {
            return OWNER;
        }

        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(project.getId(), user.getId());

        if (projectMember != null) {
            if (projectMember.getType().equals(OWNER)) {
                return OWNER;
            }
            if (projectMember.getType().equals(ADMIN)) {
                return ADMIN;
            }
            if (projectMember.getType().equals(MEMBER)) {
                return MEMBER;
            }
            if (projectMember.getType().equals(MEMBER_NO_CODE)) {
                return MEMBER_NO_CODE;
            }
        }

        return VISITOR;
    }

    public String valiteIconInfo(StorageUploadStream form) throws CoreException {

        String icon;
        if (form.exceed(PROJECT_ICON_LIMIT_SIZE)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_ICON_TOO_LARGE);
        }
        if (!form.checkFilenameCaseInsensitive(ICON_REGX_STR)) {
            throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
        }

        try {
            icon = form.storage();
        } catch (StorageUploadException e) {
            throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
        }
        if (icon == null) {
            throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
        }
        ImageInfo info = storage.imageInfo(icon);
        if (null == info) {
            throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
        }
        if (info.getWidth() > PROJECT_ICON_MAX_WIDTH || info.getHeight() > PROJECT_ICON_MAX_HEIGHT) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_ICON_ERROR);
        }
        if (info.getWidth() < PROJECT_ICON_MIN_WIDTH || info.getHeight() < PROJECT_ICON_MIN_HEIGHT) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_ICON_ERROR);
        }
        Pattern pattern = Pattern.compile(ICON_REGX_STR);
        if (pattern.matcher(info.getFormat().toLowerCase()).matches()) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_ICON_ERROR);
        }
        return icon;
    }

    /**
     * 更新访问项目时间时，清空动态未读条数缓存，因缓存 key 格式特殊，故新增此方法
     *
     * @param projectId
     * @param userId
     */
    protected void handleUnReadCache(Integer projectId, Integer userId) {
        String tableName = "activities";
        Optional.ofNullable(TableMapping.CACHE_DEFINITION_MAP)
                .map(MappingSerialize::getEntities)
                .map(d -> d.get(tableName))
                .ifPresent(m -> {
                    String region = m.getRegion();
                    String version = m.getVersion();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(region)
                            .append(":")
                            .append(countCacheKey(version,
                                    "unread", "projectId", projectId, "userId", userId));
                    String cacheKey = stringBuilder.toString();
                    jedisManager.setex(cacheKey, 86400 * 10, "0");
                });

    }

    protected String countCacheKey(String version, Object... params) {
        return version + ":C:" + StringUtils.join(params, '#');
    }

    protected void handleCache(Project project, CacheTypeEnum type) {
        if (type == CacheTypeEnum.UPDATE || type == CacheTypeEnum.DELETE) {
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "name", project.getName());

            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "id", project.getId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "name", project.getName(), "archiveProject");
        }
        if (type == CacheTypeEnum.CREATE || type == CacheTypeEnum.DELETE) {
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "userId", project.getUserOwnerId());
        }
        if (type == CacheTypeEnum.UPDATE) {
            // 项目归档，会更新deleted_at
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "userId", project.getUserOwnerId());
        }
        // 清除 getProjectByTeamOwnerIdAndName 方法的缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "getProjectByTeamIdAndName", "team", project.getTeamOwnerId(), "name", project.getName());
        // 清除 getArchiveProjectByTeamIdAndName 方法的缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "getArchiveProjectByTeamIdAndName", "team", project.getTeamOwnerId(), "name", project.getName());
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

    private boolean shouldInitDepot(String projectTemplate, String template) {
        switch (ProjectTemplateEnums.valueOf(projectTemplate)) {
            case PROJECT_MANAGE:
            case DEV_OPS:
            case CODE_HOST:
            case CHOICE_DEMAND:
                return false;
            case DEMO_BEGIN:
                if (DemoProjectTemplateEnums.AGILE.name().equalsIgnoreCase(template)
                        || DemoProjectTemplateEnums.TESTING.name().equalsIgnoreCase(template)
                        || DemoProjectTemplateEnums.CLASSIC.name().equalsIgnoreCase(template)) {
                    return false;
                }
            default:
                return true;
        }
    }
}
