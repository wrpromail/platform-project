package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

import tk.mybatis.mapper.additional.idlist.IdListMapper;

@Mapper
public interface ProjectInvisibleDao extends tk.mybatis.mapper.common.Mapper<Project>,
        IdListMapper<Project, Integer> {

    List<Project> findJoinedProjectsByLabel(
            @Param("teamId") Integer teamId,
            @Param("joinedProjectIds") Set<Integer> joinedProjectIds,
            @Param("label") String label
    );
}