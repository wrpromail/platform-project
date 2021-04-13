package net.coding.lib.project.service;

import com.google.common.collect.ImmutableMap;

import net.coding.lib.project.dao.ProjectPreferenceDao;
import net.coding.lib.project.entity.ProjectPreference;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProjectPreferenceService {


    private final ProjectPreferenceDao projectPreferenceDao;
    /*-------------------------------- 偏好设置类型常量 --------------------------------*/
    /* 通知设置 */
    /**
     * 项目公告.
     */
    public static final Short PREFERENCE_TYPE_PROJECT_TWEET = 1;
    /**
     * 未受保护的分支.
     */
    public static final Short PREFERENCE_TYPE_UNPROTECTED_BRANCH_MERGE_REQUEST = 2;


    /*-------------------------------- 偏好设置状态常量 --------------------------------*/
    /* 通用. */
    /**
     * 关闭.
     */
    public static final short PREFERENCE_STATUS_FALSE = 0;
    /**
     * 开启.
     */
    public static final short PREFERENCE_STATUS_TRUE = 1;

    /**
     * 合并请求偏好设置的方式(PREFERENCE_TYPE_MR_SETTING type 7) 0 默认直接合并 1 默认 Squash 合并 2 只能 Squash 合并
     */
    public static final short PREFERENCE_STATUS_DEFAULT = 0;
    public static final short PREFERENCE_STATUS_SQUASH = 1;
    public static final short PREFERENCE_STATUS_ONLY_SQUASH = 2;
    /**
     * 项目偏好设置的默认设置.
     */
    public static final Map<Short, Short> DEFAULT_PREFERENCES = ImmutableMap.<Short, Short>builder()
            .put(PREFERENCE_TYPE_PROJECT_TWEET, PREFERENCE_STATUS_TRUE)
            .build();


    public ProjectPreference getById(Integer id) {
        return projectPreferenceDao.getById(id);
    }

    public int insert(ProjectPreference projectPreference) {
        return projectPreferenceDao.insert(projectPreference);
    }

    public int update(ProjectPreference projectPreference) {
        return projectPreferenceDao.update(projectPreference);
    }

    public List<ProjectPreference> findList(Map<String, Object> param) {
        return projectPreferenceDao.findList(param);
    }

    /**
     * 获取项目的偏好设置.
     *
     * @param projectId 项目编号
     * @return 项目的偏好设置列表
     */
    public List<ProjectPreference> getProjectPreferences(Integer projectId) {
        List<ProjectPreference> projectPreferences =
                projectPreferenceDao.getByProjectId(projectId);
        return CollectionUtils.isEmpty(projectPreferences)
                ? getInitPreferences(projectId)
                : projectPreferences;
    }

    /**
     * 初始化项目的默认偏好设置并返回设置列表.
     *
     * @param projectId 项目编号
     * @return 偏好设置列表
     */
    private List<ProjectPreference> getInitPreferences(Integer projectId) {
        initProjectPreferences(projectId);
        return projectPreferenceDao.getByProjectId(projectId);
    }


    /**
     * 初始化项目的偏好设置.
     *
     * @param projectId 项目编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void initProjectPreferences(Integer projectId) {
        List<ProjectPreference> preferences = DEFAULT_PREFERENCES
                .entrySet()
                .stream()
                .map((entry) -> ProjectPreference.builder().projectId(projectId)
                        .type(entry.getKey())
                        .status(entry.getValue()).build())
                .collect(Collectors.toList());
        projectPreferenceDao.insertIntegration(preferences);
    }

    public ProjectPreference getByProjectIdAndType(Integer projectId, Short type) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("type", type.intValue());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectPreferenceDao.getByProjectIdAndType(parameters);
    }

    /**
     * 切换项目偏好设置的状态.
     *
     * @param projectId 项目编号
     * @param type      偏好设置类型
     * @param status    偏好设置状态
     * @return 切换成功返回true否则返回false
     */
    public boolean toggleProjectPreference(Integer projectId, Short type, Short status) {
        return projectPreferenceDao.updateStatus(projectId, Integer.valueOf(type),
                Integer.valueOf(status)) > 0;
    }

}
