package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;
import lombok.extern.slf4j.Slf4j;
import net.coding.common.base.event.ProjectArchiveEvent;
import net.coding.common.base.event.ProjectDeleteEvent;
import net.coding.common.base.event.ProjectUnarchiveEvent;
import net.coding.common.eventbus.AsyncExternalEventBus;
import net.coding.common.eventbus.Pubsub;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.events.all.platform.CommonProto;
import net.coding.events.all.platform.CommonProto.Operator;
import net.coding.events.all.platform.CommonProto.Team;
import net.coding.events.all.platform.ProjectEventProto.ProjectArchivedEvent;
import net.coding.events.all.platform.ProjectEventProto.ProjectDeletedEvent;
import net.coding.events.all.platform.ProjectEventProto.ProjectDisplayNameChangeEvent;
import net.coding.events.all.platform.ProjectEventProto.ProjectUnArchivedEvent;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProgramMemberService;
import net.coding.lib.project.service.RamTransformTeamService;
import net.coding.lib.project.service.member.ProgramMemberPrincipalService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectAdaptorService extends AbstractProjectAdaptorService {

    private final RamTransformTeamService ramTransformTeamService;
    private final ProgramMemberService programMemberService;
    private final ProgramMemberPrincipalService programMemberPrincipalService;
    private final AsyncExternalEventBus asyncExternalEventBus;

    public ProjectAdaptorService(AsyncEventBus asyncEventBus, LoggingGrpcClient loggingGrpcClient,
            TeamGrpcClient teamGrpcClient, UserGrpcClient userGrpcClient,
            AclServiceGrpcClient aclServiceGrpcClient, LocaleMessageSource localeMessageSource,
            NotificationGrpcClient notificationGrpcClient, Pubsub pubsub,
            RamTransformTeamService ramTransformTeamService,
            ProgramMemberService programMemberService,
            ProgramMemberPrincipalService programMemberPrincipalService,
            AsyncExternalEventBus asyncExternalEventBus) {
        super(asyncEventBus, loggingGrpcClient, teamGrpcClient, userGrpcClient,
                aclServiceGrpcClient, localeMessageSource, notificationGrpcClient, pubsub);
        this.ramTransformTeamService = ramTransformTeamService;
        this.programMemberService = programMemberService;
        this.programMemberPrincipalService = programMemberPrincipalService;
        this.asyncExternalEventBus = asyncExternalEventBus;
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
        // 新事件
        asyncExternalEventBus.post(ProjectDeletedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProject(CommonProto.Project.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());

    }

    @Override
    public void projectArchiveEvent(Integer userId, Project project) {
        ProjectArchiveEvent event = new ProjectArchiveEvent();
        event.setTeamId(project.getTeamOwnerId());
        event.setProjectId(project.getId());
        event.setOwnerId(userId);
        // 这个事件需要逐步去掉
        asyncEventBus.post(event);
        // 新事件
        asyncExternalEventBus.post(ProjectArchivedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProject(CommonProto.Project.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());

    }

    @Override
    public void projectUnArchiveEvent(Integer userId, Project project) {
        // 这个事件需要逐步去掉
        asyncEventBus.post(
                ProjectUnarchiveEvent.builder()
                        .teamId(project.getTeamOwnerId())
                        .projectId(project.getId())
                        .ownerId(userId).build()
        );
        // 新事件
        asyncExternalEventBus.post(ProjectUnArchivedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProject(CommonProto.Project.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());
    }

    @Override
    public void deleteProgramMember(Integer teamId, Integer userId, Project project) {
        if (ramTransformTeamService.ramOnline(teamId)) {
            programMemberPrincipalService.removeProgramProjects(teamId, userId, project);
        } else {
            programMemberService.removeProgramProjects(teamId, userId, project);
        }
        //删除事项
        programMemberService.removeProgramIssueRelation(0, project.getId());
    }

    @Override
    public void checkProgramTime(Project program) {

    }

    @Override
    public void checkProgramPay(Integer teamId) {

    }

    @Override
    public void postDisplayNameChangeEvent(int userId, Project oldProject, Project newProject) {
        // 新事件
        asyncExternalEventBus.post(ProjectDisplayNameChangeEvent.newBuilder()
                .setTeamId(newProject.getTeamOwnerId())
                .setProjectId(newProject.getId())
                .setNewDisplayName(newProject.getDisplayName())
                .setTeam(Team.newBuilder()
                        .setId(newProject.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setPreProject(CommonProto.Project.newBuilder()
                        .setId(oldProject.getId())
                        .setDisplayName(oldProject.getDisplayName())
                        .build())
                .setPostProject(CommonProto.Project.newBuilder()
                        .setId(newProject.getId())
                        .setDisplayName(newProject.getDisplayName())
                        .build())
                .build());
    }
}
