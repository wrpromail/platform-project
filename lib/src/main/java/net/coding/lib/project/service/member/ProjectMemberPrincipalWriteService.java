package net.coding.lib.project.service.member;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberBatchAddReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberBatchDelReqDTO;
import net.coding.lib.project.dto.request.ProjectMemberReqDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.platform.ram.pojo.dto.GrantDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;
import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberPrincipalWriteService {

    private final ProjectDao projectDao;

    private final ProjectMemberDao projectMemberDao;

    private final TransactionTemplate transactionTemplate;

    private final ProjectMemberFilterService projectMemberFilterService;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProjectMemberAdaptorFactory projectMemberAdaptorFactory;

    /**
     * ???????????????/??????????????????
     */
    public void addMember(
            Integer teamId,
            Integer currentUserId,
            Integer projectId,
            List<ProjectMemberAddReqDTO> reqDTOs
    ) throws CoreException {
        //???????????????????????????????????????
        List<ProjectMemberAddReqDTO> projectMemberAddReqDTOS = projectMemberFilterService.checkAddProjectMember(teamId, currentUserId, reqDTOs);
        doAddMember(teamId, currentUserId, projectId, projectMemberAddReqDTOS);
    }

    /**
     * ????????????
     */
    public void batchAddMember(
            Integer teamId,
            Integer currentUserId,
            ProjectMemberBatchAddReqDTO reqDTO
    ) {
        List<ProjectMemberAddReqDTO> reqDTOs = StreamEx.of(reqDTO.getPrincipals())
                .map(dto -> ProjectMemberAddReqDTO.builder()
                        .principalType(dto.getPrincipalType())
                        .principalId(dto.getPrincipalId())
                        .build())
                .toList();
        //???????????????????????????????????????
        List<ProjectMemberAddReqDTO> memberAddReqDTOS = projectMemberFilterService.checkAddProjectMember(teamId, currentUserId, reqDTOs);
        StreamEx.of(reqDTO.getProjects())
                .forEach(dto -> {
                    List<ProjectMemberAddReqDTO> addReqDTOs = StreamEx.of(memberAddReqDTOS)
                            .peek(addReqDTO -> addReqDTO.setPolicyIds(dto.getPolicyIds()))
                            .toList();
                    try {
                        doAddMember(teamId, currentUserId, dto.getProjectId(), addReqDTOs);
                    } catch (CoreException e) {
                        log.error(
                                "Project {} add member failure, cause of {}",
                                dto.getProjectId(),
                                e.getMessage()
                        );
                    }
                });
    }

    /**
     * ?????????????????????
     */
    public void doAddMember(
            Integer teamId,
            Integer currentUserId,
            Integer projectId,
            List<ProjectMemberAddReqDTO> reqDTOs) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        List<GrantDTO> grantInfoDTOS = new ArrayList<>();
        //???????????????
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        List<ProjectMember> addMembers = StreamEx.of(reqDTOs)
                .map(reqDTO -> {
                    //?????????????????????
                    boolean existMember = StreamEx.of(members)
                            .anyMatch(member -> member.getPrincipalType().equals(reqDTO.getPrincipalType().name())
                                    && member.getPrincipalId().equals(reqDTO.getPrincipalId()));
                    //???????????????
                    if (existMember) {
                        return null;
                    }
                    if (!CollectionUtils.isEmpty(reqDTO.getPolicyIds())) {
                        StreamEx.of(reqDTO.getPolicyIds())
                                .forEach(policyId ->
                                        grantInfoDTOS.add(new GrantDTO()
                                                .setGrantScope(reqDTO.getPrincipalType().name().toLowerCase())
                                                .setGrantObjectId(reqDTO.getPrincipalId())
                                                .setPolicyId(policyId)
                                                .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
                                                .setResourceId(String.valueOf(project.getId())))
                                );
                    }

                    return ProjectMember.builder()
                            .projectId(project.getId())
                            .userId(reqDTO.getPrincipalType().equals(ProjectMemberPrincipalTypeEnum.USER)
                                    ? Integer.parseInt(reqDTO.getPrincipalId()) : 0)
                            .principalType(reqDTO.getPrincipalType().name())
                            .principalId(reqDTO.getPrincipalId())
                            .principalSort(reqDTO.getPrincipalType().getSort())
                            .type((short) 0)
                            .alias(EMPTY)
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .createdAt(new Timestamp(System.currentTimeMillis()))
                            .lastVisitAt(new Timestamp(System.currentTimeMillis()))
                            .build();

                })
                .filter(Objects::nonNull)
                .collect(toList());
        if (!CollectionUtils.isEmpty(addMembers)) {
            transactionTemplate.execute(status -> {
                projectMemberDao.batchInsert(addMembers);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        projectMemberAdaptorFactory
                                .create(project.getPmType())
                                .postAddMembersEvent(
                                        project,
                                        currentUserId,
                                        addMembers,
                                        members
                                );
                        log.debug(
                                "Project batch add member send event, teamId = {}, projectId={}",
                                teamId,
                                project.getId()
                        );
                        projectMemberInspectService.attachGrant(currentUserId, grantInfoDTOS);
                    }
                });
                log.debug(
                        "Project batch add member, teamId = {}, projectId={}",
                        teamId,
                        project.getId()
                );
                return TRUE;
            });

        }
    }

    public void delMember(Integer teamId,
                          Integer currentUserId,
                          Integer projectId,
                          List<ProjectMemberReqDTO> principals)
            throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        List<ProjectMember> checkMembers = StreamEx.of(principals)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(principal ->
                                principal.getPrincipalType() + ":" + principal.getPrincipalId()))),
                        ArrayList::new))
                .stream()
                .flatMap(principal -> StreamEx.of(members)
                        .filter(member -> member.getPrincipalType().equals(principal.getPrincipalType().name())
                                && member.getPrincipalId().equals(principal.getPrincipalId())))
                .collect(toList());
        List<ProjectMember> delMembers = projectMemberAdaptorFactory.create(project.getPmType())
                .filterProjectMemberRoleType(currentUserId, project, checkMembers);
        if (!CollectionUtils.isEmpty(delMembers)) {
            transactionTemplate.execute(status -> {
                projectMemberDao.batchDelete(delMembers);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        projectMemberAdaptorFactory
                                .create(project.getPmType())
                                .postDeleteMemberEvent(project, currentUserId, delMembers);
                        log.debug(
                                "Project batch delete member send event, teamId = {}, projectId={}",
                                teamId,
                                project.getId()
                        );
                        projectMemberInspectService.removeResourceGrant(currentUserId, project, delMembers);
                    }
                });
                log.debug("Project batch delete member, teamId = {}, projectId={}",
                        teamId,
                        project.getId()
                );
                return TRUE;
            });
        }
    }

    public void batchDelMember(Integer teamId,
                               Integer currentUserId,
                               ProjectMemberBatchDelReqDTO reqDTO) {
        StreamEx.of(reqDTO.getProjectIds())
                .forEach(projectId -> {
                    try {
                        delMember(teamId, currentUserId, projectId, reqDTO.getPrincipals());
                    } catch (CoreException e) {
                        log.error(
                                "Project {} add member failure, cause of {}",
                                projectId,
                                e.getMessage()
                        );
                    }
                });
    }

    public void quit(Integer teamId, Integer currentUserId, Integer projectId) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        ProjectMember member = projectMemberDao.getByProjectIdAndUserId(
                project.getId(),
                currentUserId,
                BeanUtils.getDefaultDeletedAt());
        if (Objects.isNull(member)) {
            throw CoreException.of(PERMISSION_DENIED);
        }
        List<ProjectMember> members = projectMemberAdaptorFactory.create(project.getPmType())
                .filterProjectMemberRoleType(currentUserId, project, Stream.of(member).collect(toList()));
        if (CollectionUtils.isEmpty(members)) {
            throw CoreException.of(PERMISSION_DENIED);
        }
        transactionTemplate.execute(status -> {
            projectMemberDao.batchDelete(members);
            projectMemberInspectService.removeResourceGrant(currentUserId, project, members);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    projectMemberAdaptorFactory
                            .create(project.getPmType())
                            .postMemberQuitEvent(currentUserId, project, members);
                    log.debug(
                            "Project quit member send Event, teamId = {}, projectId = {}",
                            teamId,
                            project.getId()
                    );
                }
            });
            log.debug(
                    "Project quit member, teamId = {}, projectId={}",
                    teamId,
                    project.getId()
            );
            return TRUE;
        });
    }
}
