package net.coding.lib.project.service;

import net.coding.common.cache.evict.constant.CacheType;
import net.coding.common.cache.evict.constant.TableMapping;
import net.coding.common.cache.evict.definition.MappingSerialize;
import net.coding.common.cache.evict.manager.EvictCacheManager;
import net.coding.common.json.Json;
import net.coding.common.redis.api.JedisManager;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.CacheTypeEnum;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectHandCacheService {
    private final JedisManager jedisManager;

    private final static String TABLE_NAME = "projects";

    public void handleProjectCache(Project project, CacheTypeEnum type) {
        if (type == CacheTypeEnum.UPDATE || type == CacheTypeEnum.DELETE) {

            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, project.getId());
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "name", project.getName());

            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "id", project.getId(), "archiveProject");
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "user_owner_id", project.getUserOwnerId(), "name", project.getName(), "archiveProject");
        }
        if (type == CacheTypeEnum.CREATE || type == CacheTypeEnum.DELETE) {
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "userId", project.getUserOwnerId());
        }
        if (type == CacheTypeEnum.UPDATE) {
            // 项目归档，会更新deleted_at
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "userId", project.getUserOwnerId());
        }
        // 清除 getProjectByTeamOwnerIdAndName 方法的缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "getProjectByTeamIdAndName", "team", project.getTeamOwnerId(), "name", project.getName());
        // 清除 getArchiveProjectByTeamIdAndName 方法的缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "getArchiveProjectByTeamIdAndName", "team", project.getTeamOwnerId(), "name", project.getName());
    }

    /**
     * 更新访问项目时间时，清空动态未读条数缓存，因缓存 key 格式特殊，故新增此方法
     *
     * @param projectId
     * @param userId
     */
    public void handleUnReadCache(Integer projectId, Integer userId) {
        String tableName = "activities";
        Optional.ofNullable(TableMapping.CACHE_DEFINITION_MAP)
                .map(MappingSerialize::getEntities)
                .map(d -> d.get(tableName))
                .ifPresent(m -> {
                    String region = m.getRegion();
                    String version = m.getVersion();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(region)
                            .append(":")
                            .append(countCacheKey(version,
                                    "unread", "projectId", projectId, "userId", userId));
                    String cacheKey = stringBuilder.toString();
                    jedisManager.setex(cacheKey, 86400 * 10, "0");
                });
    }

    public void handleProjectMemberCache(ProjectMember pm, CacheTypeEnum type) {
        log.info("清除项目成员相关缓存信息 projectMember : {}", Json.toJson(pm));
        String TABLE_NAME = "project_members";
        if (type == CacheTypeEnum.UPDATE || type == CacheTypeEnum.DELETE) {
            EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.bean, "userId", pm.getUserId(), "projectId", pm.getProjectId());
        }
        // 项目成员变动时，清除getProjectMembersWithArchived（）方法的缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.list, "getProjectMembersWithArchived", pm.getProjectId(), pm.getType());
        // 清除成员统计缓存
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.count, "countProjectMembers:", pm.getProjectId());
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.count, "countProjects:", pm.getUserId());
        EvictCacheManager.evictTableCache(TABLE_NAME, CacheType.list, "getProjectMembersByUser", pm.getUserId());
    }

    protected String countCacheKey(String version, Object... params) {
        return version + ":C:" + StringUtils.join(params, '#');
    }
}
