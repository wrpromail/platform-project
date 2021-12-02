package net.coding.lib.project.service;

import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.UserProjectSettingDao;
import net.coding.lib.project.dto.UserProjectSettingValueDTO;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.UserProjectSetting;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.setting.user.UserProjectSettingDefaultReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Service
@Slf4j
@AllArgsConstructor
public class UserProjectSettingService {
    private final UserProjectSettingDao userProjectSettingDao;
    private final UserProjectSettingDefaultReader reader;
    private final LocaleMessageSource localeMessageSource;
    private final ProjectMemberService projectMemberService;

    public List<UserProjectSettingValueDTO> getValuesByCodes(List<String> codes,
                                                             Integer projectId,
                                                             Integer userId
    ) throws CoreException {
        List<UserProjectSettingValueDTO> resultList = new ArrayList<>();
        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(projectId, userId);
        if (projectMember == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        codes.removeIf(code -> !reader.read(code).isPresent());
        if (CollectionUtils.isNotEmpty(codes)) {
            List<UserProjectSetting> savedUserProjectSettings =
                    userProjectSettingDao.getUserProjectSettingsByCodes(
                            codes, projectId, userId, BeanUtils.getDefaultDeletedAt()
                    );

            Map<String, UserProjectSettingValueDTO> mapping = StreamEx.of(savedUserProjectSettings)
                    .map(s -> builderDTO(s.getCode(), s.getValue()))
                    .toMap(UserProjectSettingValueDTO::getCode, Function.identity(), (a, b) -> a);

            resultList = StreamEx.of(reader.read())
                    .filter(define -> codes.contains(define.getCode()))
                    .map(define -> mapping.getOrDefault(define.getCode(),
                            builderDTO(define.getCode(), define.getDefaultValue())
                    )).collect(Collectors.toList());

        }
        return resultList;
    }

    public UserProjectSettingValueDTO updateUserProjectSettingValue(String code,
                                                                    String value,
                                                                    Integer projectId,
                                                                    Integer userId
    ) throws CoreException {
        if (!reader.read(code).isPresent()) {
            throw CoreException.of(CoreException.ExceptionType.USER_PROJECT_SETTING_CODE_NOT_EXISTS);
        }
        ProjectMember projectMember = projectMemberService.getByProjectIdAndUserId(projectId, userId);
        if (projectMember == null) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        // 查询用户配置
        UserProjectSetting userProjectSetting =
                userProjectSettingDao.getUserProjectSetting(
                        code, projectId, userId, BeanUtils.getDefaultDeletedAt());

        if (userProjectSetting != null) {
            userProjectSetting.setValue(value);
            userProjectSettingDao.updateValue(userProjectSetting.getId(), value);
        } else {
            userProjectSetting = UserProjectSetting.builder()
                    .projectId(projectId)
                    .code(code)
                    .userId(userId)
                    .value(value)
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .description(
                            StringUtils.defaultString(
                                    localeMessageSource.getMessage(reader.read(code).get().getDescription()), ""
                            )
                    )
                    .build();
            userProjectSettingDao.insert(userProjectSetting);
        }
        return builderDTO(userProjectSetting.getCode(), userProjectSetting.getValue());
    }

    public UserProjectSettingValueDTO builderDTO(String code, String value) {
        return UserProjectSettingValueDTO.builder()
                .code(code)
                .value(value)
                .message(reader.read(code).isPresent() ? localeMessageSource.getMessage(code) : "")
                .build();
    }
}
