package net.coding.lib.project.dao;


import net.coding.lib.project.entity.NonResourceReference;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NonResourceReferenceDao {

    public Integer countByTarget(@Param("targetProjectId") Integer targetProjectId, @Param("targetIId") Integer targetIId);

    public Integer addNoneResourceReference(NonResourceReference nonResourceReference);
}