package net.coding.lib.project.credential.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.credential.entity.CredentialTask;
import net.coding.lib.project.credential.enums.CredentialTaskType;
import net.coding.lib.project.credential.enums.CredentialType;
import net.coding.lib.project.dao.credentail.ProjectCredentialTaskDao;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.CiJobGrpcClient;
import net.coding.lib.project.grpc.client.QCIPipelineGrpcClient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.ci.CiJobProto;
import qci.grpc.server.PipelineOuterClass;

@Slf4j
@AllArgsConstructor
@Service
public class ProjectCredentialTaskService {
    private final CiJobGrpcClient ciJobGrpcClient;
    private final ProjectCredentialTaskDao projectCredentialTaskDao;
    private final QCIPipelineGrpcClient qciPipelineGrpcClient;

    public List<ConnectionTaskDTO> taskFilterSelected(Integer projectId, Credential credential) {
        List<ConnectionTaskDTO> connectionTaskDTOList = taskList(projectId, credential, null);
        if (CollectionUtils.isNotEmpty(connectionTaskDTOList)) {
            return connectionTaskDTOList.stream()
                    .filter(ConnectionTaskDTO::isSelected)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 查询当前凭据关联的 ci job 列表（附加是否有权限的信息） 如果当前凭据不为 {@link CredentialType#USERNAME_PASSWORD} 或者
     * {@link CredentialType#SSH} 则返回空集合， ci job 暂只能使用这两种凭据 当 credential 为空时，则返回的jobs全部为
     * selected
     */
    public List<ConnectionTaskDTO> taskList(int projectId, Credential credential, String type) {
        List<CredentialTask> credentialTasks = new ArrayList<>();
        List<ConnectionTaskDTO> connectionTask = new ArrayList<>();
        if (!checkType(credential, type)) {
            return Collections.emptyList();
        }
        if (credential != null) {
            credentialTasks.addAll(projectCredentialTaskDao.getCredentialTask(
                    projectId,
                    credential.getId(),
                    CredentialTaskType.JobTask.value(),
                    BeanUtils.getDefaultDeletedAt()
            ));
            credentialTasks.addAll(projectCredentialTaskDao.getCredentialTask(
                    projectId,
                    credential.getId(),
                    CredentialTaskType.QCITask.value(),
                    BeanUtils.getDefaultDeletedAt()
            ));
        }

        // 持续集成 1.0 的 ci job
        Predicate<CiJobProto.CiJob> jobPredicate =
                job -> StreamEx.of(credentialTasks).anyMatch(c ->
                        CredentialTaskType.JobTask.value() == c.getType()
                                && Objects.equals(c.getTaskId(), job.getId())
                );

        // 持续集成 2.0 的 pipeline
        Predicate<PipelineOuterClass.Pipeline> pipelinePredicate =
                pipeline -> StreamEx.of(credentialTasks).anyMatch(c ->
                        CredentialTaskType.QCITask.value() == c.getType()
                                && Objects.equals(c.getTaskId(), pipeline.getId())
                );

        connectionTask.addAll(
                Optional.ofNullable(ciJobGrpcClient.listByProject(projectId))
                        .map(Collection::stream)
                        .orElse(StreamEx.empty())
                        .map(t ->
                                ConnectionTaskDTO.builder()
                                        .id(t.getId())
                                        .name(t.getName())
                                        .selected(jobPredicate.test(t))
                                        .type(CredentialTaskType.JobTask.value())
                                        .build()
                        )
                        .collect(Collectors.toList())
        );

        connectionTask.addAll(
                Optional.ofNullable(
                                qciPipelineGrpcClient.listByProjectId(projectId)
                        )
                        .map(Collection::stream)
                        .orElse(StreamEx.empty())
                        .map(t ->
                                ConnectionTaskDTO.builder()
                                        .id(t.getId())
                                        .name(t.getName())
                                        .selected(pipelinePredicate.test(t))
                                        .type(CredentialTaskType.QCITask.value())
                                        .build()
                        )
                        .collect(Collectors.toList())
        );

        return connectionTask;
    }

    public void batchToggleTaskPermission(
            int projectId,
            int connId,
            List<ConnectionTaskDTO> tasks
    ) throws CoreException {
        Set<Integer> cciIds = StreamEx
                .of(ciJobGrpcClient.listByProject(projectId))
                .map(CiJobProto.CiJob::getId)
                .toSet();
        Set<Integer> qciIds = StreamEx
                .of(qciPipelineGrpcClient.listByProjectId(projectId))
                .map(PipelineOuterClass.Pipeline::getId)
                .toSet();
        Set<Integer> cciSelected = StreamEx.of(tasks)
                .filterBy(ConnectionTaskDTO::getType, CredentialTaskType.JobTask.value())
                .map(ConnectionTaskDTO::getId)
                .toSet();
        Set<Integer> qciSelected = StreamEx.of(tasks)
                .filterBy(ConnectionTaskDTO::getType, CredentialTaskType.QCITask.value())
                .map(ConnectionTaskDTO::getId)
                .toSet();
        boolean cciMatched = cciIds.containsAll(cciSelected);
        boolean qciMatched = qciIds.containsAll(qciSelected);
        if (!qciMatched || !cciMatched) {
            throw CoreException.of(CoreException.ExceptionType.CI_JOB_NOT_FOUND);
        }
        projectCredentialTaskDao.deleteByCredId(connId);
        tasks.forEach(dto -> {
                    try {
                        toggleTaskPermission(
                                projectId,
                                connId,
                                dto.getType(),
                                dto.getId(),
                                dto.isSelected()
                        );
                    } catch (CoreException e) {
                        log.warn(
                                "Toggle credential task permission failure, cause of {}",
                                e.getMessage()
                        );
                    }
                }
        );
    }

    public void toggleTaskPermission(
            int projectId,
            int connId,
            int type,
            int taskId,
            boolean selected
    ) throws CoreException {
        if (selected) {
            CredentialTask credentialTask = CredentialTask.builder()
                    .projectId(projectId)
                    .connId(connId)
                    .taskId(taskId)
                    .type((short) type)
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .build();
            projectCredentialTaskDao.insert(credentialTask);
        }
    }

    private boolean checkType(Credential credential, String type) {
        CredentialType credentialType;
        if (credential == null) {
            if (StringUtils.isNotBlank(type)) {
                credentialType = CredentialType.valueOf(type);
            } else {
                return false;
            }
        } else {
            credentialType = CredentialType.valueOf(credential.getType());
        }
        return credentialType != CredentialType.SSH_TOKEN;
    }

    public List<CredentialTask> getTaskIdsByCredentialId(int projectId, int id) {
        return Optional.ofNullable(
                projectCredentialTaskDao.getCredentialTask(projectId, id, null, BeanUtils.getDefaultDeletedAt())
        ).orElse(new ArrayList<>());
    }

}
