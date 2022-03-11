package net.coding.lib.project.dao;

import net.coding.lib.project.entity.SearchEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchEntityDao {

    public Integer insertEntry(SearchEntity searchEntity);

    public Integer updateEntry(@Param("id") Integer id, @Param("titleKeywords") String title, @Param("contentKeywords") String content);

    public SearchEntity getByTargetIdAndType(@Param("targetId") Integer targetId, @Param("targetType") String targetType);

    public Integer deleteById(@Param("id") Integer id);

    public void batchUpdateTargetTypeByTargetIdAndType(@Param("newTargetType") String newTargetType, @Param("projectId") Integer projectId, @Param("targetType") String targetType, @Param("targetIdList") List<Integer> targetIdList );
}
