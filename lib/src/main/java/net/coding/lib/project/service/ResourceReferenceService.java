package net.coding.lib.project.service;

import net.coding.e.proto.wiki.WikiProto;
import net.coding.lib.project.dao.ResourceReferenceDao;
import net.coding.lib.project.entity.ResourceReference;
import net.coding.lib.project.enums.ResourceTypeEnum;
import net.coding.lib.project.grpc.client.WikiGrpcClient;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResourceReferenceService {

    @Resource
    private ResourceReferenceDao resourceReferenceDao;

    @Autowired
    private WikiGrpcClient wikiGrpcClient;

    public List<ResourceReference> findList(Map<String, Object> parameters) {
        return resourceReferenceDao.findList(parameters);
    }

    public int batchDelete(Map<String, Object> parameters) {
        return resourceReferenceDao.batchDelete(parameters);
    }

    public List<ResourceReference> getResourceReferenceMutually(Map<String, Object> parameters) {
        return resourceReferenceDao.getResourceReferenceMutually(parameters);
    }

    public ResourceReference insert(ResourceReference resourceReference) {
        resourceReferenceDao.insert(resourceReference);
        return resourceReference;
    }

    public int batchInsert(List<ResourceReference> resourceReferenceList) {
        return resourceReferenceDao.batchInsert(resourceReferenceList);
    }

    public int update(ResourceReference resourceReference) {
        return resourceReferenceDao.update(resourceReference);
    }

    public int deleteById(Integer id) {
        if(id <= 0) {
            return 0;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("id", id);
        return resourceReferenceDao.deleteById(parameters);
    }

    public int deleteByIds(List<Integer> ids) {
        if(CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("ids", ids);
        return resourceReferenceDao.deleteByIds(parameters);
    }

    public int deleteSelfByTypeAndId(String selfType, Integer selfId) {
        List<ResourceReference> resourceReferenceList = findListBySelfType(selfType, selfId);
        List<Integer> ids = resourceReferenceList
                .stream()
                .map(ResourceReference::getId)
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("ids", ids);
        return resourceReferenceDao.deleteByIds(parameters);
    }

    public int deleteTargetByTypeAndId(String targetType, Integer targetId) {
        List<ResourceReference> resourceReferenceList = findListByTargetType(targetType, targetId);
        List<Integer> ids = resourceReferenceList
                .stream()
                .map(ResourceReference::getId)
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("ids", ids);
        return resourceReferenceDao.deleteByIds(parameters);
    }

    public int deleteByTypeAndId(String type, Integer id) {
        int selfDelete = deleteSelfByTypeAndId(type, id);
        int targetDelete = deleteTargetByTypeAndId(type, id);
        if(targetDelete > 0 && selfDelete > 0) {
            return 1;
        }
        return 0;
    }

    public int deleteByProjectId(Integer projectId) {
        List<ResourceReference> resourceReferenceList = findByProjectId(projectId, false);
        List<Integer> ids = resourceReferenceList
                .stream()
                .map(ResourceReference::getId)
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deletedAt", DateUtil.getCurrentDate());
        parameters.put("ids", ids);
        return resourceReferenceDao.deleteByIds(parameters);
    }

    public int countByTarget(Integer targetProjectId, Integer targetIid) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetProjectId", targetProjectId);
        parameters.put("targetIid", targetIid);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.countByTarget(parameters);
    }

    public int countBySelfWithTargetDeleted(Integer projectId, Integer code) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", code);
        return resourceReferenceDao.countBySelfWithTargetDeleted(parameters);
    }

    public List<ResourceReference> findListByTargetType(String targetType, Integer targetId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetType", targetType);
        parameters.put("targetId", targetId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.findListByTargetType(parameters);
    }

    public List<ResourceReference> findListByTargetProjectId(Integer targetProjectId, Integer targetIid, Integer userId, boolean isFilter) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetProjectId", targetProjectId);
        parameters.put("targetIid", targetIid);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        if(isFilter) {
            List<ResourceReference> resourceReferences = resourceReferenceDao.findListByTargetProjectId(parameters)
                    .stream()
                    .filter(record -> {
                        if(ResourceTypeEnum.Wiki.getType().equals(record.getTargetType())) {
                            WikiProto.GetWikiByProjectIdAndIidData wiki = wikiGrpcClient.getWikiByProjectIdAndIidWithoutRecycleBin(record.getTargetProjectId(), record.getTargetIid());
                            if(wiki == null) {
                                return false;
                            }
                            return wikiGrpcClient.wikiCanRead(userId, wiki.getProjectId(), wiki.getIid());
                        } else {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
            return resourceReferences;
        } else {
            return resourceReferenceDao.findListByTargetProjectId(parameters);
        }
    }

    public List<ResourceReference> findListBySelfType(String selfType, Integer selfId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfType", selfType);
        parameters.put("selfId", selfId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.findListBySelfType(parameters);
    }

    public List<ResourceReference> findListBySelfProjectId(Integer selfProjectId, Integer selfIid) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfIid", selfIid);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.findListBySelfProjectId(parameters);
    }

    public List<ResourceReference> findListBySelfAndTarget(Integer projectId, Integer selfAndTargetId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("selfAndTargetId", selfAndTargetId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.findListBySelfAndTarget(parameters);
    }

    public List<ResourceReference> findReferMutuallyList(Integer selfProjectId, Integer selfIid, Integer userId, boolean isFilter) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfIid", selfIid);
        if(isFilter) {
            List<ResourceReference> resourceReferences = resourceReferenceDao.findReferMutuallyList(parameters)
                    .stream()
                    .filter(record -> {
                        if(ResourceTypeEnum.Wiki.getType().equals(record.getTargetType())) {
                            WikiProto.GetWikiByProjectIdAndIidData wiki = wikiGrpcClient.getWikiByProjectIdAndIidWithoutRecycleBin(record.getTargetProjectId(), record.getTargetIid());
                            if(wiki == null) {
                                return false;
                            }
                            return wikiGrpcClient.wikiCanRead(userId, wiki.getProjectId(), wiki.getIid());
                        } else {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
            return resourceReferences;
        } else {
            return resourceReferenceDao.findReferMutuallyList(parameters);
        }
    }

    public List<ResourceReference> findMutuallyList(Integer selfProjectId, Integer selfCode, Integer targetProjectId, Integer targetCode) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfCode", selfCode);
        parameters.put("targetProjectId", targetProjectId);
        parameters.put("targetCode", targetCode);
        return resourceReferenceDao.findMutuallyList(parameters);
    }

    public List<Integer> findIdsMutually(Integer projectId, Integer code) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", code);
        return resourceReferenceDao.findIdsMutually(parameters);
    }

    public List<ResourceReference> findBySelfWithDescriptionCitedRelation(String selfType, Integer selfId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfType", selfType);
        parameters.put("selfId", selfId);
        return resourceReferenceDao.findBySelfWithDescriptionCitedRelation(parameters);
    }

    public List<ResourceReference> findBySelfWithoutDescriptionCitedRelation(String selfType, Integer selfId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfType", selfType);
        parameters.put("selfId", selfId);
        return resourceReferenceDao.findBySelfWithoutDescriptionCitedRelation(parameters);
    }

    public List<ResourceReference> findBySelfWithTargetDeleted(Integer projectId, Integer code, Integer userId, boolean isFilter) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", code);
        if(isFilter) {
            List<ResourceReference> resourceReferences = resourceReferenceDao.findBySelfWithTargetDeleted(parameters)
                    .stream()
                    .filter(record -> {
                        if(ResourceTypeEnum.Wiki.getType().equals(record.getTargetType())) {
                            WikiProto.GetWikiByProjectIdAndIidData wiki = wikiGrpcClient.getWikiByProjectIdAndIidWithoutRecycleBin(record.getTargetProjectId(), record.getTargetIid());
                            if(wiki == null) {
                                return false;
                            }
                            return wikiGrpcClient.wikiCanRead(userId, wiki.getProjectId(), wiki.getIid());
                        } else {
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
            log.info("findBySelfWithTargetDeleted resourceReferences={}", resourceReferences.toString());
            return resourceReferences;
        } else {
            return resourceReferenceDao.findBySelfWithTargetDeleted(parameters);
        }
    }

    public List<ResourceReference> findByProjectId(Integer projectId, boolean withDeleted) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        if(false == withDeleted) {
            parameters.put("deletedAt", "1970-01-01 00:00:00");
        }
        return resourceReferenceDao.findByProjectId(parameters);
    }

    public ResourceReference getByProjectIdAndCode(Integer selfProjectId, Integer selfCode, Integer targetProjectId, Integer targetCode) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfIid", selfCode);
        parameters.put("targetProjectId", targetProjectId);
        parameters.put("targetIid", targetCode);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.getByProjectIdAndCode(parameters);
    }

    public ResourceReference getByTypeAndId(String selfType, Integer selfId, String targetType, Integer targetId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfType", selfType);
        parameters.put("selfId", selfId);
        parameters.put("targetType", targetType);
        parameters.put("targetId", targetId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.getByTypeAndId(parameters);
    }

    public ResourceReference getOptional(Integer selfProjectId, String selfType, Integer selfId, String targetType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfType", selfType);
        parameters.put("selfId", selfId);
        parameters.put("targetType", targetType);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return resourceReferenceDao.getOptional(parameters);
    }

    public ResourceReference getById(Integer id, boolean withDeleted) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        if(false == withDeleted) {
            parameters.put("deletedAt", "1970-01-01 00:00:00");
        }
        return resourceReferenceDao.getById(parameters);
    }

    public List<ResourceReference> getWithDeletedByIds(List<Integer> ids) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ids", ids);
        return resourceReferenceDao.getWithDeletedByIds(parameters);
    }

    public boolean existsResourceReference(Integer targetProjectId, Integer targetIid, Integer selfProjectId, Integer selfIid) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetProjectId", targetProjectId);
        parameters.put("targetIid", targetIid);
        parameters.put("selfProjectId", selfProjectId);
        parameters.put("selfIid", selfIid);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        ResourceReference resourceReference = resourceReferenceDao.existsResourceReference(parameters);
        if(Objects.nonNull(resourceReference)) {
            return true;
        }
        return false;
    }
}
