package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ProgramEvent;
import net.coding.common.eventbus.AsyncExternalEventBus;
import net.coding.common.eventbus.Pubsub;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.grpcClient.collaboration.MilestoneGrpcClient;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
import net.coding.events.all.platform.CommonProto;
import net.coding.events.all.platform.CommonProto.Operator;
import net.coding.events.all.platform.CommonProto.Program;
import net.coding.events.all.platform.CommonProto.Team;
import net.coding.events.all.platform.ProgramProto.ProgramArchivedEvent;
import net.coding.events.all.platform.ProgramProto.ProgramDeletedEvent;
import net.coding.events.all.platform.ProgramProto.ProgramDisplayNameUpdatedEvent;
import net.coding.events.all.platform.ProgramProto.ProgramUnArchivedEvent;
import net.coding.events.all.platform.ProjectEventProto;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.grpc.client.platform.SystemSettingGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.NotificationGrpcClient;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProgramMemberService;
import net.coding.platform.charge.api.pojo.EnterpriseInfoDTO;
import net.coding.platform.charge.client.grpc.EnterpriseGrpcClient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import proto.platform.system.setting.SystemSettingProto;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROGRAM_START_AFTER_MILESTONE;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_CHARGE_NOT_ADVANCED_PAY;
import static net.coding.lib.project.service.ProgramService.PLATFORM_FEATURE_PROGRAM_PAYMENT;
import static net.coding.lib.project.service.ProgramService.TYPE_ADVANCED_PAY;
import static net.coding.lib.project.service.ProgramService.TYPE_FLAGSHIP_PAY;

@Slf4j
@Service
public class ProgramAdaptorService extends AbstractProjectAdaptorService {
    protected final AsyncExternalEventBus asyncExternalEventBus;

    private final MilestoneGrpcClient milestoneGrpcClient;

    private final EnterpriseGrpcClient enterpriseGrpcClient;

    private final ProgramMemberService programMemberService;

    private final SystemSettingGrpcClient systemSettingGrpcClient;

    public ProgramAdaptorService(AsyncEventBus asyncEventBus, LoggingGrpcClient loggingGrpcClient, TeamGrpcClient teamGrpcClient, UserGrpcClient userGrpcClient, AclServiceGrpcClient aclServiceGrpcClient, LocaleMessageSource localeMessageSource, NotificationGrpcClient notificationGrpcClient, Pubsub pubsub, AsyncExternalEventBus asyncExternalEventBus, MilestoneGrpcClient milestoneGrpcClient, EnterpriseGrpcClient enterpriseGrpcClient, ProgramMemberService programMemberService, SystemSettingGrpcClient systemSettingGrpcClient) {
        super(asyncEventBus, loggingGrpcClient, teamGrpcClient, userGrpcClient, aclServiceGrpcClient, localeMessageSource, notificationGrpcClient, pubsub);
        this.asyncExternalEventBus = asyncExternalEventBus;
        this.milestoneGrpcClient = milestoneGrpcClient;
        this.enterpriseGrpcClient = enterpriseGrpcClient;
        this.programMemberService = programMemberService;
        this.systemSettingGrpcClient = systemSettingGrpcClient;
    }


    @Override
    public Integer pmType() {
        return PmTypeEnums.PROGRAM.getType();
    }

    @Override
    public Class Clazz() {
        return net.coding.e.lib.core.bean.Program.class;
    }

    @Override
    protected String updateNameLog() {
        return "program_update_name";
    }

    @Override
    protected String updateDisplayNameLog() {
        return "program_update_display_name";
    }

    @Override
    protected String updateNameAndDisplayNameLog() {
        return "program_update_name_and_display_name";
    }

