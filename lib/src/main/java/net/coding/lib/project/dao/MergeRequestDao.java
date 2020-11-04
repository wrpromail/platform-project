package net.coding.lib.project.dao;

import net.coding.lib.project.entity.MergeRequest;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MergeRequestDao {
    MergeRequest getById(@Param("id") Integer id);
}
