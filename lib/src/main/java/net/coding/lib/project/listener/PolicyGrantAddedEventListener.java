package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.platform.ram.event.policy.grant.PolicyGrantAddedEvent;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;
import net.coding.platform.ram.service.PolicyRemoteService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyGrantAddedEventListener {

    private final ProjectMemberDao projectMemberDao;

    private final PolicyRemoteService policyRemoteService;

    @Subscribe
    @Transactional
    public void handle(PolicyGrantAddedEvent event) {

        PolicyResponseDTO policyDTO = policyRemoteService.getPolicy(event.getPolicyId());

/*        List<ProjectMember> members = projectMemberDao.findPrincipalMembers(
                event.getContextInfo().getTenantId().intValue(),
                ProjectMemberPrincipalTypeEnum.valueOf(event.getGrantScope()).name(),
                event.getGrantObjectId());*/
    }
}
