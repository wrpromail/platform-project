package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.e.grpcClient.collaboration.IssueWorkflowGrpcClient;
import net.coding.exchange.dto.team.Team;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.dao.ProgramProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dao.TeamProjectDao;
import net.coding.lib.project.dto.ProgramDTO;
import net.coding.lib.project.dto.ProgramPathDTO;
import net.coding.lib.project.dto.ProgramUserDTO;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.dto.ProjectUserDTO;
import net.coding.lib.project.entity.ProgramProject;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.TeamProject;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProgramProjectEventEnums;
import net.coding.lib.project.enums.ProgramWorkflowEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.CreateProgramForm;
import net.coding.lib.project.form.QueryProgramForm;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.infra.PinyinService;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramPageQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;
import net.coding.lib.project.service.project.adaptor.ProjectAdaptorFactory;
import net.coding.lib.project.utils.DateUtil;
import net.coding.platform.permission.proto.CommonProto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.user.UserProto;

import static net.coding.common.base.bean.ProjectTweet.ACTION_CREATE;
import static net.coding.common.constants.RoleConstants.OWNER;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.ACTION.ACTION_VIEW;
import static net.coding.lib.project.enums.ProgramProjectRoleTypeEnum.*;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PERMISSION_DENIED;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_CREATION_ERROR;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_DISPLAY_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NAME_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_MEMBER_NOT_EXISTS;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;

@Service
@Slf4j
@AllArgsConstructor
public class ProgramService {

    private final TeamServiceGrpcClient teamServiceGrpcClient;

    private final UserGrpcClient userGrpcClient;

    private final TransactionTemplate transactionTemplate;

    private final ProjectServiceHelper projectServiceHelper;

    private final ProjectService projectService;

    private final ProjectMemberService projectMemberService;

    private final ProjectValidateService projectValidateService;

    private final ProjectPreferenceService projectPreferenceService;

    private final ProgramDao programDao;

    private final ProgramProjectDao programProjectDao;

    private final TeamProjectDao teamProjectDao;

    private final ProjectMemberDao projectMemberDao;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final IssueWorkflowGrpcClient issueWorkflowGrpcClient;

    private final ProjectAdaptorFactory projectAdaptorFactory;

    private final ProjectDTOService projectDTOService;

    private final PinyinService pinyinService;

