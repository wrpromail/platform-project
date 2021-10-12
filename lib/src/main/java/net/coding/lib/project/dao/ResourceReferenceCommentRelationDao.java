package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ResourceReferenceCommentRelation;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResourceReferenceCommentRelationDao {

    int insert(ResourceReferenceCommentRelation record);

    int countComment(Map<String, Object> parameter);

    int update(ResourceReferenceCommentRelation record);

    int batchInsert(List<ResourceReferenceCommentRelation> list);

    int deleteByReferenceIds(Map<String, Object> parameter);

    int deleteByReferenceIdsAndCitedSource(Map<String, Object> parameter);

    int deleteByCommentIdAndReferenceIds(Map<String, Object> parameter);

    List<ResourceReferenceCommentRelation> findByResourceReferenceId(Map<String, Object> parameter);

    List<Integer> findReferenceRelationsAbove(Map<String, Object> parameter);

    List<Integer> findReferenceRelationsBelowEqual(Map<String, Object> parameter);

    List<Integer> findUsedReferenceIdsWithoutDescription(Map<String, Object> parameter);

    List<ResourceReferenceCommentRelation> findByCommentIdAndCommentType(Map<String, Object> parameter);
}
