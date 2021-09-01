package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProjectMemberCreateEvent;
import net.coding.common.base.gson.JSON;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.enums.ProgramProjectRoleTypeEnum.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMemberCreateEventListener {
    private final ProjectService projectService;

    private final ProgramDao programDao;

    private final ProjectMemberService projectMemberService;

    @Subscribe
    @Transactional
    public void handle(ProjectMemberCreateEvent event) {
        try {
            log.info("ProjectMemberCreateEvent , event :{}", JSON.toJson(event));
            Project project = projectService.getById(event.getProjectId());
            if (Objects.isNull(project)) {
                log.info("ProjectMemberCreateEvent Project is null, projectId = {}", event.getProjectId());
                return;
            }
            if (Objects.equals(project.getPmType(), PmTypeEnums.PROGRAM.getType())) {
                log.info("ProjectMemberCreateEvent Project pmType , pmType = {}", project.getPmType());
                return;
            }
            programDao.selectPrograms(ProgramQueryParameter.builder()
                    .teamId(project.getTeamOwnerId())
                    .projectId(project.getId())
                    .build()
            ).forEach(program -> projectMemberService.findListByProjectId(program.getId())
                    .stream()
                    .filter(pm -> pm.getType().equals(ProgramRoleTypeEnum.ProgramOwner.getCode()))
                    .map(ProjectMember::getUserId)
                    .findFirst()
                    .ifPresent(currentUserId -> {
                        try {
                            projectMemberService.doAddMember(currentUserId,
                                    Stream.of(event.getUserId()).collect(Collectors.toList()),
                                    ProgramRoleTypeEnum.ProgramProjectMember.getCode(),
                                    program,
                                    false);
                        } catch (CoreException e) {
                            log.info("ProjectMemberCreateEvent doAddMember Error, projectId = {}, userId = {}",
                                    event.getProjectId(),
                                    event.getUserId());
                        }
                    }));
        } catch (Exception ex) {
            log.error("ProjectMemberCreateEvent Error, projectId = {}, userId = {}",
                    event.getProjectId(),
                    event.getUserId());
        }
    }
}
