package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProjectMemberDeleteEvent;
import net.coding.common.base.gson.JSON;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.lib.project.service.ProgramMemberService;
import net.coding.lib.project.service.ProjectService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMemberDeleteEventListener {

    private final ProgramDao programDao;

    private final ProjectService projectService;

    private final ProgramMemberService programMemberService;

    @Subscribe
    @Transactional
    public void handle(ProjectMemberDeleteEvent event) {
    /*    try {
            log.info("ProjectMemberDeleteEvent , event :{}", JSON.toJson(event));
            Project project = projectService.getById(event.getProjectId());
            if (Objects.isNull(project)) {
                log.info("ProjectMemberDeleteEvent Project is null, projectId = {}", event.getProjectId());
                return;
            }
            if (Objects.equals(project.getPmType(), PmTypeEnums.PROGRAM.getType())) {
                log.info("ProjectMemberDeleteEvent Project pmType , pmType = {}", project.getPmType());
                return;
            }
            programDao.selectPrograms(ProgramQueryParameter.builder()
                    .teamId(project.getTeamOwnerId())
                    .projectId(event.getProjectId())
                    .userId(event.getUserId())
                    .build()
            ).forEach(program -> {
                //项目集下其他项目成员中所在项目
                boolean isExist = StreamEx.of(programDao.selectProgramProjects(
                                ProgramProjectQueryParameter.builder()
                                        .teamId(program.getTeamOwnerId())
                                        .programId(program.getId())
                                        .userId(event.getUserId())
                                        .build()))
                        .anyMatch(p -> !p.getId().equals(project.getId()));
                if (isExist) {
                    return;
                }
                programMemberService.delMember(program.getTeamOwnerId(), program, event.getUserId());
            });
        } catch (Exception ex) {
            log.error("ProjectMemberDeleteEvent Exception Error, projectId = {}, targetUserId = {}",
                    event.getProjectId(),
                    event.getUserId());
        }*/
    }
}
