package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.parameter.ProjectUpdateParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.omg.CORBA.INTERNAL;

import java.util.List;

@Mapper
public interface ProjectDao {

    Project getProjectById(@Param("id") Integer id);

    Project getProjectByNameAndTeamId(@Param("name") String name,
                                      @Param("teamOwnerId") Integer teamOwnerId);

    Project getProjectByIdAndTeamId(@Param("id") Integer id,
                                    @Param("teamOwnerId") Integer teamOwnerId);

    List<Project> findByProjects(ProjectQueryParameter parameter);

    int delete(@Param("id") Integer id);

    Integer updateBasicInfo(ProjectUpdateParameter project);

    Integer updateIcon(@Param("id") Integer id, @Param("icon") String icon);
}