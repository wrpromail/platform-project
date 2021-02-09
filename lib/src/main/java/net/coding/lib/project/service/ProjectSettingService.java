package net.coding.lib.project.service;

import net.coding.common.base.cache.CacheManager;
import net.coding.common.cache.evict.constant.CacheType;
import net.coding.common.cache.evict.manager.EvictCacheManager;
import net.coding.common.config.PDSettings;
import net.coding.common.config.TencentOASettings;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectSettingsDao;
import net.coding.lib.project.dto.ProjectFunctionDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectSetting;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.helper.ProjectServiceHelper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.entity.ProjectSetting.TOTAL_PROJECT_FUNCTION;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;

/**
 * @Author liuying
 * @Date 2021/1/5 11:02 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class ProjectSettingService {

    private final ProjectSettingsDao projectSettingsDao;

    private final ProjectServiceHelper projectServiceHelper;

    private final TencentOASettings tencentOASettings;

    private final PDSettings pdSettings;

    public static final String CACHE_REGION = "net.coding.lib.project.service.ProjectSettingService";

    private static final String TABLE_NAME = "project_settings";


    /**
     * 获取项目功能的所有设置，无记录使用默认值。
     */
    public List<ProjectFunctionDTO> getAllFunction(Integer projectId) {
        return TOTAL_PROJECT_FUNCTION
                .stream()
                .map(function -> {
                    ProjectSetting setting = findProjectSetting(projectId, function.getCode());
                    return ProjectFunctionDTO.builder()
                            .code(function.getCode())
                            .description(function.getDescription())
                            .projectId(projectId)
                            .value(setting != null ? setting.getValue() :
                                    tencentOASettings.getIsOAVersion() ? function.getOaDefaultValue() :
                                            pdSettings.getIsPDVersion() ? ProjectSetting.getValue(getPDDefaultValue(function)) :
                                                    function.getDefaultValue()
                            )
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 修改功能状态
     */
    public boolean updateProjectFunction(Integer projectId,
                                         String function,
                                         boolean status
    ) throws CoreException {
        // 当前登录用户
        if (Objects.isNull(SystemContextHolder.get())) {
            return false;
        }
        // 匹配 项目功能
        Optional<ProjectSetting.Code> settingOptional = TOTAL_PROJECT_FUNCTION
                .stream()
                .filter(code -> code.getCode().equals(function))
                .findFirst();
        if (!settingOptional.isPresent()) {
            throw CoreException.of(PARAMETER_INVALID);
        }

        ProjectSetting.Code setting = settingOptional.get();
        String value = status ? ProjectSetting.valueTrue : ProjectSetting.valueFalse;
        ProjectSetting projectSetting = findProjectSetting(projectId, function);

        if (Objects.isNull(projectSetting)) {
            projectSetting = ProjectSetting.builder()
                    .projectId(projectId)
                    .code(setting.getCode())
                    .description(setting.getDescription())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .updatedAt(new Timestamp(System.currentTimeMillis()))
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
        } else if (projectSetting.getCode().equals(value)) {
            return false;
        }

        CacheManager.evict(CACHE_REGION, "getAllFunction:" + projectId);

        projectSetting.setValue(value);
        if (saveOrUpdateProjectSetting(projectSetting)) {
            // 1: 开启 0: 关闭
            Short action = status ? ProjectSetting.open : ProjectSetting.close;
            Integer userId = SystemContextHolder.get().getId();
            projectServiceHelper.postFunctionActivity(userId, projectSetting, action);
            return true;
        }
        return false;
    }

    /**
     * 更新项目旧的开关是否开启
     */
    public ProjectSetting updateProjectTaskHide(Integer projectId, boolean hide) {
        ProjectSetting projectSetting = findProjectSetting(projectId, ProjectSetting.Code.TASK_HIDE.getCode());
        if (null == projectSetting) {
            projectSetting = new ProjectSetting();
            projectSetting.setProjectId(projectId);
            projectSetting.setCode(ProjectSetting.Code.TASK_HIDE.getCode());
            projectSetting.setValue(hide ? ProjectSetting.valueTrue : ProjectSetting.valueFalse);
            projectSetting.setDescription(ProjectSetting.Code.TASK_HIDE.getDescription());

            projectSettingsDao.insert(projectSetting);
        } else {
            projectSetting.setValue(hide ? ProjectSetting.valueTrue : ProjectSetting.valueFalse);
            projectSettingsDao.update(projectSetting);
        }

        CacheManager.evict(CACHE_REGION, "getAllFunction:" + projectId);

        return projectSetting;
    }

    public ProjectSetting findProjectSetting(Integer projectId, String function) {
        if (Objects.isNull(projectId) || StringUtils.isEmpty(function)) {
            return null;
        }
        ProjectSetting projectSetting = ProjectSetting.builder().projectId(projectId)
                .code(function).build();
        return projectSettingsDao.findProjectSetting(projectSetting);
    }

    public boolean saveOrUpdateProjectSetting(ProjectSetting projectSetting) {
        if (Objects.isNull(projectSetting.getId())) {
            return projectSettingsDao.insert(projectSetting) > 0;
        }
        if (projectSettingsDao.update(projectSetting) > 0) {
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "project_id", projectSetting.getProjectId(), "code", projectSetting.getCode());
            return true;
        }
        return false;
    }

    public Boolean getPDDefaultValue(ProjectSetting.Code function) {
        switch (function) {
            case FUNCTION_AGILE_DEVELOPMENT:
                return !pdSettings.getAgileDevelopmentClose();
            case FUNCTION_TEST_MANAGEMENT:
                return !pdSettings.getTestManagementClose();
            case FUNCTION_CODE_MANAGEMENT:
                return !pdSettings.getCodeManagementClose();
            case FUNCTION_CONTINUE_INTEGRATION:
                return !pdSettings.getContinueIntegrationClose();
            case FUNCTION_DEPLOYMENT_MANAGEMENT:
                return !pdSettings.getDeploymentManagementClose();
            case FUNCTION_ARTIFACT:
                return !pdSettings.getArtifactClose();
            case FUNCTION_WIKI:
                return !pdSettings.getWikiClose();
            case FUNCTION_STATISTICS:
                return !pdSettings.getStatisticsClose();
            case FUNCTION_OLD_TASK:
                return !pdSettings.getOldTaskClose();
            case FUNCTION_CODE_ANALYSIS:
                return !pdSettings.getCodeAnalysisClose();
            case FUNCTION_API_DOCS:
                return !pdSettings.getApiDocsClose();
            case FUNCTION_QTA:
                return !pdSettings.getQtaClose();
            default:
                return false;
        }
    }
}
