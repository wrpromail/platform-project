package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ActionEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberPrincipalUserEventService;
import net.coding.platform.ram.event.group.user.GroupUserAddedEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

/**
 * 用户组成员添加事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupUserAddedEventListener {

    private final ProjectMemberDao projectMemberDao;

    private final ProjectMemberPrincipalUserEventService projectMemberPrincipalUserEventService;

    @Subscribe
    @Transactional
    public void handle(GroupUserAddedEvent event) {
        try {
            Integer teamId = event.getContextInfo().getTenantId().intValue();
            Integer operatorUserId = event.getContextInfo().getUserId().intValue();
            log.info("GroupUserAddedEvent, teamId = {}, operator = {}, userGroupId = {}",
                    teamId,
                    operatorUserId,
                    event.getGroupId());
            List<ProjectMember> principalMembers = projectMemberDao.findPrincipalMembers(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(event.getContextInfo().getTenantId().intValue())
                            .principalType(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())
                            .principalIds(StreamEx.of(event.getGroupId()).map(String::valueOf).toSet())
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .build()
            )
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(principalMembers)) {
                return;
            }
            projectMemberPrincipalUserEventService.sendMemberUserEvent(
                    teamId,
                    operatorUserId,
                    principalMembers,
                    StreamEx.of(event.getUserId().intValue()).toList(),
                    ActionEnums.ADDED);
        } catch (Exception e) {
            log.error("GroupUserAddedEvent Error, teamId = {}, groupId = {}, userId = {}, message = {}",
                    event.getContextInfo().getTenantId(),
                    event.getGroupId(),
                    event.getUserId(),
                    e.getMessage());
        }
    }
}
