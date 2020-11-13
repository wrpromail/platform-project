package net.coding.lib.project.service;

import net.coding.lib.project.dao.ResourceReferenceCommentRelationDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
}
