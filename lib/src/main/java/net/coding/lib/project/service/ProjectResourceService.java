package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.lib.project.dao.ProjectResourceDao;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.enums.NotSearchTargetTypeEnum;
import net.coding.lib.project.enums.ScopeTypeEnum;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

@Service
public class ProjectResourceService {

    @Resource
    private ProjectResourceDao projectResourcesDao;

    private static final Map<String, String> typeToUrlMap = new HashMap<String, String>() {{
        put("merge-request-bean", "_buildMergeRequestLink");
        put("pull-request-bean", "git/pull");
        put("project-topic", "topic");
        put("project-file", "_buildProjectFileLink");
        put("release", "_buildReleaseLink");
        put("wiki", "wiki");
        put("defect", "defect");
        put("mission", "mission");
        put("requirement", "requirement");
        put("iteration", "iteration");
        put("sub-task", "subtask");
        put("epic", "epic");
        put("external-link", "_buildExternalLinkUrl");
        put("testing-plan-case-result", "_buildTestingPlanCaseResultUrl");
    }};

    public ProjectResource updateProjectResource(ProjectResource record) {
        ProjectResource projectResource = getByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            projectResource.setTitle(null == record.getTitle() ? "" : record.getTitle());
            projectResource.setUpdatedAt(DateUtil.getCurrentDate());
            projectResource.setUpdatedBy(record.getUpdatedBy());
            update(projectResource);
        }
        return projectResource;
    }

    public int insert(ProjectResource record) {
        return projectResourcesDao.insert(record);
    }

    public int update(ProjectResource projectResource) {
        return projectResourcesDao.update(projectResource);
    }

    public int batchDelete(Map<String, Object> parameters) {
        return projectResourcesDao.batchDelete(parameters);
    }

    public PageInfo<ProjectResource> findProjectResourceList(Integer projectId, String keyword, List<String> targetTypes, Integer page, Integer pageSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        if (StringUtils.isNotEmpty(keyword)) {
            parameters.put("keyword", "%" + keyword + "%");
        }
        if (Objects.nonNull(targetTypes) && targetTypes.size() > 0) {
            parameters.put("targetTypes", targetTypes);
        }
        parameters.put("scopeType", ScopeTypeEnum.PROJECT.value());
        //排除不需要搜索的目标类型
        parameters.put("notTargetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        PageInfo<ProjectResource> pageInfo = PageHelper.startPage(page, pageSize)
                .doSelectPageInfo(() -> projectResourcesDao.findList(parameters));
        return pageInfo;
    }

    public PageInfo<ProjectResource> findGlobalResourceList(Integer teamId, String keyword, List<String> targetTypes, Integer page, Integer pageSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", teamId);
        if (StringUtils.isNotEmpty(keyword)) {
            parameters.put("keyword", "%" + keyword + "%");
        }
        if (Objects.nonNull(targetTypes) && targetTypes.size() > 0) {
            parameters.put("targetTypes", targetTypes);
        }
        parameters.put("scopeType", ScopeTypeEnum.TEAM.value());
        //排除不需要搜索的目标类型
        parameters.put("notTargetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        PageInfo<ProjectResource> pageInfo = PageHelper.startPage(page, pageSize)
                .doSelectPageInfo(() -> projectResourcesDao.findList(parameters));
        return pageInfo;
    }


    public ProjectResource getByProjectIdAndTypeAndTarget(Integer projectId, Integer targetId, String targetType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("targetId", targetId);
        parameters.put("targetType", targetType);
        //parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.getByProjectIdAndTypeAndTarget(parameters);
    }

    public ProjectResource getByScopeIdAndScopeTypeAndTypeAndTarget(Integer scopeId, Integer scopeType, Integer targetId, String targetType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("targetId", targetId);
        parameters.put("targetType", targetType);
        //parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.getByScopeIdAndScopeTypeAndTypeAndTarget(parameters);
    }

    public List<ProjectResource> batchListByProjectAndTypeAndTargets(Integer projectId, List<Integer> targetIds, String targetType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("targetType", targetType);
        parameters.put("targetIds", targetIds);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.findList(parameters);
    }

    public List<ProjectResource> batchProjectResourceList(Integer projectId, List<Integer> codes) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("codes", codes);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.findList(parameters);
    }

    public ProjectResource relateProjectResource(ProjectResource record) {
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedBy(record.getCreatedBy());
        record.setUpdatedAt(record.getCreatedAt());
        record.setDeletedBy(0);
        projectResourcesDao.insert(record);
        return record;
    }

    public ProjectResource getByProjectIdAndCode(Integer projectId, Integer code) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", String.valueOf(code));
        //排除不需要搜索的目标类型
        parameters.put("targetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.getByProjectIdAndCode(parameters);
    }

    public int batchRelateResource(List<ProjectResource> projectResourceList) {
        return projectResourcesDao.batchInsert(projectResourceList);
    }

    public int countByProjectIdAndCodes(Integer projectId, List<Integer> codeList) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("codes", codeList);
        return projectResourcesDao.countByProjectIdAndCodes(parameters);
    }

    public ProjectResource getById(Integer projectResourceId) {
        return projectResourcesDao.getById(projectResourceId);
    }

    public List<ProjectResource> batchListByTypeAndTargets(String targetType, List<Integer> targetIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetType", targetType);
        parameters.put("targetIds", targetIds);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.findList(parameters);
    }

    public ProjectResource getProjectResourceWithDeleted(Integer projectId, Integer code) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", String.valueOf(code));
        //排除不需要搜索的目标类型
        parameters.put("targetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        //parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.getProjectResourceWithDeleted(parameters);
    }

    public List<Integer> findFixResourceList(Integer id) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        return projectResourcesDao.findFixResourceList(parameters);
    }

    public Integer getBeginFixId() {
        return projectResourcesDao.getBeginFixId();
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateProjectResource(Integer projectId, List<Integer> targetIds, String currentTargetType, String targetTargetType, Integer userId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("targetIds", targetIds);
        parameters.put("currentTargetType", currentTargetType);
        parameters.put("targetTargetType", targetTargetType);
        parameters.put("updatedAt", DateUtil.getCurrentDate());
        parameters.put("updatedBy", userId);
        return projectResourcesDao.batchUpdateProjectResource(parameters);
    }

    public int delete(Map<String, Object> parameters) {
        return projectResourcesDao.delete(parameters);
    }

    public List<ProjectResource> findResourceList(Integer scopeId, String resourceId, String title, List<String> targetTypes, Integer pageSize, Integer scopeType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        if (StringUtils.isNotEmpty(resourceId)) {
            parameters.put("resourceId", "%" + resourceId + "%");
        }
        if (StringUtils.isNotEmpty(title)) {
            parameters.put("title", "%" + title + "%");
        }
        if (Objects.nonNull(targetTypes) && targetTypes.size() > 0) {
            parameters.put("targetTypes", targetTypes);
        }
        parameters.put("scopeType", scopeType);
        //排除不需要搜索的目标类型
        parameters.put("notTargetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        parameters.put("pageSize", pageSize);
        List<ProjectResource> projectResourceList = projectResourcesDao.findListForKm(parameters);
        return projectResourceList != null ? projectResourceList : new ArrayList<ProjectResource>();
    }

    public ProjectResource findProjectResourceDetail(Integer scopeId, String code, Integer scopeType) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        if (StringUtils.isNotEmpty(code)) {
            parameters.put("code", code);
        }
        if (scopeType != null) {
            parameters.put("scopeType", scopeType);
        }
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        ProjectResource projectResource = projectResourcesDao.findProjectResourceDetail(parameters);
        return projectResource;
    }

    public void recoverResource(Map<String, Object> parameters) {
        projectResourcesDao.recoverResource(parameters);
    }
}
