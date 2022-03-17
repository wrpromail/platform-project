package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ProgramEvent;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.grpcClient.collaboration.MilestoneGrpcClient;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
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

@Slf4j
@Service
public class ProgramAdaptorService extends AbstractProjectAdaptorService {
    private final MilestoneGrpcClient milestoneGrpcClient;

    private final EnterpriseGrpcClient enterpriseGrpcClient;

    private final ProgramMemberService programMemberService;

    private final SystemSettingGrpcClient systemSettingGrpcClient;

    public ProgramAdaptorService(AsyncEventBus asyncEventBus, LoggingGrpcClient loggingGrpcClient, TeamGrpcClient teamGrpcClient, UserGrpcClient userGrpcClient, AclServiceGrpcClient aclServiceGrpcClient, LocaleMessageSource localeMessageSource, NotificationGrpcClient notificationGrpcClient, MilestoneGrpcClient milestoneGrpcClient, EnterpriseGrpcClient enterpriseGrpcClient, ProgramMemberService programMemberService, SystemSettingGrpcClient systemSettingGrpcClient) {
        super(asyncEventBus, loggingGrpcClient, teamGrpcClient, userGrpcClient, aclServiceGrpcClient, localeMessageSource, notificationGrpcClient);
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
    }

    @Override
    public void projectArchiveEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.ARCHIVE);
    }

    @Override
    public void projectUnArchiveEvent(Integer userId, Project project) {
        postProgramEvent(userId, project, ProgramEvent.Function.UNARCHIVE);
    }

    @Override
    public void deleteProgramMember(Integer teamId, Integer userId, Project program) {
        //删除事项
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
                //21003 开始时间大于里程碑预估完成时间
                //21004 结束时间小于里程碑预估完成时间
                if (ex.getCode() == 21003 || ex.getCode() == 21004) {
                    throw CoreException.of(PROGRAM_START_AFTER_MILESTONE);
                }
            }
        }
    }

    /**
     * 校验是否开启计费/开启则判断是否是高级版
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
                    .filter(type -> type == TYPE_ADVANCED_PAY)
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
        asyncEventBus.post(ProgramEvent.builder()
                .teamId(project.getTeamOwnerId())
                .userId(userId)
                .projectId(project.getId())
                .function(function)
                .build());
    }
}
