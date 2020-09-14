package net.coding.lib.project.service;

import net.coding.lib.project.dao.ResourceReferenceDao;
import net.coding.lib.project.entity.ResourceReference;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

@Service
public class ResourceReferenceService {

    @Resource
    private ResourceReferenceDao resourceReferenceDao;

    public List<ResourceReference> findList(Map<String, Object> parameters) {
        return resourceReferenceDao.findList(parameters);
    }

    public int batchDelete(Map<String, Object> parameters) {
        return resourceReferenceDao.batchDelete(parameters);
    }
}