    public ProgramPathDTO createProgram(Integer currentTeamId, Integer currentUserId, CreateProgramForm form) throws Exception {
        Team team = teamServiceGrpcClient.getTeam(currentTeamId);
        if (Objects.isNull(team)) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (!teamServiceGrpcClient.isMember(team.getId(), currentUserId)) {
            throw CoreException.of(TEAM_MEMBER_NOT_EXISTS);
        }
        if (Objects.nonNull(projectService.getByNameAndTeamId(form.getName(), team.getId()))) {
            throw CoreException.of(PROJECT_NAME_EXISTS);
        }
        if (Objects.nonNull(projectService.getByDisplayNameAndTeamId(form.getDisplayName(), team.getId()))) {
            throw CoreException.of(PROJECT_DISPLAY_NAME_EXISTS);
        }
        if (form.getProgramWorkflow() == ProgramWorkflowEnums.PROGRAM) {
            Project program = programDao.selectByIdAndTeamId(form.getWorkflowProgramId(), currentTeamId);
            if (Objects.isNull(program)) {
                throw CoreException.of(RESOURCE_NO_FOUND);
            }
        }

        Project program = transactionTemplate.execute(status -> {
            int r = RandomUtils.nextInt(14) + 1;
            String icon = StringUtils.defaultIfBlank(form.getIcon(),
                    "/static/project_icon/scenery-version-2-" + r + ".svg");
            Project insertProgram = Project.builder()
                    .teamOwnerId(team.getId())
                    .name(form.getName().replace(" ", "-"))
                    .displayName(form.getDisplayName())
                    .namePinyin(pinyinService.getPinYin(
                            form.getDisplayName(),
                            form.getName()))
                    .icon(icon)
                    .description(form.getDescription())
                    .startDate(projectValidateService.getStartDate(form.getStartDate()))
                    .endDate(projectValidateService.getEndDate(form.getEndDate()))
                    .pmType(PmTypeEnums.PROGRAM.getType())
                    .build();
            programDao.insertSelective(insertProgram);
            teamProjectDao.insertSelective(TeamProject.builder()
                    .projectId(insertProgram.getId())
                    .teamId(team.getId())
                    .createdAt(DateUtil.getCurrentDate())
                    .updatedAt(DateUtil.getCurrentDate())
                    .deletedAt(DateUtil.strToDate(BeanUtils.NOT_DELETED_AT))
                    .build());
            return insertProgram;
        });
        if (Objects.isNull(program)) {
            throw CoreException.of(PROJECT_CREATION_ERROR);
        }
        //发通知
        projectServiceHelper.sendCreateProjectNotification(team.getOwner_id(), currentUserId,
                program, ProgramProjectEventEnums.createProgram);
        //初始化项目集权限
        advancedRoleServiceGrpcClient.initPredefinedRoles(team.getId(), CommonProto.TargetType.PROGRAM,
                Stream.of(program.getId().longValue()).collect(Collectors.toSet()));
        //初始化项目协同
        initProgramWorkflow(currentTeamId, currentUserId, program.getId(),
                form.getProgramWorkflow(), ObjectUtils.defaultIfNull(form.getWorkflowProgramId(), 0));
        //添加成员
        projectMemberService.doAddMember(currentUserId, Collections.singletonList(currentUserId),
                ProgramRoleTypeEnum.ProgramOwner.getCode(), program, false);
        // 创建项目默认的偏好设置 由于创建项目较慢这里 7 次 insert 把这个转移到异步
        projectPreferenceService.initProjectPreferences(program.getId());

        projectAdaptorFactory.create(program.getPmType())
                .postProjectCreateEvent(currentUserId, program, ACTION_CREATE);

        return ProgramPathDTO.builder()
                .id(program.getId())
                .path(getProgramPath(program))
                .build();
    }

