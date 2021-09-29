package net.coding.lib.project.dao;


import net.coding.lib.project.entity.ResourceReference;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResourceReferenceDao {

    List<ResourceReference> findList(Map<String, Object> parameter);

    int batchDelete(Map<String, Object> parameter);

    List<ResourceReference> getResourceReferenceMutually(Map<String, Object> parameter);

    ResourceReference getResourceReference(Map<String, Object> parameter);

    int insert(ResourceReference resourceReference);

    int batchInsert(List<ResourceReference> list);

    int update(ResourceReference resourceReference);

    int deleteById(Map<String, Object> parameter);

    int deleteByIds(Map<String, Object> parameter);

    int countByTarget(Map<String, Object> parameter);

    int countBySelfWithTargetDeleted(Map<String, Object> parameter);

    List<ResourceReference> findListByTargetType(Map<String, Object> parameter);

    List<ResourceReference> findListByTargetProjectId(Map<String, Object> parameter);

    List<ResourceReference> findListBySelfType(Map<String, Object> parameter);

    List<ResourceReference> findListBySelfProjectId(Map<String, Object> parameter);

    List<ResourceReference> findListBySelfAndTarget(Map<String, Object> parameter);

    List<ResourceReference> findReferMutuallyList(Map<String, Object> parameter);

    List<ResourceReference> findMutuallyList(Map<String, Object> parameter);

    List<Integer> findIdsMutually(Map<String, Object> parameter);

    List<ResourceReference> findBySelfWithDescriptionCitedRelation(Map<String, Object> parameter);

    List<ResourceReference> findBySelfWithoutDescriptionCitedRelation(Map<String, Object> parameter);

    List<ResourceReference> findBySelfWithTargetDeleted(Map<String, Object> parameter);

    List<ResourceReference> findByProjectId(Map<String, Object> parameter);

    ResourceReference getByProjectIdAndCode(Map<String, Object> parameter);

    ResourceReference getByTypeAndId(Map<String, Object> parameter);

    ResourceReference getOptional(Map<String, Object> parameter);

    ResourceReference getById(Map<String, Object> parameter);

    List<ResourceReference> getWithDeletedByIds(Map<String, Object> parameter);

    ResourceReference existsResourceReference(Map<String, Object> parameter);

    void delete(Map<String, Object> map);
}