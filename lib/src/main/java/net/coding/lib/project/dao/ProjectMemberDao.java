package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectMember;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMemberDao {

    int insert(ProjectMember record);

    int update(ProjectMember record);

    ProjectMember getById(@Param("id") Integer id);

    List<ProjectMember> findList(Map<String, Object> parameter);

    List<ProjectMember> findListByProjectId(Map<String, Object> parameter);

    ProjectMember getByProjectIdAndUserId(Map<String, Object> parameter);
}