    public ProgramPathDTO addProgramProject(Integer currentTeamId, Integer currentUserId,
                                            Integer programId, Set<Integer> projectIds,
                                            Set<Integer> adminIds) throws Exception {
        Project program = programDao.selectByIdAndTeamId(programId, currentTeamId);
        if (Objects.isNull(program)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        if (CollectionUtils.isNotEmpty(adminIds)) {
            List<UserProto.User> users = userGrpcClient.findUserByIds(new ArrayList<>(adminIds));
            if (CollectionUtils.isNotEmpty(users)) {
                Set<Integer> targetUserIds = users.stream()
                        .filter(user -> user.getTeamId() == currentTeamId)
                        .map(UserProto.User::getId)
                        .collect(Collectors.toSet());
                projectMemberService.doAddMember(currentUserId, new ArrayList<>(targetUserIds),
                        ProgramRoleTypeEnum.ProgramAdmin.getCode(), program, false);
            }
        }
        if (CollectionUtils.isNotEmpty(projectIds)) {
            Set<Integer> targetUserIds = new HashSet<>();
            //我参与的项目
            List<Integer> joinedProjectIds =
                    StreamEx.of(projectService.getJoinedProjects(currentTeamId, currentUserId))
                            .map(Project::getId)
                            .collect(Collectors.toList());
            //项目集中已存在项目
            List<Integer> programProjectIds =
                    StreamEx.of(programProjectDao.select(ProgramProject.builder()
                            .programId(program.getId())
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .build())
                    )
                            .map(ProgramProject::getProjectId)
                            .collect(Collectors.toList());
            StreamEx.of(projectIds)
                    .filter(joinedProjectIds::contains)
                    .filter(projectId -> !programProjectIds.contains(projectId))
                    .forEach(projectId -> {
                        projectMemberService.findListByProjectId(projectId)
                                .forEach(pm -> targetUserIds.add(pm.getUserId()));
                        programProjectDao.insertSelective(ProgramProject.builder()
                                .programId(program.getId())
                                .projectId(projectId)
                                .createdAt(new Timestamp(System.currentTimeMillis()))
                                .updatedAt(new Timestamp(System.currentTimeMillis()))
                                .deletedAt(BeanUtils.getDefaultDeletedAt())
                                .build());
                    });
            if (CollectionUtils.isNotEmpty(targetUserIds)) {
                projectMemberService.doAddMember(currentUserId, new ArrayList<>(targetUserIds),
                        ProgramRoleTypeEnum.ProgramProjectMember.getCode(), program, false);
            }
        }
        return ProgramPathDTO.builder()
                .id(program.getId())
                .path(getProgramPath(program))
                .build();
    }

    public ResultPage<ProgramDTO> getProgramPages(ProgramPageQueryParameter parameter) throws CoreException {
        if (parameter.getQueryType().equals(QueryProgramForm.QueryType.ALL.name())) {
            projectAdaptorFactory.create(PmTypeEnums.PROGRAM.getType())
                    .hasPermissionInEnterprise(
                            parameter.getTeamId(),
                            parameter.getUserId(),
                            PmTypeEnums.PROGRAM.getType(),
                            ACTION_VIEW);
        }
        PageInfo<ProgramDTO> pageInfo = PageHelper.startPage(parameter.getPage(), parameter.getPageSize())
                .doSelectPageInfo(() -> programDao.selectProgramPages(parameter));
        List<ProgramDTO> programDTOList = pageInfo.getList().stream()
                .peek(p -> {
                    p.setProgramUser(toProgramUserDTO(p.getId()));
                    p.setProjects(toProjectDTO(parameter.getTeamId(), p.getId()));
                })
                .collect(Collectors.toList());
        return new ResultPage<>(programDTOList, parameter.getPage(), parameter.getPageSize(), pageInfo.getTotal());
    }

    public List<ProgramDTO> getProgramDTOs(Integer teamId, Integer projectId, Integer userId) throws CoreException {
        return getPrograms(teamId, projectId, userId)
                .stream()
                .map(this::toProgramDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Project> getPrograms(Integer teamId, Integer projectId, Integer userId) throws CoreException {
        ProgramQueryParameter parameter = ProgramQueryParameter.builder()
                .teamId(teamId)
                .projectId(projectId)
                .build();
        if (userId != null && userId > 0) {
            boolean hasEnterprisePermission = projectAdaptorFactory.create(PmTypeEnums.PROGRAM.getType())
                    .hasEnterprisePermission(teamId, userId, PmTypeEnums.PROGRAM.getType(), ACTION_VIEW);
            if (!hasEnterprisePermission) {
                parameter.setUserId(userId);
            }
        }
        return programDao.selectPrograms(parameter);
    }

    public Project getProgram(Integer teamId, Integer userId, Integer programId) throws CoreException {
        Project program = programDao.selectByIdAndTeamId(programId, teamId);
        if (Objects.isNull(program)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        if (Objects.isNull(projectMemberService.getByProjectIdAndUserId(program.getId(), userId))) {
            projectAdaptorFactory.create(program.getPmType())
                    .hasPermissionInEnterprise(teamId, userId, program.getPmType(), ACTION_VIEW);
        }
        return program;
    }

    public List<ProjectDTO> getProgramAllProjects(ProgramProjectQueryParameter parameter) throws CoreException {
        if (parameter.getQueryType().equals(QueryProgramForm.QueryType.ALL.name())) {
            projectAdaptorFactory.create(PmTypeEnums.PROGRAM.getType())
                    .hasPermissionInEnterprise(
                            parameter.getTeamId(),
                            parameter.getUserId(),
                            PmTypeEnums.PROGRAM.getType(),
                            ACTION_VIEW);
        }
        return StreamEx.of(programDao.selectProgramAllProjects(parameter))
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
    }

    public List<ProjectDTO> getProgramProjectDTOs(Integer currentTeamId, Integer currentUserId,
                                                  Integer programId, Boolean queryJoined) throws CoreException {
        getProgram(currentTeamId, currentUserId, programId);
        return StreamEx.of(
                getProgramProjects(ProgramProjectQueryParameter.builder()
                        .teamId(currentTeamId)
                        .programId(programId)
                        .userId(queryJoined ? currentUserId : 0)
                        .build())
        )
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .peek(p -> p.setMemberCount(projectMemberDao.findListByProjectId(p.getId(), p.getDeleted_at()).size()))
                .collect(Collectors.toList());
    }

    public List<Project> getProgramProjects(ProgramProjectQueryParameter parameter) {
        return programDao.selectProgramProjects(parameter);
    }

    public List<ProjectUserDTO> getBatchUserProgramProjects(Integer currentTeamId, Integer currentUserId,
                                                            Integer programId, Set<Integer> userIds) throws CoreException {
        if (!Optional.ofNullable(projectMemberService.getByProjectIdAndUserId(programId, currentUserId))
                .isPresent()) {
            throw CoreException.of(PERMISSION_DENIED);
        }
        return StreamEx.of(userIds)
                .map(userId -> {
                    List<ProjectDTO> projects = StreamEx.of(
                            programDao.selectProgramProjects(ProgramProjectQueryParameter.builder()
                                    .teamId(currentTeamId)
                                    .userId(userId)
                                    .programId(programId).build())
                    )
                            .map(projectDTOService::toDetailDTO)
                            .nonNull()
                            .collect(Collectors.toList());
                    return ProjectUserDTO.builder()
                            .userId(userId)
                            .projects(projects)
                            .build();

                }).nonNull()
                .collect(Collectors.toList());
    }

    private List<ProgramUserDTO> toProgramUserDTO(Integer programId) {
        return StreamEx.of(projectMemberService.findListByProjectId(programId))
                .filter(m -> Objects.equals(m.getType(), OWNER))
                .map(m -> Optional.ofNullable(userGrpcClient.getUserById(m.getUserId()))
                        .map(u -> ProgramUserDTO.builder()
                                .id(u.getId())
                                .name(u.getName())
                                .avatar(u.getAvatar())
                                .build())
                        .orElse(null))
                .nonNull()
                .collect(Collectors.toList());
    }

    private List<ProjectDTO> toProjectDTO(Integer teamId, Integer programId) {
        return StreamEx.of(
                programDao.selectProgramProjects(ProgramProjectQueryParameter.builder()
                        .teamId(teamId)
                        .programId(programId)
                        .build())
        )
                .map(projectDTOService::toDetailDTO)
                .nonNull()
                .collect(Collectors.toList());
    }

    private ProgramDTO toProgramDTO(Project program) {
        if (program == null) {
            return null;
        }
        return ProgramDTO.builder()
                .id(Optional.ofNullable(program.getId()).orElse(0))
                .name(program.getName())
                .displayName(program.getDisplayName())
                .description(TextUtils.htmlEscape(program.getDescription()))
                .icon(program.getIcon())
                .startDate(program.getStartDate())
                .endDate(program.getEndDate())
                .build();
    }

    public void initProgramWorkflow(Integer currentTeamId, Integer currentUserId, Integer programId,
                                    ProgramWorkflowEnums programWorkflow, Integer workflowProgramId) {
        try {
            issueWorkflowGrpcClient.initProgramWorkflow(currentTeamId, programId, currentUserId,
                    programWorkflow.name(), ObjectUtils.defaultIfNull(workflowProgramId, 0));
        } catch (Exception ex) {
            log.error("initProgramWorkflow Error , programId = {}, workflowProgramId = {}", programId, workflowProgramId, ex);
        }
    }


    public String getProgramPath(Project program) {
        return "/p/" + program.getName();
    }
}
