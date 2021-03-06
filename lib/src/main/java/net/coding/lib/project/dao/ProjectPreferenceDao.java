package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectPreference;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectPreferenceDao {

    int insert(ProjectPreference record);

    int update(ProjectPreference record);

    ProjectPreference getById(@Param("id") Integer id);

    List<ProjectPreference> getByProjectId(@Param("projectId") Integer projectId);

    List<ProjectPreference> findList(Map<String, Object> parameter);

    ProjectPreference getByProjectIdAndType(Map<String, Object> parameter);

    int insertIntegration(@Param("list") List<ProjectPreference> projectPreferences);

    int updateStatus(@Param("projectId") Integer projectId, @Param("type") Integer type, @Param("status") Integer status);
}
