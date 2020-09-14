package net.coding.lib.project.dao;


import net.coding.lib.project.entity.ResourceReference;

import java.util.List;
import java.util.Map;

public interface ResourceReferenceDao {

    List<ResourceReference> findList(Map<String, Object> parameter);

    int batchDelete(Map<String, Object> parameter);
}