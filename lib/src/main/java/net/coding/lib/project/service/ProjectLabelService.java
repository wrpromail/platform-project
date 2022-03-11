package net.coding.lib.project.service;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.e.grpcClient.collaboration.IssueLabelGrpcClient;
import net.coding.e.proto.ActivitiesProto.SendActivitiesRequest;
import net.coding.grpc.client.activity.ActivityGrpcClient;
import net.coding.lib.project.dao.ProjectLabelDao;
import net.coding.lib.project.entity.ProjectLabel;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.CoreRuntimeException;
import net.coding.lib.project.form.ProjectLabelForm;

import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class ProjectLabelService {

    private final ProjectLabelDao projectLabelDao;
    private final ActivityGrpcClient activityGrpcClient;
    private final IssueLabelGrpcClient issueLabelGrpcClient;
    private final AsyncEventBus asyncEventBus;
    private final RedissonClient redissonClient;

    /**
     * 获取项目所有标签
     */
    public List<ProjectLabel> getAllLabelByProject(Integer projectId) {
        return projectLabelDao.findByProjectId(projectId);
    }

    public ProjectLabel findById(Integer id) {
        return projectLabelDao.findById(id);
    }

    public ProjectLabel findByIdWithDeleted(Integer id) {
        return projectLabelDao.findByIdWitDeleted(id);
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
     * 原子性创建标签
     *
     * @param projectId
     * @param userId
     * @param name
     * @param color
     * @return
     */
    public ProjectLabel getOrCreateLabel(Integer projectId, Integer userId, String name, String color) {
        RLock rLock = redissonClient.getLock(String.format("getOrCreateLabel:projectId:%d.lock", projectId));
        try {
            rLock.lock(30L, TimeUnit.SECONDS);
            ProjectLabel projectLabel = getByNameAndProject(
                    name,
                    projectId
            );
            if (projectLabel != null) {
                return projectLabel;
            } else {
                ProjectLabel newProjectLabel = new ProjectLabel();
                newProjectLabel.setProjectId(projectId);
                newProjectLabel.setOwnerId(userId);
                newProjectLabel.setName(name);
                newProjectLabel.setColor(color);
                int newLabelId = projectLabelDao.insert(newProjectLabel);
                newProjectLabel.setId(newLabelId);
                activityGrpcClient.sendActivity(SendActivitiesRequest.newBuilder()
                        .setOwnerId(userId)
                        .setType(ProjectLabel.class.getSimpleName())
                        .setTargetId(newLabelId)
                        .setProjectId(projectId)
                        .setAction(ProjectLabel.ACTION_CREATE_LABEL)
                        .setContent(StringUtils.EMPTY)
                        .build());
                asyncEventBus.post(
                        ActivityEvent.builder()
                                .creatorId(userId)
                                .type(net.coding.common.base.bean.ProjectLabel.class)
                                .targetId(newLabelId)
                                .projectId(projectId)
                                .action(ProjectLabel.ACTION_CREATE_LABEL)
                                .content(StringUtils.EMPTY)
                                .build()
                );
                return newProjectLabel;
            }
        } finally {
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
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
        try {
            issueLabelGrpcClient.deleteIssueLabelById(labelId, projectId);
        } catch (Exception e) {
            log.error("RPC issueLabelGrpcClient.deleteIssueLabelById error , message {}", e.getMessage());
        }
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

    public List<ProjectLabel> getByIdsWithDeleted(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return projectLabelDao.findByIdsWithDeleted(ids);
    }

    public ProjectLabel getByNameAndProject(String name, Integer projectId) {
        if (StringUtils.isEmpty(name) || projectId == null) {
            return null;
        }
        return projectLabelDao.getByNameAndProject(name, projectId);
    }

    public List<ProjectLabel> getLabelsByProjectIdAndNames(List<String> names, List<Integer> projectIdList) {
        if (names.isEmpty() || projectIdList.isEmpty()) {
            return Collections.emptyList();
        }
        return projectLabelDao.getLabelsByProjectIdAndNames(names, projectIdList);
    }

    public List<ProjectLabel> getLabelsByProjectIds(List<Integer> projectIdList) {
        if (projectIdList.isEmpty()) {
            return Collections.emptyList();
        }
        return projectLabelDao.getLabelsByProjects( projectIdList);
    }
}
