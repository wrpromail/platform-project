package net.coding.lib.project.service.member;

import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ActionEnums;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

/**
 * 用于主体及成员变更处理事件
 */
@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberPrincipalUserEventService {

    private final ProjectDao projectDao;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProjectMemberAdaptorFactory projectMemberAdaptorFactory;

    /**
     * 用于部门/用户组事件 成员添加移除 发送事件
     */
    public void sendMemberUserEvent(Integer teamId,
                                    Integer currentUserId,
                                    List<ProjectMember> principalMembers,
                                    List<Integer> userIds,
                                    ActionEnums action) {
        principalMembers
                .forEach(member -> {
                    Project project = projectDao.getProjectByIdAndTeamId(
                            member.getProjectId(),
                            teamId);
                    if (Objects.isNull(project)) {
                        return;
                    }
                    List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId())
                            .stream()
                            .filter(m -> !(m.getPrincipalType().equals(member.getPrincipalType())
                                    && m.getPrincipalId().equals(member.getPrincipalId())))
                            .collect(Collectors.toList());
                    Set<Integer> existUserIds = projectMemberInspectService.getPrincipalMemberUserIds(members);
                    List<Integer> memberUserIds = StreamEx.of(userIds)
                            .filter(userId -> !existUserIds.contains(userId))
                            .distinct()
                            .toList();
                    //用户在项目内是否存在
                    if (CollectionUtils.isEmpty(memberUserIds)) {
                        return;
                    }
                    if (ActionEnums.ADDED.equals(action)) {
                        projectMemberAdaptorFactory.create(project.getPmType())
                                .postAddMembersUserEvent(project, currentUserId, memberUserIds);
                    } else if (ActionEnums.DELETED.equals(action)) {
                        projectMemberAdaptorFactory.create(project.getPmType())
                                .postDeleteMemberUserEvent(project, currentUserId, memberUserIds);
                    }
                });
    }
}
