package net.coding.lib.project.service;

import net.coding.grpc.client.platform.TeamProjectServiceGrpcClient;
import net.coding.lib.project.AppProperties;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.UpdateProjectForm;
import net.coding.lib.project.group.ProjectGroup;
import net.coding.lib.project.group.ProjectGroupService;
import net.coding.lib.project.infra.TextModerationService;
import net.coding.lib.project.parameter.ProjectCreateParameter;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.template.ProjectTemplateDemoType;
import net.coding.lib.project.template.ProjectTemplateType;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.validator.UrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import java.sql.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.team.project.TeamProjectProto;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MIN_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_CLOUD_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MIN_LENGTH;
import static net.coding.common.constants.CommonConstants.DATA_REGEX;
import static net.coding.common.constants.ProjectConstants.ARCHIVE_PROJECT_DELETED_AT;
import static net.coding.common.constants.ProjectConstants.PROJECT_NAME_CLOUD_REGEX;
import static net.coding.common.constants.ProjectConstants.PROJECT_NAME_REGEX;
import static net.coding.lib.project.exception.CoreException.ExceptionType.CONTENT_INCLUDE_SENSITIVE_WORDS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_IS_EMPTY;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_LENGTH_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_IS_EMPTY;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_LENGTH_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TEMPLATE_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_TYPE_INVALID;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_VCS_TYPE_INVALID;

