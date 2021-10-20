package net.coding.lib.project.service;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.e.proto.ActivitiesProto.SendActivitiesRequest;
import net.coding.grpc.client.activity.ActivityGrpcClient;
import net.coding.lib.project.dao.IssueLabelDao;
import net.coding.lib.project.dao.ProjectLabelDao;
import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.CoreRuntimeException;
import net.coding.lib.project.form.ProjectLabelForm;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class ProjectLabelService {

    private final ProjectLabelDao projectLabelDao;
    private final ActivityGrpcClient activityGrpcClient;
    private final IssueLabelDao issueLabelDao;
    private final AsyncEventBus asyncEventBus;

    /**
     * 获取项目所有标签
     */
    public List<ProjectLabel> getAllLabelByProject(Integer projectId) {
        return projectLabelDao.findByProjectId(projectId);
    }

    public ProjectLabel findById(Integer id) {
        return projectLabelDao.findById(id);
    }

    /**
     * 新建标签并发送动态.
     *
     * @param userId 当前用户
     */
    @Transactional(rollbackFor = Exception.class)
    public int createLabel(Integer userId, ProjectLabelForm form) {
        ProjectLabel origin = getByNameAndProject(form.getName(), form.getProjectId());
        if (origin != null) {
            throw new CoreRuntimeException(CoreException.ExceptionType.LABEL_EXIST);
        }
        ProjectLabel projectLabel = new ProjectLabel();
        projectLabel.setProjectId(form.getProjectId());
        projectLabel.setOwnerId(userId);
        projectLabel.setName(form.getName());
        projectLabel.setColor(form.getColor());
        int newLabelId = projectLabelDao.insert(projectLabel);
        // 发送任务创建动态
        activityGrpcClient.sendActivity(SendActivitiesRequest.newBuilder()
                .setOwnerId(userId)
                .setType(ProjectLabel.class.getSimpleName())
                .setTargetId(newLabelId)
                .setProjectId(form.getProjectId())
                .setAction(ProjectLabel.ACTION_CREATE_LABEL)
                .setContent(StringUtils.EMPTY)
                .build());
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.common.base.bean.ProjectLabel.class)
                        .targetId(newLabelId)
                        .projectId(form.getProjectId())
                        .action(ProjectLabel.ACTION_CREATE_LABEL)
                        .content(StringUtils.EMPTY)
                        .build()
        );
        return newLabelId;
    }

    /**
     * 更新标签并发送动态.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLabel(Integer userId, int labelId, ProjectLabelForm form) {

        ProjectLabel projectLabel = projectLabelDao.findById(labelId);
        if (projectLabel == null) {
            throw new CoreRuntimeException(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        ProjectLabel origin = getByNameAndProject(form.getName(), form.getProjectId());
        if (origin != null && !projectLabel.getId().equals(origin.getId())) {
            throw new CoreRuntimeException(CoreException.ExceptionType.LABEL_EXIST);
        }
        projectLabel.setName(form.getName());
        projectLabel.setColor(form.getColor());
        int ok = projectLabelDao.update(projectLabel);
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.common.base.bean.ProjectLabel.class)
                        .targetId(labelId)
                        .projectId(form.getProjectId())
                        .action(ProjectLabel.ACTION_UPDATE_LABEL)
                        .content(StringUtils.EMPTY)
                        .build()
        );
        activityGrpcClient.sendActivity(SendActivitiesRequest.newBuilder()
                .setOwnerId(userId)
                .setType(ProjectLabel.class.getSimpleName())
                .setTargetId(labelId)
                .setProjectId(form.getProjectId())
                .setAction(ProjectLabel.ACTION_UPDATE_LABEL)
                .setContent(StringUtils.EMPTY)
                .build());
        return ok > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLabel(Integer userId, Integer labelId, Integer projectId) {
        ProjectLabel projectLabel = projectLabelDao.findById(labelId);
        if (projectLabel == null || !projectId.equals(projectLabel.getProjectId())) {
            throw new CoreRuntimeException(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        issueLabelDao.deleteByLabelId(labelId);
        int ok = projectLabelDao.delete(projectLabel);
        asyncEventBus.post(
                ActivityEvent.builder()
                        .creatorId(userId)
                        .type(net.coding.common.base.bean.ProjectLabel.class)
                        .targetId(labelId)
                        .projectId(projectId)
                        .action(ProjectLabel.ACTION_DELETE_LABEL)
                        .content(StringUtils.EMPTY)
                        .build()
        );
        activityGrpcClient.sendActivity(SendActivitiesRequest.newBuilder()
                .setOwnerId(userId)
                .setType(ProjectLabel.class.getSimpleName())
                .setTargetId(labelId)
                .setProjectId(projectId)
                .setAction(ProjectLabel.ACTION_DELETE_LABEL)
                .setContent(StringUtils.EMPTY)
                .build());
        return ok > 0;
    }

    public List<ProjectLabel> getByIds(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return projectLabelDao.findByIds(ids);
    }

    public ProjectLabel getByNameAndProject(String name, Integer projectId) {
        if (StringUtils.isEmpty(name) || projectId == null) {
            return null;
        }
        return projectLabelDao.getByNameAndProject(name, projectId);
    }
}
