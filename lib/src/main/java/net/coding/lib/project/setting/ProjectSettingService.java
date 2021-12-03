package net.coding.lib.project.setting;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.platform.SystemSettingGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.exception.ProjectNotFoundException;
import net.coding.lib.project.exception.ProjectSettingInvalidCodeException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.system.setting.SystemSettingProto;
import proto.platform.user.UserProto;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSettingService {

    private final ProjectSettingsDao projectSettingsDao;
    private final ProjectSettingDefaultReader projectSettingDefaultReader;
    private final ProjectDao projectDao;
    private final AsyncEventBus asyncEventBus;
    private final SystemSettingGrpcClient systemSettingGrpcClient;

    /**
     * 更新模版开关
     */
    public ProjectSetting update(Integer projectId, String code, String value) {
        Integer operatorId = Optional.ofNullable(SystemContextHolder.get()).map(UserProto.User::getId).orElse(0);
        Project project = Optional.ofNullable(projectDao.getProjectById(projectId))
                .orElseThrow(ProjectNotFoundException::new);
        Optional<ProjectSettingDefault> settingOptional = StreamEx
                .of(projectSettingDefaultReader.read())
                .filterBy(ProjectSettingDefault::getCode, code)
                .findFirst();
        if (!settingOptional.isPresent()) {
            log.warn("Project setting code {} is invalid, please add it into default project setting on project-setting-default.json", code);
            throw new ProjectSettingInvalidCodeException();
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        ProjectSettingDefault settingDefault = settingOptional.get();
        ProjectSetting projectSetting = projectSettingsDao.findProjectSetting(projectId, code);
        if (Objects.isNull(projectSetting)) {
            projectSetting = ProjectSetting.builder()
                    .id(0)
                    .projectId(projectId)
                    .code(settingDefault.getCode())
                    .description(settingDefault.getDescription())
                    .value(settingDefault.getDefaultValue())
                    .createdAt(now)
                    .updatedAt(now)
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
        }
        // 默认值无需插入数据
        if (StringUtils.equals(projectSetting.getValue(), value)) {
            return projectSetting;
        }
        String beforeValue = projectSetting.getValue();
        boolean saved;
        projectSetting.setValue(value);
        projectSetting.setUpdatedAt(now);
        if (Objects.nonNull(projectSetting.getId()) && projectSetting.getId() > 0) {
            saved = projectSettingsDao.update(projectSetting) > 0;
        } else {
            saved = projectSettingsDao.insert(projectSetting) > 0;
        }
        if (saved) {
            sendChangeEvent(project.getTeamOwnerId(), project.getId(), operatorId, code, value, beforeValue);
        }
        return projectSetting;
    }

    /**
     * 查询项目设置值是否配置，如未配置，将使用默认值填充
     */
    public List<ProjectSetting> findProjectSettings(Integer projectId, List<String> functions) {
        if (Objects.isNull(projectId) || CollectionUtils.isEmpty(functions)) {
            return Collections.emptyList();
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Map<String, ProjectSetting> mapping = StreamEx.of(
                projectSettingsDao.findProjectSettings(projectId, functions, BeanUtils.getDefaultDeletedAt())
        ).toMap(ProjectSetting::getCode, Function.identity(), (a, b) -> a);

        List<ProjectSetting> settings = StreamEx.of(projectSettingDefaultReader.read())
                .filter(define -> functions.contains(define.getCode()))
                .map(define -> mapping.getOrDefault(define.getCode(),
                        ProjectSetting
                                .builder()
                                .projectId(projectId)
                                .code(define.getCode())
                                .value(define.getDefaultValue())
                                .createdAt(now)
                                .updatedAt(now)
                                .description(define.getDescription())
                                .build()
                )).collect(Collectors.toList());
        StreamEx.of(settings)
                .peek(s -> {
                    if (isMenuDisabled(s.getCode())) {
                        s.setValue(String.valueOf(BooleanUtils.toInteger(false)));
                    }
                });
        return settings;
    }

    /**
     * 查询项目设置值是否配置，如未配置，将使用默认值填充
     */
    public List<ProjectSetting> findProjectsSetting(List<Integer> projectIds, String function) {
        if (StringUtils.isEmpty(function) || CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyList();
        }
        Optional<ProjectSettingDefault> defineOptional = projectSettingDefaultReader.read(function);
        if (!defineOptional.isPresent()) {
            return Collections.emptyList();
        }

        // 过滤掉不可见的项目
        List<Integer> visibleProject = StreamEx.of(projectDao.getByIds(projectIds, BeanUtils.getDefaultDeletedAt()))
                .filterBy(Project::getInvisible, false)
                .map(Project::getId)
                .collect(Collectors.toList());

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        Map<Integer, ProjectSetting> mapping = StreamEx.of(
                projectSettingsDao.findProjectsSetting(visibleProject, function, BeanUtils.getDefaultDeletedAt())
        ).toMap(ProjectSetting::getProjectId, Function.identity(), (a, b) -> a);

        List<ProjectSetting> settings = defineOptional
                .map(
                        define -> StreamEx
                                .of(visibleProject)
                                .map(projectId -> mapping.getOrDefault(projectId,
                                        ProjectSetting
                                                .builder()
                                                .projectId(projectId)
                                                .code(define.getCode())
                                                .value(define.getDefaultValue())
                                                .createdAt(now)
                                                .updatedAt(now)
                                                .description(define.getDescription())
                                                .build()
                                ))
                                .collect(Collectors.toList())
                )
                .orElse(Collections.emptyList());
        StreamEx.of(settings)
                .peek(s -> {
                    if (isMenuDisabled(s.getCode())) {
                        s.setValue(String.valueOf(BooleanUtils.toInteger(false)));
                    }
                });
        return settings;
    }

    public ProjectSetting get(Integer id) {
        return projectSettingsDao.get(id, BeanUtils.getDefaultDeletedAt());
    }

    /**
     * 查询项目设置值是否配置，如未配置，将使用默认值填充
     */
    public ProjectSetting findByCode(final Integer projectId, final String code) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        ProjectSetting setting = Optional.ofNullable(projectSettingsDao.findProjectSetting(projectId, code))
                .orElseGet(
                        () -> projectSettingDefaultReader
                                .read(code)
                                .map(define -> ProjectSetting
                                        .builder()
                                        .projectId(projectId)
                                        .code(code)
                                        .value(define.getDefaultValue())
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .description(define.getDescription())
                                        .build()
                                )
                                .orElse(null)
                );
        Optional.ofNullable(setting)
                .ifPresent(s -> {
                    if (isMenuDisabled(code)) {
                        s.setValue(String.valueOf(BooleanUtils.toInteger(false)));
                    }
                });
        return setting;
    }

    public boolean isMenuDisabled(String code) {
        return projectSettingDefaultReader.read(code)
                .filter(d -> Objects.nonNull(d.getFeatureCode()))
                .map(d -> systemSettingGrpcClient.get(
                        SystemSettingProto
                                .SystemSettingRequest
                                .newBuilder()
                                .setCode(d.getFeatureCode())
                                .build()
                )).map(SystemSettingProto.SystemSettingResponse::getSetting)
                .map(SystemSettingProto.SystemSetting::getValue)
                .map(BooleanUtils::toBooleanObject)
                .filter(Boolean.FALSE::equals)
                .isPresent();
    }

    public void sendChangeEvent(
            Integer teamId,
            Integer projectId,
            Integer operatorId,
            String code,
            String value,
            String beforeValue
    ) {
        log.info("Send project {} setting code {} change event before value is {} current value is {}", projectId, code, beforeValue, value);
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
