package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface ProjectDao {

    Project getById(@Param("id") Integer id);

    Project getByNameAndTeamId(Map<String, Object> parameter);
}