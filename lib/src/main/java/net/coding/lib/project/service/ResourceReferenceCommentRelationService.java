package net.coding.lib.project.service;

import net.coding.lib.project.dao.ResourceReferenceCommentRelationDao;
import net.coding.lib.project.entity.ResourceReferenceCommentRelation;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceReferenceCommentRelationService {

    @Autowired
    private ResourceReferenceCommentRelationDao resourceReferenceCommentRelationDao;

    public int countComment(Integer resourceReferenceId) {
        Map<String, Object> param = new HashMap<>();
        param.put("resourceReferenceId", resourceReferenceId);
        param.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.countComment(param);
    }

    public int insert(ResourceReferenceCommentRelation record) {
        return resourceReferenceCommentRelationDao.insert(record);
    }

    public int batchInsert(List<ResourceReferenceCommentRelation> list) {
        return resourceReferenceCommentRelationDao.batchInsert(list);
    }

    public int deleteByReferenceIds(List<Integer> referenceIds, boolean isDescription) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceIds", referenceIds);
        parameter.put("deletedAt", DateUtil.getCurrentDate());
        if (isDescription) {
            parameter.put("citedSource", "DESCRIPTION");
            return resourceReferenceCommentRelationDao.deleteByReferenceIdsAndCitedSource(parameter);
        }
        return resourceReferenceCommentRelationDao.deleteByReferenceIds(parameter);
    }

    public int deleteByCommentIdAndReferenceIds(Integer commentId, List<Integer> referenceIds) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceIds", referenceIds);
        parameter.put("commentId", commentId);
        parameter.put("deletedAt", DateUtil.getCurrentDate());
        return resourceReferenceCommentRelationDao.deleteByCommentIdAndReferenceIds(parameter);
    }

    public List<ResourceReferenceCommentRelation> findByResourceReferenceId(Integer referenceId) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceId", referenceId);
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.findByResourceReferenceId(parameter);
    }

    public List<Integer> findReferenceRelationsAbove(List<Integer> referenceIds, Integer number) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceIds", referenceIds);
        parameter.put("number", number);
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.findReferenceRelationsAbove(parameter);
    }

    public List<Integer> findReferenceRelationsBelowEqual(List<Integer> referenceIds, Integer number) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceIds", referenceIds);
        parameter.put("number", number);
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.findReferenceRelationsBelowEqual(parameter);
    }

    public List<Integer> findUsedReferenceIdsWithoutDescription(List<Integer> referenceIds) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("referenceIds", referenceIds);
        parameter.put("citedSource", "DESCRIPTION");
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.findUsedReferenceIdsWithoutDescription(parameter);
    }

    public List<ResourceReferenceCommentRelation> findByCommentIdAndCommentType(Integer commentId, String resourceType) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("commentId", commentId);
        parameter.put("resourceType", resourceType);
        parameter.put("citedSource", "COMMENT");
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceCommentRelationDao.findByCommentIdAndCommentType(parameter);
    }
}
