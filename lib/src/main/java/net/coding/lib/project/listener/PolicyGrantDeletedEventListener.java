package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.lib.project.service.ProjectService;
import net.coding.platform.ram.event.policy.grant.PolicyGrantDeletedEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyGrantDeletedEventListener {

    private final ProjectService projectService;

    @Subscribe
    @Transactional
    public void handle(PolicyGrantDeletedEvent event) {
/*        Project project = projectService.getById(event.getProjectId());
        if (Objects.isNull(project)) {
            log.info("ProjectMemberRoleChangeEventListener Project is null, projectId = {}", event.getProjectId());
            return;
        }*/
    }
}
