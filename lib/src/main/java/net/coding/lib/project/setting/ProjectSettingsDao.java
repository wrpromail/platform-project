package net.coding.lib.project.setting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectSettingsDao {

    ProjectSetting findProjectSetting(@Param("projectId") Integer projectId, @Param("code") String code);

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
