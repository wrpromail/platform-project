package net.coding.lib.project.dao;

import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectPin;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectPinDao extends tk.mybatis.mapper.common.Mapper<ProjectPin> {
    List<Project> getProjectPinPages(ProjectPageQueryParameter parameter);

    Integer findMaxSort(@Param("userId") Integer userId);

    Integer batchUpdateSortBlock(@Param("userId") Integer userId,
                                 @Param("sourceSort") Integer sourceSort,
                                 @Param("targetSort") Integer targetSort);
}
