package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.member.ProjectMemberPrincipalWriteService;
import net.coding.platform.ram.event.policy.grant.PolicyGrantAddedEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyGrantAddedEventListener {

    private final ProjectDao projectDao;

    private final ProjectMemberDao projectMemberDao;

    private final ProjectMemberPrincipalWriteService projectMemberPrincipalWriteService;

    /**
     * 权限组添加事件，如主体成员数据不在项目/项目集内，则添加
     */
    @Subscribe
    @Transactional
    public void handle(PolicyGrantAddedEvent event) throws CoreException {
        Integer teamId = event.getContextInfo().getTenantId().intValue();
        Integer operatorUserId = event.getContextInfo().getUserId().intValue();
        //只处理项目/项目集
        if (!Objects.equals(PmTypeEnums.PROJECT.name().toLowerCase(), event.getResourceType())
                && !Objects.equals(PmTypeEnums.PROGRAM.name().toLowerCase(), event.getResourceType())) {
            return;
        }
        log.info("PolicyGrantAddedEvent, teamId = {}, operator = {}, projectId = {} ",
                teamId,
                operatorUserId,
                event.getResourceId());
        Project project = projectDao.getProjectByIdAndTeamId(Integer.valueOf(event.getResourceId()), teamId);
        if (Objects.isNull(project)) {
            return;
        }
        ProjectMember principalMember = projectMemberDao.getPrincipalMember(
                Integer.valueOf(event.getResourceId()),
                event.getGrantScope(),
                event.getGrantObjectId(),
                BeanUtils.getDefaultDeletedAt());
        if (Objects.nonNull(principalMember)) {
            return;
        }
        if (Objects.equals(ProjectMemberPrincipalTypeEnum.USER.name(), event.getGrantScope())) {
            ProjectMember member = projectMemberDao.getByProjectIdAndUserId(
                    Integer.valueOf(event.getResourceId()),
                    Integer.valueOf(event.getGrantObjectId()),
                    BeanUtils.getDefaultDeletedAt());
            if (Objects.nonNull(member)) {
                return;
            }
        }
        projectMemberPrincipalWriteService.addMember(teamId,
                operatorUserId,
                Integer.valueOf(event.getResourceId()),
                Collections.singletonList(ProjectMemberAddReqDTO.builder()
                        .principalType(ProjectMemberPrincipalTypeEnum.valueOf(event.getGrantScope()))
                        .principalId(event.getGrantObjectId())
                        .build()));
    }
}
