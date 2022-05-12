package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectPersonalPreferenceDao;
import net.coding.lib.project.entity.ProjectPersonalPreference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectPersonalPreferenceService {
    private final ProjectPersonalPreferenceDao projectPersonalPreferenceDao;

    public ProjectPersonalPreference findBy(Integer projectId, Integer userId, String key) {
        if (projectId == null || userId == null || StringUtils.isBlank(key)) {
            throw new IllegalArgumentException();
        }
        return projectPersonalPreferenceDao.findByProjectUserKey(projectId, userId, key, BeanUtils.NOT_DELETED_AT);
    }

    public List<ProjectPersonalPreference> put(
            Integer projectId,
            Integer userId,
            Map<String, String> puttingPreferences
    ) {
        return puttingPreferences.entrySet()
                .stream()
                .map(stringStringEntry -> {
                            ProjectPersonalPreference preference = projectPersonalPreferenceDao
                                    .findByProjectUserKey(
                                            projectId,
                                            userId,
                                            stringStringEntry.getKey(),
                                            null);
                            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
                            if (Objects.nonNull(preference)) {
                                preference.setValue(stringStringEntry.getValue());
                                preference.setUpdatedAt(timestamp);
                                projectPersonalPreferenceDao.update(preference);
                                return preference;
                            }
                            preference = new ProjectPersonalPreference();
                            preference.setProjectId(projectId);
                            preference.setUserId(userId);
                            preference.setKey(stringStringEntry.getKey());
                            preference.setValue(stringStringEntry.getValue());
                            preference.setCreatedAt(timestamp);
                            preference.setUpdatedAt(timestamp);
                            preference.setId(projectPersonalPreferenceDao.insert(preference));
                            return preference;
                        }
                ).collect(Collectors.toList());
    }
}
