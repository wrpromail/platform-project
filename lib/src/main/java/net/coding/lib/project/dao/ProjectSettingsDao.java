package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectSetting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * @Author liuying
 * @Date 2021/1/5 1:56 下午
 * @Version 1.0
 */
@Mapper
public interface ProjectSettingsDao {

    ProjectSetting findProjectSetting(ProjectSetting projectSetting);

    int insert(ProjectSetting projectSetting);

    int update(ProjectSetting projectSetting);

    List<ProjectSetting> findProjectSettings(
            @Param("projectId") Integer projectId,
            @Param("list") List<String> functions,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<ProjectSetting> findProjectsSetting(
            @Param("list") List<Integer> projectIds,
            @Param("code") String function,
            @Param("deletedAt") Timestamp deletedAt
    );

    ProjectSetting get(
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp deletedAt
    );
}
