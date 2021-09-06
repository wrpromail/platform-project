package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectPersonalPreference;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface ProjectPersonalPreferenceDao {
    ProjectPersonalPreference findByProjectUserKey(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer userId,
            @Param("key") String key,
            @Param("deletedAt") String deletedAt
    );

    int update(@Param("preference") ProjectPersonalPreference preference);

    int insert(@Param("preference") ProjectPersonalPreference preference);
}
