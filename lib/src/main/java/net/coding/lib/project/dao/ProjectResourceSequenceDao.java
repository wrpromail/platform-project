package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectResourceSequence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectResourceSequenceDao {

    int insert(ProjectResourceSequence record);

    ProjectResourceSequence getById(@Param("projectId") Integer projectId);

    int update(@Param("projectId") Integer projectId);

    int getCode();

    ProjectResourceSequence getByProjectId(@Param("projectId") Integer projectId);
}
