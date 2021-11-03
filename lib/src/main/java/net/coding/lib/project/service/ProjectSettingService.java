package net.coding.lib.project.service;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.cache.CacheManager;
import net.coding.common.cache.evict.constant.CacheType;
import net.coding.common.cache.evict.manager.EvictCacheManager;
import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.platform.SystemSettingGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectSettingsDao;
import net.coding.lib.project.dto.ProjectFunctionDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectSetting;
import net.coding.lib.project.event.ProjectSettingChangeEvent;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectNotFoundException;
import net.coding.lib.project.helper.ProjectServiceHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.system.setting.SystemSettingProto;
import proto.platform.user.UserProto;

import static net.coding.lib.project.entity.ProjectSetting.TOTAL_PROJECT_FUNCTION;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PARAMETER_INVALID;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSettingService {

    private final ProjectSettingsDao projectSettingsDao;

    private final ProjectServiceHelper projectServiceHelper;

    private final AsyncEventBus asyncEventBus;

    private final ProjectDao projectDao;

    private final SystemSettingGrpcClient systemSettingGrpcClient;

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
                    boolean disabled = isDisabledSystemMenu(function.getCode());
                    String value = Optional.ofNullable(setting).map(ProjectSetting::getValue).orElse(function.getDefaultValue());
                    if (disabled) {
                        value = ProjectSetting.valueFalse;
                    }
                    return ProjectFunctionDTO.builder()
                            .code(function.getCode())
                            .description(function.getDescription())
                            .projectId(projectId)
                            .value(value)
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
        String beforeValue;
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
        beforeValue = projectSetting.getValue();
        projectSetting.setValue(value);
        if (saveOrUpdateProjectSetting(projectSetting)) {
            // 1: 开启 0: 关闭
            Short action = status ? ProjectSetting.open : ProjectSetting.close;
            Integer userId = SystemContextHolder.get().getId();
            projectServiceHelper.postFunctionActivity(userId, projectSetting, action);
            Project project = Optional.ofNullable(projectDao.getProjectById(projectId))
                    .orElseThrow(ProjectNotFoundException::new);
            Integer operatorId = Optional.ofNullable(SystemContextHolder.get()).map(UserProto.User::getId).orElse(0);
            sendProjectSettingChangeEvent(project.getTeamOwnerId(), project.getId(), operatorId, function, value, beforeValue);
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
            projectSetting = ProjectSetting.builder()
                    .projectId(projectId)
                    .code(ProjectSetting.Code.TASK_HIDE.getCode())
                    .value(hide ? ProjectSetting.valueTrue : ProjectSetting.valueFalse)
                    .description(ProjectSetting.Code.TASK_HIDE.getDescription())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .updatedAt(new Timestamp(System.currentTimeMillis()))
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
            projectSettingsDao.insert(projectSetting);
        } else {
            projectSetting.setValue(hide ? ProjectSetting.valueTrue : ProjectSetting.valueFalse);
            projectSettingsDao.update(projectSetting);
        }

        CacheManager.evict(CACHE_REGION, "getAllFunction:" + projectId);

        return projectSetting;
    }

    /**
     * 更新模版开关
     */
    public ProjectSetting updateProjectSetting(Integer projectId, String code, String value) {
        ProjectSetting projectSetting = findProjectSetting(projectId, code);
        String beforeValue = null;
        if (projectSetting == null) {
            projectSetting = ProjectSetting.builder()
                    .projectId(projectId)
                    .code(code)
                    .value(value)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .updatedAt(new Timestamp(System.currentTimeMillis()))
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
            projectSettingsDao.insert(projectSetting);
        } else {
            beforeValue = projectSetting.getValue();
            projectSetting.setValue(value);
            projectSettingsDao.update(projectSetting);
        }
        CacheManager.evict(CACHE_REGION, "getAllFunction:" + projectId);

        Project project = Optional.ofNullable(projectDao.getProjectById(projectId))
                .orElseThrow(ProjectNotFoundException::new);
        Integer operatorId = Optional.ofNullable(SystemContextHolder.get()).map(UserProto.User::getId).orElse(0);
        sendProjectSettingChangeEvent(project.getTeamOwnerId(), project.getId(), operatorId, code, value, beforeValue);
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

    public List<ProjectSetting> findProjectSettings(Integer projectId, List<String> functions) {
        if (Objects.isNull(projectId) || CollectionUtils.isEmpty(functions)) {
            return Collections.emptyList();
        }
        return projectSettingsDao.findProjectSettings(projectId, functions, BeanUtils.getDefaultDeletedAt());
    }

    public List<ProjectSetting> findProjectsSetting(List<Integer> projectIds, String function) {
        if (StringUtils.isEmpty(function) || CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyList();
        }
        return projectSettingsDao.findProjectsSetting(projectIds, function, BeanUtils.getDefaultDeletedAt());
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

    public ProjectSetting get(Integer id) {
        return projectSettingsDao.get(id, BeanUtils.getDefaultDeletedAt());
    }

    public String getCodeDefaultValue(String code) {
        ProjectSetting.Code projectSetting = ProjectSetting.Code.getByCode(code);
        return projectSetting == null ? null : projectSetting.getDefaultValue();
    }

    public boolean isDisabledSystemMenu(String code) {
        return Optional.ofNullable(ProjectSetting.Code.getByCode(code))
                .map(c -> systemSettingGrpcClient.get(
                        SystemSettingProto
                                .SystemSettingRequest
                                .newBuilder()
                                .setCode(getSystemMenuCode(c.getMenuCode()))
                                .build()
                ))
                .map(SystemSettingProto.SystemSettingResponse::getSetting)
                .map(SystemSettingProto.SystemSetting::getValue)
                .map(BooleanUtils::toBooleanObject)
                .filter(Boolean.FALSE::equals)
                .isPresent();
    }

    private String getSystemMenuCode(String code) {
        return "platform_feature_menu_" + code + "_enabled";
    }

    public void sendProjectSettingChangeEvent(Integer teamId, Integer projectId, Integer operatorId,
                                              String code, String value, String beforeValue) {
        asyncEventBus.post(
                ProjectSettingChangeEvent
                        .builder()
                        .teamId(teamId)
                        .projectId(projectId)
                        .operatorId(operatorId)
                        .code(code)
                        .afterValue(value)
                        .beforeValue(beforeValue)
                        .build()
        );
    }
}
