package net.coding.lib.project.dao;


import net.coding.lib.project.entity.ResourceReference;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResourceReferenceDao {

    List<ResourceReference> findList(Map<String, Object> parameter);

    int batchDelete(Map<String, Object> parameter);

    List<ResourceReference> getResourceReferenceMutually(Map<String, Object> parameter);

    ResourceReference getResourceReference(Map<String, Object> parameter);
}