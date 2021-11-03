package net.coding.lib.project.service.credential;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.credentail.ProjectCredentialTaskDao;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.CredentialTask;
import net.coding.lib.project.enums.CredentialTaskTypeEnums;
import net.coding.lib.project.enums.CredentialTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.CiJobGrpcClient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.ci.CiJobProto;

@Slf4j
@AllArgsConstructor
@Service
public class ProjectCredentialTaskService {
    private final CiJobGrpcClient ciJobGrpcClient;
    private final ProjectCredentialTaskDao projectCredentialTaskDao;

    public List<ConnectionTaskDTO> taskFilterSelected(Integer projectId, Credential credential) {
        List<ConnectionTaskDTO> connectionTaskDTOList = taskList(projectId, credential, null);
        if (CollectionUtils.isNotEmpty(connectionTaskDTOList)) {
            return connectionTaskDTOList.stream()
                    .filter(ConnectionTaskDTO::isSelected)
                    .collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 查询当前凭据关联的 ci job 列表（附加是否有权限的信息） 如果当前凭据不为 {@link CredentialTypeEnums#USERNAME_PASSWORD} 或者
     * {@link CredentialTypeEnums#SSH} 则返回空集合， ci job 暂只能使用这两种凭据 当 credential 为空时，则返回的jobs全部为
     * selected
     */
    public List<ConnectionTaskDTO> taskList(int projectId, Credential credential, String type) {
        List<CredentialTask> credentialTasks = new ArrayList<>();
        if (!checkType(credential, type)) {
            return Collections.emptyList();
        }
        if (credential != null) {
            credentialTasks = projectCredentialTaskDao.getCredentialTask(
                    projectId,
                    credential.getId(),
                    CredentialTaskTypeEnums.JobTask.value(),
                    BeanUtils.getDefaultDeletedAt()
            );
        }
        List<CiJobProto.CiJob> ciJobList = ciJobGrpcClient.listByProject(projectId);
        List<CredentialTask> finalCredentialTasks = credentialTasks;
        if (CollectionUtils.isNotEmpty(ciJobList)) {
            return ciJobList.stream().map(
                    job -> toBuilderConnectionTaskDTO(job, finalCredentialTasks)
            ).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    public void batchToggleTaskPermission(
            int projectId,
            int connId,
            List<ConnectionTaskDTO> taskDTOS
    ) throws CoreException {
        List<CiJobProto.CiJob> ciJobList = ciJobGrpcClient.listByProject(projectId);
        if (CollectionUtils.isNotEmpty(ciJobList)) {
            projectCredentialTaskDao.deleteByCredId(connId);
            List<Integer> ciJobIdList = ciJobList.stream()
                    .map(CiJobProto.CiJob::getId)
                    .collect(Collectors.toList());
            boolean flag = taskDTOS.stream().allMatch(connectionTaskDTO -> {
                if (connectionTaskDTO.getType() == CredentialTaskTypeEnums.JobTask.value()) {
                    return ciJobIdList.contains(connectionTaskDTO.getId());
                }
                return false;
            });
            if (flag) {
                taskDTOS.forEach(connectionTaskDTO -> {
                            try {
                                toggleTaskPermission(
                                        projectId,
                                        connId,
                                        connectionTaskDTO.getType(),
                                        connectionTaskDTO.getId(),
                                        connectionTaskDTO.isSelected()
                                );
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                        }
                );
            } else {
                throw CoreException.of(CoreException.ExceptionType.CI_JOB_NOT_FOUND);
            }
        }
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

    private ConnectionTaskDTO toBuilderConnectionTaskDTO(CiJobProto.CiJob job, List<CredentialTask> perms) {
        return ConnectionTaskDTO.builder()
                .id(job.getId())
                .name(job.getName())
                .selected(hasPermission(job, perms))
                .type(CredentialTaskTypeEnums.JobTask.value())
                .build();
    }

    private boolean hasPermission(CiJobProto.CiJob job, List<CredentialTask> perms) {
        return perms.stream().anyMatch(
                perm -> perm.getType() == CredentialTaskTypeEnums.JobTask.value()
                        && perm.getTaskId() == job.getId()
        );
    }

    private boolean checkType(Credential credential, String type) {
        CredentialTypeEnums credentialType;
        if (credential == null) {
            if (StringUtils.isNotBlank(type)) {
                credentialType = CredentialTypeEnums.valueOf(type);
            } else {
                return false;
            }
        } else {
            credentialType = CredentialTypeEnums.valueOf(credential.getType());
        }
        return credentialType != CredentialTypeEnums.SSH_TOKEN;
    }

    public List<CredentialTask> getTaskIdsByCredentialId(int projectId, int id, boolean decrypt) {
        return Optional.ofNullable(
                projectCredentialTaskDao.getCredentialTask(projectId, id, null, BeanUtils.getDefaultDeletedAt())
        ).orElse(new ArrayList<>());
    }

}
