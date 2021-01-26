package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;
import net.coding.lib.project.parameter.ProjectQueryParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectDao {

    Project getProject(Project project);

    Integer update(Project project);

    List<Project> findByProjects(ProjectQueryParameter parameter);

    int delete(@Param("id") Integer id);
}