    @Override
    public void projectCreateEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.Create);
    }

    @Override
    public void projectDeleteEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.DELETE);
        asyncExternalEventBus.post(ProgramDeletedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProgram(Program.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());
    }

    @Override
    public void projectArchiveEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.ARCHIVE);
        asyncExternalEventBus.post(ProgramArchivedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProgram(Program.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());
    }

    @Override
    public void projectUnArchiveEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.UNARCHIVE);
        asyncExternalEventBus.post(ProgramUnArchivedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(project.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setProgram(Program.newBuilder()
                        .setId(project.getId())
                        .setDisplayName(project.getDisplayName())
                        .build())
                .build());
    }

    @Override
    public void deleteProgramMember(Integer teamId, Integer userId, Project program) {
        //????????????
        programMemberService.removeProgramIssueRelation(program.getId(), 0);
    }

    @Override
    public void checkProgramTime(Project program) throws MilestoneException, CoreException {
        if (program.getStartDate() != null && program.getEndDate() != null) {
            try {
                milestoneGrpcClient.checkProgramTime(
                        new Timestamp(program.getStartDate().getTime()),
                        new Timestamp(program.getEndDate().getTime()),
                        program.getId());
            } catch (MilestoneException ex) {
                log.error("CheckProgramTime Error , StartDate = {}, EndDate = {}, ProgramId = {}",
                        new Timestamp(program.getStartDate().getTime()),
                        new Timestamp(program.getEndDate().getTime()),
                        program.getId(),
                        ex);
                //21003 ?????????????????????????????????????????????
                //21004 ?????????????????????????????????????????????
                if (ex.getCode() == 21003 || ex.getCode() == 21004) {
                    throw CoreException.of(PROGRAM_START_AFTER_MILESTONE);
                }
            }
        }
    }

    /**
     * ????????????????????????/?????????????????????????????????
     */
    @Override
    public void checkProgramPay(Integer currentTeamId) throws CoreException {
        boolean isProgramPay;
        try {
            SystemSettingProto.SystemSettingResponse response =
                    systemSettingGrpcClient.get(SystemSettingProto.SystemSettingRequest.newBuilder()
                            .setCode(PLATFORM_FEATURE_PROGRAM_PAYMENT)
                            .build());
            isProgramPay = Optional.ofNullable(response.getSetting())
                    .map(SystemSettingProto.SystemSetting::getValue)
                    .filter(StringUtils::isNotBlank)
                    .map(Boolean::valueOf)
                    .orElse(TRUE);
        } catch (Exception ex) {
            log.error("CheckProgramPay systemSettingGrpcClient get Error {}", ex.getMessage());
            isProgramPay = TRUE;
        }
        if (!isProgramPay) {
            return;
        }
        boolean isPay;
        try {
            isPay = Optional.ofNullable(enterpriseGrpcClient.getInfo(currentTeamId))
                    .map(EnterpriseInfoDTO::getType)
                    .filter(type -> (type == TYPE_ADVANCED_PAY || type == TYPE_FLAGSHIP_PAY))
                    .isPresent();
        } catch (Exception ex) {
            log.error("CheckProgramPay enterpriseGrpcClient getInfo Error {}", ex.getMessage());
            isPay = FALSE;
        }
        if (!isPay) {
            throw CoreException.of(TEAM_CHARGE_NOT_ADVANCED_PAY);
        }
    }

    public void postProgramEvent(Integer userId, Project project, ProgramEvent.Function function) {
        asyncExternalEventBus.postLocal(ProgramEvent.builder()
                .teamId(project.getTeamOwnerId())
                .userId(userId)
                .projectId(project.getId())
                .function(function)
                .build());
    }

    @Override
    public void postDisplayNameChangeEvent(int userId, Project oldProgram, Project newProgram) {
        // ????????????????????????
        ProjectEventProto.ProjectDisplayNameChangeEvent build =
                ProjectEventProto.ProjectDisplayNameChangeEvent.newBuilder()
                        .setTeamId(newProgram.getTeamOwnerId())
                        .setProjectId(newProgram.getId())
                        .setNewDisplayName(newProgram.getDisplayName())
                        .build();
        pubsub.publish(build);
        // ?????????
        asyncExternalEventBus.post(ProgramDisplayNameUpdatedEvent.newBuilder()
                .setTeam(Team.newBuilder()
                        .setId(newProgram.getTeamOwnerId())
                        .build())
                .setOperator(Operator.newBuilder()
                        .setId(userId)
                        .setLocale(localeMessageSource.getLocale().toString())
                        .build())
                .setPreProgram(CommonProto.Program.newBuilder()
                        .setId(oldProgram.getId())
                        .setDisplayName(oldProgram.getDisplayName())
                        .build())
                .setPostProgram(CommonProto.Program.newBuilder()
                        .setId(newProgram.getId())
                        .setDisplayName(newProgram.getDisplayName())
                        .build())
                .build());
    }
}
