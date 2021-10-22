package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ProjectArchiveEvent;
import net.coding.common.base.event.ProjectDeleteEvent;
import net.coding.common.base.event.ProjectUnarchiveEvent;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.service.ProgramMemberService;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProjectAdaptorService extends AbstractProjectAdaptorService {
    private final ProgramMemberService programMemberService;

    public ProjectAdaptorService(AsyncEventBus asyncEventBus, LoggingGrpcClient loggingGrpcClient, TeamGrpcClient teamGrpcClient, AclServiceGrpcClient aclServiceGrpcClient, LocaleMessageSource localeMessageSource, ProgramMemberService programMemberService) {
        super(asyncEventBus, loggingGrpcClient, teamGrpcClient, aclServiceGrpcClient, localeMessageSource);
        this.programMemberService = programMemberService;
    }

    @Override
    public Integer pmType() {
        return PmTypeEnums.PROJECT.getType();
    }

    @Override
    public Class Clazz() {
        return net.coding.e.lib.core.bean.Project.class;
    }

    @Override
    protected String updateNameLog() {
        return "project_update_name";
    }

    @Override
    protected String updateDisplayNameLog() {
        return "project_update_display_name";
    }

    @Override
    protected String updateNameAndDisplayNameLog() {
        return "project_update_name_and_display_name";
    }

    @Override
    public void projectCreateEvent(Integer userId, Project project) {

    }

    @Override
    public void projectDeleteEvent(Integer userId, Project project) {
        asyncEventBus.post(
                ProjectDeleteEvent.builder()
                        .teamId(project.getTeamOwnerId())
                        .userId(userId)
                        .projectId(project.getId())
                        .build()
        );
    }

    @Override
    public void projectArchiveEvent(Integer userId, Project project) {
        ProjectArchiveEvent event = new ProjectArchiveEvent();
        event.setTeamId(project.getTeamOwnerId());
        event.setProjectId(project.getId());
        event.setOwnerId(userId);
        asyncEventBus.post(event);
    }

    @Override
    public void projectUnArchiveEvent(Integer userId, Project project) {
        asyncEventBus.post(
                ProjectUnarchiveEvent.builder()
                        .teamId(project.getTeamOwnerId())
                        .projectId(project.getId())
                        .ownerId(userId).build()
        );
    }

    @Override
    public void deleteProgramMember(Integer teamId, Project project) {
        programMemberService.removeProgramProjects(teamId, project);
        //删除事项
        programMemberService.removeProgramIssueRelation(0, project.getId());
    }

    @Override
    public void checkProgramTime(Project program) {

    }

    @Override
    public void checkProgramPay(Integer teamId) {

    }
}
