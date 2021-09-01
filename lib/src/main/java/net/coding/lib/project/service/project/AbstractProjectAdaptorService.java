package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ActivityEvent;
import net.coding.common.base.event.ProjectNameChangeEvent;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.ProgramProjectEventEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.logging.loggingProto;
import proto.platform.permission.PermissionProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.NETWORK_CONNECTION_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static org.apache.logging.log4j.util.Strings.EMPTY;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractProjectAdaptorService {
    protected final AsyncEventBus asyncEventBus;

    protected final LoggingGrpcClient loggingGrpcClient;

    protected final TeamGrpcClient teamGrpcClient;

    protected final AclServiceGrpcClient aclServiceGrpcClient;

    protected final LocaleMessageSource localeMessageSource;

    public abstract Integer pmType();

    public abstract Class Clazz();

    public abstract void projectCreateEvent(Integer userId, Project project);

    public abstract void projectDeleteEvent(Integer userId, Project project);

    public abstract void projectArchiveEvent(Integer userId, Project project);

    public abstract void projectUnArchiveEvent(Integer userId, Project project);

    public abstract void deleteProgramMember(Integer teamId, Project project);

    public abstract void checkProgramTime(Project program) throws MilestoneException, CoreException;


    public void postProjectCreateEvent(Integer userId, Project project, Short action) {
        projectCreateEvent(userId, project);

        postActivityEvent(userId, project, action);

        insertOperationLog(userId, project, action);
    }

    public void postProjectDeleteEvent(Integer userId, Project project, Short action) {
        projectDeleteEvent(userId, project);

        postActivityEvent(userId, project, action);

        insertOperationLog(userId, project, action);
    }

    public void postProjectArchiveEvent(Integer userId, Project project, Short action) {
        projectArchiveEvent(userId, project);

        postActivityEvent(userId, project, action);

        insertOperationLog(userId, project, action);
    }

    public void postProjectUnArchiveEvent(Integer userId, Project project, Short action) {
        projectUnArchiveEvent(userId, project);

        postActivityEvent(userId, project, action);

        insertOperationLog(userId, project, action);
    }

    public void postActivityEvent(Integer userId, Project project, Short action) {
        asyncEventBus.post(ActivityEvent.builder()
                .type(Clazz())
                .creatorId(userId)
                .targetId(project.getId())
                .projectId(project.getId())
                .action(action)
                .content(StringUtils.EMPTY)
                .build());
    }

    public void postProjectNameChangeEvent(Project project) {
        ProjectNameChangeEvent projectNameChangeEvent =
                ProjectNameChangeEvent.builder()
                        .projectId(project.getId())
                        .newName(project.getName())
                        .build();
        asyncEventBus.post(projectNameChangeEvent);
    }

    public void insertOperationLog(Integer userId, Project project, Short action) {
        Optional.ofNullable(ProgramProjectEventEnums.of(action, project.getPmType()))
                .ifPresent(eventEnums ->
                        loggingGrpcClient.insertOperationLog(loggingProto.OperationLogInsertRequest.newBuilder()
                                .setUserId(userId)
                                .setTeamId(project.getTeamOwnerId())
                                .setContentName(eventEnums.name())
                                .setTargetId(project.getId())
                                .setTargetType(project.getClass().getSimpleName())
                                .setAdminAction(false)
                                .setText(localeMessageSource.getMessage(eventEnums.getMessage(),
                                        new Object[]{EMPTY
                                                , htmlLink(project)}).trim())
                                .build()));
    }

    public void hasPermissionInEnterprise(Integer teamId, Integer userId, Integer program, Short action) throws CoreException {
        boolean hasPermissionInEnterprise = hasEnterprisePermission(teamId, userId, program, action);
        if (!hasPermissionInEnterprise) {
            throw CoreException.of(PERMISSION_DENIED);
        }
    }

    public boolean hasEnterprisePermission(Integer teamId, Integer userId, Integer program, Short action) throws CoreException {
        ProgramProjectEventEnums eventEnums = Optional.ofNullable(ProgramProjectEventEnums.of(action, program))
                .orElse(null);
        if (Objects.nonNull(eventEnums)) {
            try {
                return aclServiceGrpcClient.hasPermissionInEnterprise(
                        PermissionProto.Permission.newBuilder()
                                .setFunction(eventEnums.getPermissionFunction())
                                .setAction(eventEnums.getPermissionAction())
                                .build(),
                        userId,
                        teamId
                );
            } catch (Exception e) {
                log.warn("hasPermissionInEnterprise {}", e.getMessage());
                throw CoreException.of(NETWORK_CONNECTION_ERROR);
            }
        }
        return false;
    }


    public String htmlLink(Project project) {
        String host = teamGrpcClient.getTeamHostWithProtocolByTeamId(project.getTeamOwnerId());
        StringBuilder sb = new StringBuilder();
        sb.append("<a href='");
        sb.append(host);
        sb.append("/p/" + project.getName());
        sb.append("' target='_blank'>");
        sb.append(StringUtils.defaultIfBlank(project.getDisplayName(), project.getName()));
        sb.append("</a>");
        return sb.toString();
    }
}
