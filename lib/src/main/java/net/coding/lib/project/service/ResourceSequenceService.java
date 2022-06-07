package net.coding.lib.project.service;

import net.coding.lib.project.dao.ResourceSequenceDao;
import net.coding.lib.project.entity.ResourceSequence;
import net.coding.lib.project.enums.GlobalResourceTypeEnum;
import net.coding.lib.project.enums.ScopeTypeEnum;

import org.springframework.stereotype.Service;

import java.util.Objects;

import javax.annotation.Resource;

@Service
public class ResourceSequenceService {

    @Resource
    private ResourceSequenceDao resourceSequenceDao;

    /**
     * 调用此方法外部必须开启事务。
     *
     * @param scopeId
     * @param scopeType
     * @param targetType
     * @return
     */
    public String generateResourceCode(Integer scopeId, Integer scopeType, String targetType) {

        if (resourceSequenceDao.getByScopeIdAndScopeType(scopeId, scopeType) == null) {
            addProjectResourceSequence(ResourceSequence.builder().code(0).scopeId(scopeId).scopeType(scopeType).build());
        }
        int result = resourceSequenceDao.generateResourceCode(scopeId, scopeType);
        if (result > 0) {
            ResourceSequence resourceSequence = resourceSequenceDao.getByScopeIdAndScopeType(scopeId, scopeType);
            String code = switchCode(resourceSequence.getCode(), resourceSequence.getScopeType(), targetType);
            return code;
        } else {
            throw new RuntimeException("can not get new project resource code!!! maybe project id not found!!!");
        }
    }

    private String switchCode(Integer code, Integer scopeType, String targetType) {
        if (ScopeTypeEnum.PROJECT.value().equals(scopeType)) {
            return String.valueOf(code);
        } else if (ScopeTypeEnum.TEAM.value().equals(scopeType)) {
            GlobalResourceTypeEnum globalResourceTypeEnum = GlobalResourceTypeEnum.valueFrom(targetType);
            String formatCode;
            switch (Objects.requireNonNull(globalResourceTypeEnum)) {
                case KNOWLEDGE_MANAGE:
                    formatCode = "K-" + code;
                    break;
                case GLOBAL_REQUIREMENT:
                    formatCode = "R-" + code;
                    break;
                default:
                    formatCode = String.valueOf(code);
            }
            return formatCode;
        }
        return null;
    }

    public int addProjectResourceSequence(ResourceSequence resourceSequence) {
        return resourceSequenceDao.insert(resourceSequence);
    }
}
