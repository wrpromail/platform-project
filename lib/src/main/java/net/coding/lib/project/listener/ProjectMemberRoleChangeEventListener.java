package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProjectMemberRoleChangeEvent;
import net.coding.common.base.gson.JSON;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.service.ProjectMemberService;
import net.coding.lib.project.service.ProjectService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

/**
 * 项目成员权限事件处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMemberRoleChangeEventListener {
    private static Integer DELETE_TOLE = -1;

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final UserGrpcClient userGrpcClient;

    @Subscribe
    @Transactional
    public void handle(ProjectMemberRoleChangeEvent event) {
        try {
            log.info("ProjectMemberRoleChangeEventListener , event :{}", JSON.toJson(event));
            Project project = projectService.getById(event.getProjectId());
            if (Objects.isNull(project)) {
                log.info("ProjectMemberRoleChangeEventListener Project is null, projectId = {}", event.getProjectId());
                return;
            }
            UserProto.User currentUser = userGrpcClient.getUserById(event.getCurrentUserId());
            if (Objects.isNull(currentUser)) {
                log.info("ProjectMemberRoleChangeEventListener CurrentUser is null, currentUserId = {}", event.getCurrentUserId());
                return;
            }
            UserProto.User targetUser = userGrpcClient.getUserById(event.getTargetUserId());
            if (Objects.isNull(targetUser)) {
                log.info("ProjectMemberRoleChangeEventListener TargetUser is null, targetUserId = {}", event.getTargetUserId());
                return;
            }
            if (event.getRoleValue() == 0) {
                log.info("ProjectMemberRoleChangeEventListener RoleValue is null");
                return;
            }
            ProjectMember member = projectMemberService.getByProjectIdAndUserId(event.getProjectId(), event.getTargetUserId());
            if (Objects.nonNull(member)) {
                projectMemberService.updateProjectMemberType
                        (
                                event.getCurrentUserId(),
                                member,
                                project,
                                (short) event.getRoleValue(),
                                event.getRoleId()
                        );
            } else {
                if (event.getOperate() == DELETE_TOLE) {
                    return;
                }
                projectMemberService.doAddMember
                        (
                                event.getCurrentUserId(),
                                Collections.singletonList(event.getTargetUserId()),
                                (short) event.getRoleValue(),
                                project, false
                        );
            }
        } catch (Exception ex) {
            log.error("ProjectMemberRoleChangeEventListener Error, projectId = {}, targetUserId = {}, currentUserId = {}",
                    event.getProjectId(),
                    event.getTargetUserId(),
                    event.getCurrentUserId());
        }
    }
}
