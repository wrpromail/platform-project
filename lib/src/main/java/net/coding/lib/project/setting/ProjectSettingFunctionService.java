package net.coding.lib.project.setting;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ProjectSettingInvalidCodeException;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSettingFunctionService {
    private final static String SCOPE = "function";

    private final ProjectSettingService projectSettingService;
    private final ProjectSettingDefaultReader projectSettingDefaultReader;
    private final AsyncEventBus asyncEventBus;

    /**
     * 获取项目功能的所有设置，无记录使用默认值。
     */
    public List<ProjectSettingDefault> getFunctions() {
        return StreamEx.of(projectSettingDefaultReader.read())
                .filterBy(ProjectSettingDefault::getScope, SCOPE)
                .collect(Collectors.toList());
    }

    /**
     * 获取项目功能的所有设置，无记录使用默认值。
     */
    public List<ProjectSettingDTO> getFunctions(Integer projectId) {
        return StreamEx.of(projectSettingDefaultReader.read())
                .filterBy(ProjectSettingDefault::getScope, SCOPE)
                .map(function -> {
                    ProjectSetting setting = projectSettingService.findByCode(projectId, function.getCode());
                    return ProjectSettingDTO.builder()
                            .code(function.getCode())
                            .name(function.getName())
                            .description(function.getDescription())
                            .projectId(projectId)
                            .value(setting.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 修改功能状态
     */
    public boolean update(
            Integer projectId,
            String code,
            boolean value
    ) throws CoreException {
        // 当前登录用户
        if (Objects.isNull(SystemContextHolder.get())) {
            return false;
        }
        // 匹配 项目功能
        boolean validateCode = StreamEx.of(projectSettingDefaultReader.read())
                .filterBy(ProjectSettingDefault::getScope, SCOPE)
                .filterBy(ProjectSettingDefault::getCode, code)
                .findFirst()
                .isPresent();
        if (!validateCode) {
            log.warn("Project setting code {} is invalid function code, please validate it scope is {}", code, SCOPE);
            throw new ProjectSettingInvalidCodeException();
        }
        Integer userId = SystemContextHolder.get().getId();
        ProjectSetting setting = projectSettingService.update(
                projectId,
                code,
                String.valueOf(BooleanUtils.toInteger(value))
        );
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.e.lib.core.bean.ProjectSetting.class)
                        .targetId(setting.getId())
                        .projectId(setting.getProjectId())
                        .action((short) BooleanUtils.toInteger(value))
                        .content(setting.getCode())
                        .build()
        );
        return true;
    }
}
