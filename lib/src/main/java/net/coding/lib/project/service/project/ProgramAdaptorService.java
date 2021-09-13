package net.coding.lib.project.service.project;

import com.google.common.eventbus.AsyncEventBus;

import net.coding.common.base.event.ProgramEvent;
import net.coding.common.i18n.utils.LocaleMessageSource;
import net.coding.e.grpcClient.collaboration.MilestoneGrpcClient;
import net.coding.e.grpcClient.collaboration.exception.MilestoneException;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.LoggingGrpcClient;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.service.ProgramMemberService;

import org.springframework.stereotype.Service;


import java.sql.Timestamp;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProgramAdaptorService extends AbstractProjectAdaptorService {
    private final MilestoneGrpcClient milestoneGrpcClient;

    private final ProgramMemberService programMemberService;

    public ProgramAdaptorService(AsyncEventBus asyncEventBus, LoggingGrpcClient loggingGrpcClient, TeamGrpcClient teamGrpcClient, AclServiceGrpcClient aclServiceGrpcClient, LocaleMessageSource localeMessageSource, MilestoneGrpcClient milestoneGrpcClient, ProgramMemberService programMemberService) {
        super(asyncEventBus, loggingGrpcClient, teamGrpcClient, aclServiceGrpcClient, localeMessageSource);
        this.milestoneGrpcClient = milestoneGrpcClient;
        this.programMemberService = programMemberService;
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
    public void deleteProgramMember(Integer teamId, Project project) {
        //删除事项
        programMemberService.removeProgramIssueRelation(project.getId(), 0);
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
                log.info("CheckProgramTime Error , StartDate = {}, EndDate = {}, ProgramId = {}",
                        new Timestamp(program.getStartDate().getTime()),
                        new Timestamp(program.getEndDate().getTime()),
                        program.getId(),
                        ex);
                //21003 开始时间大于里程碑预估完成时间
                //21004 结束时间小于里程碑预估完成时间
                if (ex.getCode() == 21003 || ex.getCode() == 21004) {
                    throw ex;
                }
            }
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