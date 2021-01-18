package net.coding.lib.project.service;

import net.coding.grpc.client.platform.TeamProjectServiceGrpcClient;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.UpdateProjectForm;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import java.sql.Date;
import java.util.regex.Pattern;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.team.project.TeamProjectProto;

import static net.coding.common.constants.CommonConstants.DATA_REGEX;
import static net.coding.common.constants.ProjectConstants.PROJECT_NAME_REGEX;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MIN_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_CLOUD_MAX_LENGTH;

/**
 * @Author liuying
 * @Date 2021/1/14 10:45 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class ProjectValidateService {

    private final TeamProjectServiceGrpcClient teamProjectServiceGrpcClient;

    private final ProfanityWordService profanityWordService;

    private final ProjectDao projectDao;

    public Project getProjectByOwnerIdAndName(Integer userOwnerId, String projectName) {
        Project project = Project.builder().name(projectName).userOwnerId(userOwnerId).build();
        return projectDao.getProject(project);
    }

    public Project getProjectByOwnerIdAndDisplayName(Integer userOwnerId, String displayName) {
        Project project = Project.builder().displayName(displayName).userOwnerId(userOwnerId).build();
        return projectDao.getProject(project);
    }

    public void validate(Project project, UpdateProjectForm updateProjectForm, Errors errors) throws CoreException {

        //敏感词校验
        profanityWordService.process(errors, updateProjectForm);

        if (errors.getFieldErrorCount() > 0) {
            return;
        }
        String name = updateProjectForm.getName().replace(" ", "-");
        if (!checkProjectName(name)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NAME_ERROR);
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

    public static boolean checkProjectName(String projectName) {
        return !(!projectName.matches(PROJECT_NAME_REGEX) || projectName.endsWith(".git"));
    }

    public void validateDisplayName(UpdateProjectForm updateProjectForm, Project targetProject) throws CoreException {
        String displayName = updateProjectForm.getDisplayName();
        if (StringUtils.isBlank(displayName)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_DISPLAY_NAME_IS_EMPTY);
        }
        if (displayName.length() < PROJECT_DISPLAY_NAME_MIN_LENGTH
                || displayName.length() > PROJECT_NAME_CLOUD_MAX_LENGTH) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_DISPLAY_NAME_LENGTH_ERROR,
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
            //2.个人项目--某个人名下所有项目不能重名
            Project p = getProjectByOwnerIdAndName(project.getOwnerId(), name);
            return null != p && !project.getId().equals(p.getId());
        }
        return false;
    }

    private boolean isDisplayNameExists(String displayName, Project targetProject) {
        if (StringUtils.equals(displayName, targetProject.getDisplayName())) {
            return false;
        }

        //1.查询是否团队项目,若是团队项目则--团队范围内项目不能重名
        Integer teamId = getTeamId(targetProject);
        if (teamId != null) {
            return existProjectDisplayName(displayName, teamId);
        }

        //2.个人项目--某个人名下所有项目不能重名
        Project p = getProjectByOwnerIdAndDisplayName(targetProject.getOwnerId(), displayName);
        return null != p && !targetProject.getId().equals(p.getId());

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

}
