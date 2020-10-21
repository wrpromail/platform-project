package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.lib.project.dao.ProjectResourceDao;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.enums.NotSearchTargetTypeEnum;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
        if(StringUtils.isNotEmpty(keyword)) {
            parameters.put("keyword", "%" + keyword + "%");
        }
        if(Objects.nonNull(targetTypes) && targetTypes.size() > 0) {
            parameters.put("targetTypes", targetTypes);
        }
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

    public ProjectResource findByProjectIdAndCode(Integer projectId, Integer code) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("code", code);
        //排除不需要搜索的目标类型
        parameters.put("targetTypes", NotSearchTargetTypeEnum.getTargetTypes());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectResourcesDao.findByProjectIdAndCode(parameters);
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

    public ProjectResource selectById(Integer projectResourceId) {
        return projectResourcesDao.selectById(projectResourceId);
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
        parameters.put("code", code);
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
}
