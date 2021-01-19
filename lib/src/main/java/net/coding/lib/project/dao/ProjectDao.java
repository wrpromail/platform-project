package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectDao {

    Project getProject(Project project);

    Integer update(Project project);
}