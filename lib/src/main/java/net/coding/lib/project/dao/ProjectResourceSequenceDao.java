package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectResourceSequence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface ProjectResourceSequenceDao {

    int insert(ProjectResourceSequence record);

    ProjectResourceSequence getById(@Param("projectId") Integer projectId);

    int generateProjectResourceCode(@Param("projectId") Integer projectId);

    int generateProjectResourceCodes(Map<String, Object> parameters);

    int getCode();

    ProjectResourceSequence getByProjectId(@Param("projectId") Integer projectId);
}