/**
 * @Author liuying
 * @Date 2021/1/14 10:45 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class ProjectValidateService {

    private static final short ENABLE_SHARED = 1;

    private static final short DISABLE_SHARED = 0;

    private final ProjectDao projectDao;

    private final AppProperties appProperties;

    private final TextModerationService textModerationService;

    private final TeamProjectServiceGrpcClient teamProjectServiceGrpcClient;

    private final ProfanityWordService profanityWordService;

    private final ProjectGroupService projectGroupService;


    public void validate(Project project, UpdateProjectForm updateProjectForm, Errors errors) throws CoreException {

        //敏感词校验
        profanityWordService.process(errors, updateProjectForm);

        if (errors.getFieldErrorCount() > 0) {
            return;
        }
        String name = updateProjectForm.getName().replace(" ", "-");
        if (!checkProjectName(name)) {
            throw CoreException.of(PROJECT_NAME_ERROR);
        }

        if (IsNameExist(name, project)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NAME_EXISTS);
        }
        String startDate = updateProjectForm.getStartDate();
        String endDate = updateProjectForm.getEndDate();
        if (StringUtils.isNotBlank(startDate)) {
            boolean result = Pattern.compile(DATA_REGEX).matcher(startDate).matches();
            if (!result) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_START_DATE_ERROR);

            }
        }
        if (StringUtils.isNotBlank(endDate)) {
            boolean result = Pattern.compile(DATA_REGEX).matcher(endDate).matches();
            if (!result) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_END_DATE_ERROR);
            }
        }
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            if (getEndDate(endDate).before(getStartDate(startDate))) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_END_DATE_BEFORE_START_DATE);

            }
        }

    }

    public void validateCreateProject(ProjectCreateParameter parameter) throws CoreException {
        if (StringUtils.isBlank(parameter.getName())) {
            throw CoreException.of(PROJECT_NAME_IS_EMPTY);
        }
        if (StringUtils.isBlank(parameter.getDisplayName())) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_IS_EMPTY);
        }
        // 对外API 应 TCB, mendix 要求 projectName 长度64 位，CODING 自身前端限制32、后端以64为准
        if (parameter.getName().length() < PROJECT_NAME_MIN_LENGTH
                || parameter.getName().length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(PROJECT_NAME_LENGTH_ERROR,
                    PROJECT_NAME_MIN_LENGTH, PROJECT_NAME_CLOUD_MAX_LENGTH);
        }
        String name = parameter.getName().replace(" ", "-");
        boolean check = checkCloudProjectName(name);
        if (!check) {
            throw CoreException.of(PROJECT_NAME_ERROR);
        }

        if (parameter.getDisplayName().length() < PROJECT_DISPLAY_NAME_MIN_LENGTH
                || parameter.getDisplayName().length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_LENGTH_ERROR,
                    PROJECT_DISPLAY_NAME_MIN_LENGTH, PROJECT_NAME_CLOUD_MAX_LENGTH);
        }

        if (StringUtils.isBlank(parameter.getType()) || !NumberUtils.isNumber(parameter.getType())) {
            throw CoreException.of(PROJECT_TYPE_INVALID);
        }

        if (!"git".equalsIgnoreCase(parameter.getVcsType())
                && !"svn".equalsIgnoreCase(parameter.getVcsType())
                && !"hg".equalsIgnoreCase(parameter.getVcsType())) {
            throw CoreException.of(PROJECT_VCS_TYPE_INVALID);
        }
        validateTemplate(parameter.getTemplate());
    }

    public static boolean checkProjectName(String projectName) {
        return !(!projectName.matches(PROJECT_NAME_REGEX) || projectName.endsWith(".git"));
    }

    public void validateDisplayName(UpdateProjectForm updateProjectForm, Project targetProject) throws CoreException {
        String displayName = updateProjectForm.getDisplayName();
        if (StringUtils.isBlank(displayName)) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_IS_EMPTY);
        }
        if (displayName.length() < PROJECT_DISPLAY_NAME_MIN_LENGTH
                || displayName.length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_LENGTH_ERROR,
                    PROJECT_DISPLAY_NAME_MIN_LENGTH, PROJECT_NAME_CLOUD_MAX_LENGTH);
        }
        if (isDisplayNameExists(displayName, targetProject)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_DISPLAY_NAME_EXISTS);
        }
    }

    public Date getEndDate(String endDate) {
        if (StringUtils.isNotBlank(endDate)) {
            try {
                return Date.valueOf(endDate);
            } catch (IllegalArgumentException e) {
                log.warn("Parse task end date failed {}", e.getMessage());
            }
        }
        return null;
    }

    public Date getStartDate(String startDate) {
        if (StringUtils.isNotBlank(startDate)) {
            try {
                return Date.valueOf(startDate);
            } catch (IllegalArgumentException e) {
                log.warn("Parse task end date failed {}", e.getMessage());
            }
        }
        return null;
    }


    private boolean IsNameExist(String name, Project project) {
        if (!name.equals(project.getName())) {
            //1.查询是否团队项目,若是团队项目则--团队范围内项目不能重名
            Integer teamId = getTeamId(project);
            if (null != teamId) {
                return existProjectName(name, teamId);
            }
        }
        return false;
    }

    private boolean isDisplayNameExists(String displayName, Project targetProject) {
        if (StringUtils.equals(displayName, targetProject.getDisplayName())) {
            return false;
        }

        //1.查询是否团队项目,若是团队项目则--团队范围内项目不能重名
        Integer teamId = getTeamId(targetProject);
        return existProjectDisplayName(displayName, teamId);

    }

    public Integer getId(String id) {
        return NumberUtils.toInt(id);
    }

    public Integer getTeamId(Project project) {
        TeamProjectProto.GetTeamIdOfProjectRequest request = TeamProjectProto.GetTeamIdOfProjectRequest.newBuilder()
                .setProjectId(project.getId())
                .build();
        try {
            TeamProjectProto.GetTeamIdOfProjectResponse response = teamProjectServiceGrpcClient.getTeamIdOfProject(request);
            if (response.getCodeValue() == 0) {

                return response.getTeamId();
            }

        } catch (Exception e) {
            log.warn("getTeamId {}", e.getMessage());
        }
        return null;
    }

    public boolean existProjectName(String name, Integer teamId) {
        TeamProjectProto.ExistTeamProjectNameRequest request = TeamProjectProto.ExistTeamProjectNameRequest.newBuilder()
                .setTeamId(teamId)
                .setProjectName(name)
                .build();
        try {

            TeamProjectProto.ExistTeamProjectNameResponse response = teamProjectServiceGrpcClient.existTeamProjectName(request);
            return response.getExist();
        } catch (Exception e) {
            log.warn("existProjectName {}", e.getMessage());
        }
        return false;
    }

    public boolean existProjectDisplayName(String displayName, Integer teamId) {


        TeamProjectProto.ExistTeamDisplayNameRequest request = TeamProjectProto.ExistTeamDisplayNameRequest.newBuilder()
                .setTeamId(teamId)
                .setDisplayName(displayName)
                .build();
        try {
            TeamProjectProto.ExistTeamDisplayNameResponse response = teamProjectServiceGrpcClient.existTeamDisplayName(request);
            return response.getExist();
        } catch (Exception e) {
            log.warn("existProjectDisplayName {}", e.getMessage());
        }
        return false;
    }

    public boolean checkCloudProjectName(String projectName) {
        return !(!projectName.matches(PROJECT_NAME_CLOUD_REGEX) || projectName.endsWith(".git"));
    }

    public boolean validateProjectTemplate(String projectTemplate) {
        return StringUtils.isNotEmpty(projectTemplate)
                && EnumUtils.isValidEnum(ProjectTemplateType.class, projectTemplate);
    }

    public void validateTemplate(String template) throws CoreException {
        if (StringUtils.isNotEmpty(template)
                && ProjectTemplateDemoType.valueFrom(template) == null) {
            throw CoreException.of(PARAMETER_INVALID);
        }
    }

    public void validateCreateProjectParameter(ProjectCreateParameter parameter) throws CoreException {
        if (!validateProjectTemplate(parameter.getProjectTemplate())) {
            throw CoreException.of(PROJECT_TEMPLATE_NOT_EXIST);
        }
        validateTemplate(parameter.getTemplate());
        // 校验项目分组
        if (Objects.nonNull(parameter.getGroupId())) {
            ProjectGroup projectGroup = projectGroupService.getById(parameter.getGroupId());
            if (Objects.isNull(projectGroup) || !projectGroup.getOwnerId().equals(parameter.getUserId())) {
                throw CoreException.of(PARAMETER_INVALID);
            }
        }
        if (!checkCloudProjectName(parameter.getName())) {
            throw CoreException.of(PROJECT_NAME_ERROR);
        }
        Project existProjectDisplayName = projectDao.getProjectByDisplayNameAndTeamId(parameter.getDisplayName(), parameter.getTeamId());
        if (Objects.nonNull(existProjectDisplayName)) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_EXISTS);
        }
        Project existProjectName = projectDao.getProjectByNameAndTeamId(parameter.getName(), parameter.getTeamId());
        if (Objects.nonNull(existProjectName)) {
            throw CoreException.of(PROJECT_NAME_EXISTS);
        }
        // 创建项目使得名字不能与归档之后的项目名相同
        Project existArchiveProjectName = projectDao.getProjectArchiveByNameAndTeamId(
                parameter.getDisplayName(),
                parameter.getName(),
                parameter.getTeamId(),
                ARCHIVE_PROJECT_DELETED_AT
        );
        if (Objects.nonNull(existArchiveProjectName)) {
            throw CoreException.of(PROJECT_NAME_EXISTS);
        }
        String nameProfanityWord = textModerationService.checkContent(parameter.getName());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(nameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, nameProfanityWord);
        }
        String displayNameProfanityWord = textModerationService.checkContent(parameter.getDisplayName());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(displayNameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, displayNameProfanityWord);
        }
        String descriptionProfanityWord = textModerationService.checkContent(parameter.getDescription());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(descriptionProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, descriptionProfanityWord);
        }
        if (1024 < parameter.getDescription().length()) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_DESCRIPTION_TOO_LONG);
        }
    }

    public void validateUpdateProjectParameter(UpdateProjectForm form) throws CoreException {
        if (StringUtils.isBlank(form.getName())) {
            throw CoreException.of(PROJECT_NAME_IS_EMPTY);
        }
        if (form.getName().length() < PROJECT_NAME_MIN_LENGTH
                || form.getName().length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(PROJECT_NAME_LENGTH_ERROR,
                    PROJECT_NAME_MIN_LENGTH, PROJECT_NAME_CLOUD_MAX_LENGTH);
        }
        String name = form.getName().replace(" ", "-");
        boolean check = checkCloudProjectName(name);
        if (!check) {
            throw CoreException.of(PROJECT_NAME_ERROR);
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(form.getDisplayName())) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_IS_EMPTY);
        }
        if (form.getDisplayName().length() < PROJECT_DISPLAY_NAME_MIN_LENGTH
                || form.getDisplayName().length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_LENGTH_ERROR,
                    PROJECT_DISPLAY_NAME_MIN_LENGTH, PROJECT_NAME_CLOUD_MAX_LENGTH);
        }
        //敏感词
        String nameProfanityWord = textModerationService.checkContent(form.getName());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(nameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, nameProfanityWord);
        }
        String displayNameProfanityWord = textModerationService.checkContent(form.getDisplayName());
        if (org.apache.commons.lang3.StringUtils.isNotBlank(displayNameProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, displayNameProfanityWord);
        }
        String descriptionProfanityWord = textModerationService.checkContent(form.getDescription());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(descriptionProfanityWord)) {
            throw CoreException.of(CONTENT_INCLUDE_SENSITIVE_WORDS, descriptionProfanityWord);
        }

        String startDate = form.getStartDate();
        String endDate = form.getEndDate();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(startDate) && org.apache.commons.lang3.StringUtils.isBlank(endDate)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_END_DATE_NOT_EMPTY);
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(endDate) && org.apache.commons.lang3.StringUtils.isBlank(startDate)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_START_DATE_NOT_EMPTY);
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(startDate) && org.apache.commons.lang3.StringUtils.isNotBlank(endDate)) {
            boolean result = Pattern.compile(DATA_REGEX).matcher(endDate).matches();
            if (!result) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_END_DATE_ERROR);
            }
            result = Pattern.compile(DATA_REGEX).matcher(startDate).matches();
            if (!result) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_START_DATE_ERROR);
            }
            if (getEndDate(endDate)
                    .before(getStartDate(startDate))) {
                throw CoreException.of(CoreException.ExceptionType.PROJECT_END_DATE_BEFORE_START_DATE);
            }
        }
    }

    public void validateGroupId(ProjectPageQueryParameter parameter) throws CoreException {
        if (parameter.getGroupId() != null && parameter.getGroupId() > 0) {
            ProjectGroup projectGroup = projectGroupService.getById(parameter.getGroupId());
            if (Objects.isNull(projectGroup)
                    || (Objects.nonNull(parameter.getUserId())
                    && !parameter.getUserId().equals(projectGroup.getOwnerId()))) {
                throw CoreException.of(PARAMETER_INVALID);
            }
            if (ProjectGroup.TYPE.ALL.toString().equals(projectGroup.getType())) {
                // 全部项目，将groupId置空
                parameter.setGroupId(null);
            } else if (ProjectGroup.TYPE.NO_GROUP.toString().equals(projectGroup.getType())) {
                // 未分组项目 置0
                parameter.setGroupId(ProjectGroup.NO_GROUP_ID);
            }
        }
    }


    /**
     * 新上传逻辑采用icon
     *
     * @param icon
     * @return
     * @throws CoreException
     */
    public String validateIcon(String icon) throws CoreException {
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(icon)) {
            if (!validateImageURL(icon) || !new UrlValidator().isValid(icon)) {
                throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
            }
            return icon;
        }
        throw CoreException.of(CoreException.ExceptionType.UPDATE_PROJECT_ICON_ERROR);
    }

    public boolean validateImageURL(String url) {
        if (org.apache.commons.lang3.StringUtils.isBlank(appProperties.getIcon().getDomain())) {
            return true;
        }
        return Pattern.compile("^(?:https?|ftp)://[^.]+.(" + appProperties.getIcon().getDomain() + ")/.*$").matcher(url).find();
    }
}
