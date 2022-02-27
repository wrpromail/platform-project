package net.coding.lib.project.dao;

import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectRecentView;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface ProjectRecentViewDao extends tk.mybatis.mapper.common.Mapper<ProjectRecentView> {

    List<Project> getProjectRecentViews(
            @Param("userId") Integer userId,
            @Param("joinedProjectIds") Set<Integer> joinedProjectIds);
}
