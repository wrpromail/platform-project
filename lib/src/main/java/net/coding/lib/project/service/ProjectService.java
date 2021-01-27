package net.coding.lib.project.service;

import net.coding.common.cache.evict.constant.CacheType;
import net.coding.common.cache.evict.manager.EvictCacheManager;
import net.coding.lib.project.enums.CacheTypeEnum;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.parameter.ProjectUpdateParameter;
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
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_LIMIT_SIZE;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MAX_HEIGHT;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MAX_WIDTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MIN_HEIGHT;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_ICON_MIN_WIDTH;
import static net.coding.common.constants.RoleConstants.ADMIN;
import static net.coding.common.constants.RoleConstants.GUEST;
import static net.coding.common.constants.RoleConstants.MEMBER;
import static net.coding.common.constants.RoleConstants.MEMBER_NO_CODE;
import static net.coding.common.constants.RoleConstants.OWNER;
import static net.coding.common.constants.RoleConstants.VISITOR;


@Service
@Slf4j
@AllArgsConstructor
public class ProjectService {

    private final ProjectDao projectDao;

    private final Storage storage;

    private final RoleServiceGrpcClient roleServiceGrpcClient;

    private final TeamGrpcClient teamGrpcClient;

    private final ProjectServiceHelper projectServiceHelper;

    private final ProjectMemberService projectMemberService;

    private final CodingSettings codingSettings;

    private final ProjectValidateService projectValidateService;

    private final String TABLE_NAME = "projects";

    private final String ICON_REGX_STR = ".+\\.(jpg|bmp|gif|png|jpeg)$";


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

    public List<Project> getProjects(ProjectQueryParameter parameter) {
        return projectDao.findByProjects(parameter);
    }

    public void validateUpDate(UpdateProjectForm updateProjectForm, Errors errors) throws CoreException {
        Project project = getById(projectValidateService.getId(updateProjectForm.getId()));
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        projectValidateService.validate(project, updateProjectForm, errors);
    }

    public int update(UpdateProjectForm form) throws CoreException {
        Project project = getById(projectValidateService.getId(form.getId()));
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        String nameProfanityWord = projectServiceHelper.checkContent(form.getName());
        if (StringUtils.isNotEmpty(nameProfanityWord)) {
            throw CoreException.of(CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS, nameProfanityWord);
        }
        String displayNameProfanityWord = projectServiceHelper.checkContent(form.getDisplayName());
        if (StringUtils.isNotBlank(displayNameProfanityWord)) {
            throw CoreException.of(CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS, displayNameProfanityWord);
        }
        String descriptionProfanityWord = projectServiceHelper.checkContent(form.getDescription());
        if (StringUtils.isNotEmpty(descriptionProfanityWord)) {
            throw CoreException.of(CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS, descriptionProfanityWord);
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

    /**
     * 删除项目
     */
    public void delete(Integer userId, Integer teamId, Integer projectId) throws CoreException {
        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(
                projectId,
                userId);
        if (projectMember == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        Project project = getByIdAndTeamId(projectId, teamId);
        if (project == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        int result = projectDao.delete(projectId);
        if (result < 0) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        //清除缓存
        handleCache(project, CacheTypeEnum.DELETE);
        projectServiceHelper.postProjectDeleteEvent(userId, project);
    }


    public ProjectDTO buildProjectDTO(Project project) {
        if (project == null) {
            return null;
        }

        return ProjectDTO.builder().id(Optional.ofNullable(project.getId()).orElse(0))
                .description(TextUtils.htmlEscape(project.getDescription()))
                .name(project.getName())
                .display_name(project.getDisplayName())
                .start_date(formatDate(project.getStartDate()))
                .end_date(formatDate(project.getEndDate()))
                .icon(project.getIcon()).build();
    }


    private String formatDate(java.util.Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (date != null) {
            return dateFormat.format(date);
        }
        return null;
    }

    public Short getUserRole(Project project, UserProto.User user) {
        if (user == null) {
            return GUEST;
        }

        // enterprise: do not allow none team projects
        if (codingSettings.getEnterprise().getEnable()) {
            TeamProto.GetTeamResponse team = teamGrpcClient.getTeam(project.getTeamOwnerId());
            if (team == null || team.getData().getLock() || team.getData().getAdminLoacked() || team.getData().getId() != user.getTeamId()) {
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

}
