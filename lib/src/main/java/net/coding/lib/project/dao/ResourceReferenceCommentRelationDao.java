package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ResourceReferenceCommentRelation;

import java.util.Map;

public interface ResourceReferenceCommentRelationDao {

    int insert(ResourceReferenceCommentRelation record);

    int countComment(Map<String, Object> parameter);
}
