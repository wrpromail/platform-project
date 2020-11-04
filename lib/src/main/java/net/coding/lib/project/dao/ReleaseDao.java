package net.coding.lib.project.dao;

import net.coding.lib.project.entity.Release;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseDao {

    Release getById(@Param("id") Integer id);
}
