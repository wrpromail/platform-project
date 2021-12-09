package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ActionEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.event.DepartmentTypeChangeEvent;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberPrincipalUserEventService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 部门类型切换事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentTypeChangeEventListener {

    private final ProjectMemberDao projectMemberDao;

    private final ProjectMemberPrincipalUserEventService projectMemberPrincipalUserEventService;

    @Subscribe
    @Transactional
    public void handle(DepartmentTypeChangeEvent event) {
        try {
            log.info("DepartmentTypeChangeEvent, teamId = {}, before = {}, now = {}",
                    event.getTeamId(),
                    event.getBefore(),
                    event.getNow());

            List<ProjectMember> principalMembers = projectMemberDao.findPrincipalMembers(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(event.getTeamId())
                            .principalType(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .archivedAt(BeanUtils.getDefaultArchivedAt())
                            .build()
            )
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(principalMembers)) {
                return;
            }
            projectMemberDao.batchDelete(principalMembers);
            projectMemberPrincipalUserEventService.sendMemberUserEvent(
                    event.getTeamId(),
                    event.getOperator(),
                    principalMembers,
                    event.getUserIds(),
                    ActionEnums.DELETED);
        } catch (Exception e) {
            log.error("DepartmentTypeChangeEvent Error, teamId = {}",
                    event.getTeamId());
        }
    }
}
