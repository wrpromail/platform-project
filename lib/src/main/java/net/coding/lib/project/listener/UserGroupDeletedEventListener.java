package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ActionEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberPrincipalUserEventService;
import net.coding.platform.ram.event.group.UserGroupDeletedEvent;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserGroupDeletedEventListener {

    private final ProjectMemberDao projectMemberDao;

    private final ProjectMemberPrincipalUserEventService projectMemberPrincipalUserEventService;

    @Subscribe
    @Transactional
    public void handle(UserGroupDeletedEvent event) {
        try {
            Integer teamId = event.getContextInfo().getTenantId().intValue();
            Integer operatorUserId = event.getContextInfo().getUserId().intValue();
            log.info("UserGroupDeletedEvent, teamId = {}, operator = {}, userGroupId = {}",
                    teamId,
                    operatorUserId,
                    event.getUserGroupId());
            List<ProjectMember> principalMembers = projectMemberDao.findPrincipalMembers(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(teamId)
                            .principalType(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())
                            .principalIds(StreamEx.of(event.getUserGroupId()).map(String::valueOf).toSet())
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
                    teamId,
                    operatorUserId,
                    principalMembers,
                    StreamEx.of(event.getUserIds()).map(Long::intValue).toList(),
                    ActionEnums.DELETED);
        } catch (Exception e) {
            log.error("UserGroupDeletedEvent Error, teamId = {}, userGroupId = {}",
                    event.getContextInfo().getTenantId(),
                    event.getUserGroupId());
        }
    }
}
