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
import org.springframework.transaction.annotation.Transactional;
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
     * 项目内添加/校验成员数据
     */
    @Transactional
    public void addMember(
            Integer teamId,
            Integer currentUserId,
            Integer projectId,
            List<ProjectMemberAddReqDTO> reqDTOs
    ) throws CoreException {
        //校验添加数据团队内是否存在
        List<ProjectMemberAddReqDTO> projectMemberAddReqDTOS = projectMemberFilterService.checkAddProjectMember(teamId, currentUserId, reqDTOs);
        doAddMember(teamId, currentUserId, projectId, projectMemberAddReqDTOS);
    }

    /**
     * 批量添加
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
        //校验添加数据团队内是否存在
        List<ProjectMemberAddReqDTO> memberAddReqDTOS = projectMemberFilterService.checkAddProjectMember(teamId, currentUserId, reqDTOs);
        StreamEx.of(reqDTO.getProjects())
                .forEach(dto -> {
                    List<ProjectMemberAddReqDTO> addReqDTOs = StreamEx.of(memberAddReqDTOS)
                            .peek(addReqDTO -> addReqDTO.setPolicyIds(dto.getPolicyIds()))
                            .toList();
                    transactionTemplate.execute(status -> {
                        try {
                            doAddMember(teamId, currentUserId, dto.getProjectId(), addReqDTOs);
                        } catch (CoreException e) {
                            log.error("Project is null, projectId = {}", dto.getProjectId());
                        }
                        return TRUE;
                    });
                });
    }

    /**
     * 项目内通用添加
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
        //项目内成员
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        List<ProjectMember> addMembers = StreamEx.of(reqDTOs)
                .map(reqDTO -> {
                    //项目内是否存在
                    boolean existMember = StreamEx.of(members)
                            .anyMatch(member -> member.getPrincipalType().equals(reqDTO.getPrincipalType().name())
                                    && member.getPrincipalId().equals(reqDTO.getPrincipalId()));
                    //存在则过滤
                    if (existMember) {
                        return null;
                    }
                    StreamEx.of(reqDTO.getPolicyIds())
                            .forEach(policyId ->
                                    grantInfoDTOS.add(new GrantDTO()
                                            .setGrantScope(reqDTO.getPrincipalType().name().toLowerCase())
                                            .setGrantObjectId(reqDTO.getPrincipalId())
                                            .setPolicyId(policyId)
                                            .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
                                            .setResourceId(String.valueOf(project.getId())))
                            );
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
        if (!CollectionUtils.isEmpty(addMembers)
                && !CollectionUtils.isEmpty(grantInfoDTOS)) {
            projectMemberDao.batchInsert(addMembers);
            projectMemberInspectService.attachGrant(currentUserId, grantInfoDTOS);
            projectMemberAdaptorFactory.create(project.getPmType())
                    .postAddMembersEvent(project, currentUserId, addMembers, members);
        }
    }

    @Transactional
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
        List<ProjectMember> delMembers = StreamEx.of(principals)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(principal ->
                                principal.getPrincipalType() + ":" + principal.getPrincipalId()))),
                        ArrayList::new))
                .stream()
                .flatMap(principal -> StreamEx.of(members)
                        .filter(member -> member.getPrincipalType().equals(principal.getPrincipalType().name())
                                && member.getPrincipalId().equals(principal.getPrincipalId())))
                .collect(toList());
        delMembers = projectMemberAdaptorFactory.create(project.getPmType())
                .filterProjectMemberRoleType(currentUserId, project, delMembers);
        if (!CollectionUtils.isEmpty(delMembers)) {
            projectMemberDao.batchDelete(delMembers);
            projectMemberInspectService.removeResourceGrant(currentUserId, project, delMembers);
            projectMemberAdaptorFactory.create(project.getPmType())
                    .postDeleteMemberEvent(project, currentUserId, delMembers);
        }
    }

    public void batchDelMember(Integer teamId,
                               Integer currentUserId,
                               ProjectMemberBatchDelReqDTO reqDTO) {
        StreamEx.of(reqDTO.getProjectIds())
                .forEach(projectId -> transactionTemplate.execute(status -> {
                    try {
                        delMember(teamId, currentUserId, projectId, reqDTO.getPrincipals());
                    } catch (CoreException e) {
                        log.error("Project is null, projectId = {}", projectId);
                    }
                    return TRUE;
                }));
    }

    @Transactional
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
        projectMemberDao.batchDelete(members);
        projectMemberInspectService.removeResourceGrant(currentUserId, project, members);
        projectMemberAdaptorFactory.create(project.getPmType())
                .postMemberQuitEvent(currentUserId, project, members);
    }
}
