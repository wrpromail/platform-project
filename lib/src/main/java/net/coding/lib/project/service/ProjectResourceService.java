package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.lib.project.dao.ProjectResourceDao;
import net.coding.lib.project.dao.ProjectResourceSequenceDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ProjectResourceSequence;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

@Service
public class ProjectResourceService {

    @Resource
    private ProjectResourceDao projectResourcesDao;

    @Resource
    private ProjectResourceSequenceService projectResourceSequenceService;

    public ProjectResource getById(Integer id) {
        return null;
    }

    public void updateById(ProjectResource projectResource) {

    }

    public void deleteById(ProjectResource projectResource) {

    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectResource addProjectResource(ProjectResource record) {
        ProjectResource projectResource = findByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            return projectResource;
        }
        int code = projectResourceSequenceService.generateProjectResourceCode(record.getProjectId());
        record.setCode(code);
        record.setCreatedAt(DateUtil.getCurrentDate());
        record.setUpdatedBy(record.getCreatedBy());
        record.setUpdatedAt(DateUtil.getCurrentDate());
        record.setUpdatedAt(record.getCreatedAt());
        record.setDeletedBy(0);
        projectResourcesDao.insert(record);
        return record;
    }

    public ProjectResource updateProjectResource(ProjectResource record) {
        ProjectResource projectResource = findByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            projectResource.setTitle(null == record.getTitle() ? "" : record.getTitle());
            projectResource.setUpdatedAt(DateUtil.getCurrentDate());
            projectResource.setUpdatedBy(record.getUpdatedBy());
            projectResourcesDao.update(projectResource);
        }
        return projectResource;
    }

    public ProjectResource deleteProjectResource(ProjectResource record) {
        ProjectResource projectResource = findByProjectIdAndTypeAndTarget(record.getProjectId(),
                record.getTargetId(), record.getTargetType());
        if (Objects.nonNull(projectResource)) {
            projectResource.setTitle(null == record.getTitle() ? "" : record.getTitle());
            projectResource.setUpdatedAt(DateUtil.getCurrentDate());
            projectResource.setUpdatedBy(record.getUpdatedBy());
            projectResourcesDao.update(projectResource);
        }
        return projectResource;
    }

    public PageInfo<ProjectResource> findProjectResourceList(Integer projectId, Integer page, Integer pageSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        PageInfo<ProjectResource> pageInfo = PageHelper.startPage(1, 3)
                .doSelectPageInfo(() -> projectResourcesDao.findList(parameters));
        return pageInfo;
    }

    public ProjectResource findByProjectIdAndTypeAndTarget(Integer projectId, Integer targetId, String targetType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("targetId", targetId);
        parameters.put("targetType", targetType);
        return projectResourcesDao.findByProjectIdAndTypeAndTarget(parameters);
    }

    public List<ProjectResource> batchProjectResourceList(Integer projectId, List<Integer> codes) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("codes", codes);
        return projectResourcesDao.findList(parameters);
    }
}
