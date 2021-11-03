package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ResourceSequence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResourceSequenceDao {

    int insert(ResourceSequence record);

    ResourceSequence getByScopeIdAndScopeType(@Param("scopeId") Integer scopeId, @Param("scopeType") Integer scopeType);

    int generateResourceCode(@Param("scopeId") Integer scopeId, @Param("scopeType") Integer scopeType);

}
