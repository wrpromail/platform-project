package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectMember;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ProjectMemberDao {

    int insert(ProjectMember record);

    int update(ProjectMember record);

    ProjectMember getById(@Param("id") Integer id);

    List<ProjectMember> findList(Map<String, Object> parameter);

    List<ProjectMember> findListByProjectId(@Param("projectId") Integer projectId);

    ProjectMember getByProjectIdAndUserId(Map<String, Object> parameter);
}
