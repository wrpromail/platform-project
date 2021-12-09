package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ActionEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.event.DepartmentChangeEvent;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberAdaptorFactory;
import net.coding.lib.project.service.member.ProjectMemberPrincipalUserEventService;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static net.coding.lib.project.event.DepartmentChangeEvent.ChangeType.ADDED;
import static net.coding.lib.project.event.DepartmentChangeEvent.ChangeType.DELETED;

/**
 * 部门删除事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentChangeEventListener {

    private final ProjectDao projectDao;

    private final ProjectMemberDao projectMemberDao;

    private final ProjectMemberAdaptorFactory projectMemberAdaptorFactory;
    private final ProjectMemberPrincipalUserEventService projectMemberPrincipalUserEventService;

    @Subscribe
    @Transactional
    public void handle(DepartmentChangeEvent event) {
        try {
            log.info("DepartmentChangeEvent, teamId = {}, describeId = {}, changeType = {}",
                    event.getTeamId(),
                    event.getDescribeId(),
                    event.getChangeType().name());
            List<ProjectMember> principalMembers = projectMemberDao.findPrincipalMembers(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(event.getTeamId())
                            .principalType(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())
                            .principalIds(StreamEx.of(event.getDescribeId()).toSet())
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
            //部门删除
            if (event.getChangeType().equals(DELETED)) {
                projectMemberDao.batchDelete(principalMembers);
            }
            if (CollectionUtils.isEmpty(event.getUserChanges())) {
                return;
            }
            StreamEx.of(event.getUserChanges())
                    .groupingBy(DepartmentChangeEvent.DepartmentUserChange::getChangeType)
                    .forEach((k, v) -> {
                        List<Integer> userIds = StreamEx.of(v)
                                .map(DepartmentChangeEvent.DepartmentUserChange::getUserId)
                                .toList();
                        ActionEnums actionEnums = null;
                        if (ADDED.equals(k)) {
                            actionEnums = ActionEnums.ADDED;
                        }
                        if (DELETED.equals(k)) {
                            actionEnums = ActionEnums.DELETED;
                        }
                        projectMemberPrincipalUserEventService.sendMemberUserEvent(
                                event.getTeamId(),
                                event.getOperator(),
                                principalMembers,
                                userIds,
                                actionEnums);
                    });
        } catch (Exception e) {
            log.info("DepartmentChangeEvent, teamId = {}, describeId = {}, changeType = {}",
                    event.getTeamId(),
                    event.getDescribeId(),
                    event.getChangeType().name(),
                    e);
        }
    }
}
