package net.coding.lib.project.service;

import com.google.common.collect.ImmutableList;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.config.CodingSettings;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.ProjectTeamMemberDTO;
import net.coding.lib.project.dto.RoleDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.enums.CacheTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddMemberForm;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.hook.trigger.CreateMemberEventTriggerTrigger;
import net.coding.lib.project.hook.trigger.DeleteMemberEventTriggerTrigger;
import net.coding.lib.project.hook.trigger.UpdateMemberRoleEventTriggerTrigger;
import net.coding.lib.project.pager.ResultPageFactor;
import net.coding.lib.project.service.project.adaptor.ProjectMemberAdaptorFactory;
import net.coding.lib.project.utils.UserUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.advanced_role.AdvancedRoleProto;
import proto.common.CodeProto;
import proto.platform.team.TeamProto;
import proto.platform.user.UserProto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.coding.common.constants.ProjectConstants.PROJECT_PRIVATE;
import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberService {

    public static final Pattern AT_REG = Pattern.compile("(@([^@\\s<>()（）：:，,。…~!！？?'‘\"]+))(.{0}|\\s)");

    private final ProjectMemberDao projectMemberDao;

    private final ProjectDao projectDao;

    private final UserGrpcClient userGrpcClient;

    private final TeamGrpcClient teamGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final ProjectHandCacheService projectHandCacheService;

    private final ProjectMemberAdaptorFactory projectMemberAdaptorFactory;

    private final CreateMemberEventTriggerTrigger createMemberEventTrigger;
    private final DeleteMemberEventTriggerTrigger deleteMemberEventTrigger;
    private final UpdateMemberRoleEventTriggerTrigger updateMemberRoleEventTriggerTrigger;
    private final CodingSettings codingSettings;

    public ProjectMember getById(Integer id) {
        return projectMemberDao.getById(id);
    }

    public int insert(ProjectMember projectMember) {
        return projectMemberDao.insert(projectMember);
    }

    public int update(ProjectMember projectMember) {
        return projectMemberDao.update(projectMember);
    }

    public boolean updateProjectMemberType
            (
                    Integer currentUserId,
                    Integer memberId,
                    Project project,
                    short type,
                    Integer roleId
            ) {
        int result = projectMemberDao.updateProjectMemberType(project.getId(), memberId, type, BeanUtils.getDefaultDeletedAt());
        if (result > 0) {
            ProjectMember projectMember = getByProjectIdAndUserId(project.getId(), memberId);
            updateMemberRoleEventTriggerTrigger.trigger
                    (
                            ImmutableList.of(String.valueOf(roleId)),
                            projectMember,
                            project.getId(),
                            currentUserId
                    );
            projectHandCacheService.handleProjectMemberCache(projectMember, CacheTypeEnum.UPDATE);
        }
        return result > 0;
    }

    public List<ProjectMember> findListByProjectId(Integer projectId) {
        return projectMemberDao.findListByProjectId(projectId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public ProjectMember getByProjectIdAndUserId(Integer projectId, Integer userId) {
        return projectMemberDao.getByProjectIdAndUserId(projectId, userId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public ResultPage<ProjectMemberDTO> getProjectMembers(Integer teamId, Integer projectId, String keyWord,
                                                          Integer roleId, PageRowBounds pager) throws CoreException {
        TeamProto.GetTeamResponse currentTeam = teamGrpcClient.getTeam(teamId);
        if (Objects.isNull(currentTeam) || CodeProto.Code.SUCCESS != currentTeam.getCode()
                || ObjectUtils.isEmpty(currentTeam.getData())) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (Objects.isNull(projectDao.getProjectByIdAndTeamId(projectId, currentTeam.getData().getId()))) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        List<ProjectMember> projectMemberList = projectMemberDao.getProjectMembers(projectId, keyWord, roleId, pager);
        List<ProjectMemberDTO> projectMembers = new ArrayList<>();
        projectMemberList.forEach(projectMember ->
                projectMembers.add(ProjectMemberDTO.builder()
                        .id(projectMember.getId())
                        .project_id(projectMember.getProjectId())
                        .user_id(projectMember.getUserId())
                        .created_at(projectMember.getCreatedAt().getTime())
                        .last_visit_at(projectMember.getLastVisitAt().getTime()).build()));
        if (!CollectionUtils.isEmpty(projectMembers)) {
            projectMembers.forEach(projectMemberDTO -> {
                        UserProto.User user = userGrpcClient.getUserById(projectMemberDTO.getUser_id());
                        projectMemberDTO.setUser(UserUtil.toBuilderUser(user, false)
                        );
                    }
            );
            projectMembers.forEach(projectMemberDTO -> {
                List<AclProto.Role> roles = advancedRoleServiceGrpcClient.findUserRolesInProject(
                        projectMemberDTO.getUser().getId(),
                        projectMemberDTO.getUser().getTeamId(), projectId
                );
                projectMemberDTO.setRoles(toRoleDTO(roles));
            });
        }
        return new ResultPageFactor<ProjectMemberDTO>().def(pager, projectMembers);

    }

    public List<RoleDTO> findMemberCountByProjectId(Integer projectId) {
        List<RoleDTO> roleDTOList = new ArrayList<>();
        try {
            List<AdvancedRoleProto.RoleMemberCount> roleMemberCounts =
                    advancedRoleServiceGrpcClient.findMemberCountByProjectId(projectId);
            roleMemberCounts.forEach(roleMemberCount -> roleDTOList.add(toRoleMemberDTO(roleMemberCount)));
        } catch (Exception e) {
            log.error("advancedRoleServiceGrpcClient findMemberCountByProjectId is error{} ", e.getMessage());
        }
        return roleDTOList;
    }

    public void doAddMember(AddMemberForm addMemberForm, Integer projectId) throws CoreException {
        UserProto.User currentUser = SystemContextHolder.get();
        if (Objects.isNull(SystemContextHolder.get())) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        Project project = projectDao.getProjectById(projectId);
        if (Objects.isNull(project)) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NAME_EXISTS);
        }
        ProjectMember projectMember = getByProjectIdAndUserId(projectId, currentUser.getId());

        projectMemberAdaptorFactory.create(project.getPmType())
                .checkAddProjectMemberType(projectMember);

        List<Integer> targetUserIds = new ArrayList<>();
        Arrays.stream(addMemberForm.getUsers().split(",")).forEach(targetUserIdStr -> {
            UserProto.User user = userGrpcClient.getUserByGlobalKey(targetUserIdStr);
            if (!ObjectUtils.isEmpty(user) && user.getId() != currentUser.getId()) {
                Integer id = user.getId();
                targetUserIds.add(id);
            }
        });
        doAddMember(currentUser.getId(), targetUserIds, addMemberForm.getType(), project, false);
    }

    public void doAddMember(Integer currentUserId, List<Integer> targetUserIds,
                            short type, Project project, boolean isInvite) throws CoreException {
        try {
            Set<Integer> memberUserIds = projectMemberDao.findListByProjectId(
                    project.getId(),
                    Timestamp.valueOf(BeanUtils.NOT_DELETED_AT)
            )
                    .stream()
                    .map(ProjectMember::getUserId)
                    .collect(toSet());

            projectMemberAdaptorFactory.create(project.getPmType())
                    .checkExistProjectMember(memberUserIds, targetUserIds, project, type);

            //可添加成员
            Set<Integer> addUserIds = StreamEx.of(targetUserIds)
                    .filter(targetUserId -> !memberUserIds.contains(targetUserId))
                    .collect(toSet());
            if (CollectionUtils.isEmpty(addUserIds)) {
                return;
            }
            ProjectMember targetProjectMember = ProjectMember.builder()
                    .projectId(project.getId())
                    .type(type)
                    .deletedAt(BeanUtils.getDefaultDeletedAt())
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .lastVisitAt(new Timestamp(System.currentTimeMillis()))
                    .alias("").build();
            projectMemberDao.insertList(addUserIds, targetProjectMember);

            AclProto.Role role = projectMemberAdaptorFactory.create(project.getPmType())
                    .assignUsersToRoleByRoleType(project, addUserIds, type);

            addUserIds.forEach(userId -> {
                AtomicInteger insertRole = new AtomicInteger(0);
                insertRole.set(role.getId());
                ProjectMember projectMember = projectMemberDao.getByProjectIdAndUserId(project.getId(),
                        userId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
                //发送消息
                projectMemberAdaptorFactory.create(project.getPmType())
                        .postAddMembersEvent(insertRole, currentUserId, project, projectMember, userId, isInvite);

                createMemberEventTrigger.trigger(
                        ImmutableList.of(String.valueOf(role.getId())),
                        projectMember,
                        project.getId(),
                        currentUserId
                );
                projectHandCacheService.handleProjectMemberCache(projectMember, CacheTypeEnum.CREATE);
            });

        } catch (Exception e) {
            log.error("advancedRoleServiceGrpcClient assignUsersToRoleByRoleType is error{} ", e.getMessage());
        }
    }

    public ResultPage<ProjectTeamMemberDTO> getMemberWithProjectAndTeam(
            Integer projectId,
            String keyWord,
            PageRowBounds pager
    ) throws CoreException {
        if (Objects.isNull(projectDao.getProjectById(projectId))) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        List<ProjectTeamMemberDTO> members = projectMemberDao.getMemberWithProjectAndTeam(projectId, keyWord, pager);
        return new ResultPageFactor<ProjectTeamMemberDTO>().def(pager, members);
    }

    private List<RoleDTO> toRoleDTO(List<AclProto.Role> roles) {
        List<RoleDTO> list = new ArrayList<>();
        roles.forEach(role -> list.add(RoleDTO.builder()
                .name(role.getName())
                .roleType(role.getType())
                .roleId(role.getId()).build()));
        return list;

    }

    private RoleDTO toRoleMemberDTO(AdvancedRoleProto.RoleMemberCount roleMemberCount) {
        return RoleDTO.builder()
                .roleId(roleMemberCount.getRoleId())
                .roleType(roleMemberCount.getRoleType())
                .description(roleMemberCount.getDescription())
                .name(roleMemberCount.getName())
                .createdAt(roleMemberCount.getCreatedAt())
                .memberCount(roleMemberCount.getMemberCount())
                .isPermissionCanEdit(roleMemberCount.getPermissionCanEdit())
                .isRoleCanDelete(roleMemberCount.getRoleCanDelete())
                .isRoleCanEdit(roleMemberCount.getRoleCanEdit()).build();

    }

    public List<Integer> filterNotifyUserIds(
            Integer userId,
            String content,
            Project project,
            ProjectTweet tweet) {
        // 获取被 @ 的用户编号
        Set<Integer> atUserIds = parseAtUser(userId, project, content, tweet.getOwnerId());
        // 获取用户列表
        List<ProjectMember> members = findListByProjectId(project.getId());
        // 过滤创建者自身及被 @ 的用户
        return members.stream()
                .filter(m ->
                        !Objects.equals(m.getUserId(), userId)
                                && !atUserIds.contains(m.getUserId()))
                .map(ProjectMember::getUserId)
                .collect(toList());
    }

    public Set<Integer> parseAtUser(Integer userId, Project project, String content, Integer targetOwnerId) {
        Set<Integer> userIdSet = new HashSet<>();
        if (userId == null || project == null || StringUtils.isEmpty(content)) {
            return userIdSet;
        }

        if (null == project.getTeamOwnerId()) {
            return userIdSet;
        }

        Matcher matcher = AT_REG.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(2);
            if ("all".equals(StringUtils.lowerCase(name))) {
                userIdSet.addAll(findListByProjectId(project.getId()).stream()
                        .map(ProjectMember::getId)
                        .collect(Collectors.toSet()));
                break;
            }
            UserProto.User user = userGrpcClient.getUserByNameAndTeamId(name, project.getTeamOwnerId());
            if (null == user) {
                user = userGrpcClient.getUserByGlobalKey(name);
            }
            if (null == user) {
                continue;
            }
            if (project.getType().equals(PROJECT_PRIVATE) && !isMember(user, project.getId())) {
                continue;
            }
            if (Objects.equals(user.getId(), targetOwnerId)) {
                continue;
            }
            userIdSet.add(user.getId());
        }
        userIdSet.remove(userId);
        return userIdSet;
    }

    public boolean isMember(UserProto.User user, Integer projectId) {
        if (Objects.isNull(user) || Objects.isNull(projectId)) {
            return false;
        }
        boolean flag = StringUtils.equals(
                user.getGlobalKey(),
                codingSettings.getEnterprise().getTokenUser()
        );
        if (flag) {
            return true;
        }
        return getByProjectIdAndUserId(projectId, user.getId()) != null;
    }

    public void delMember(Integer currentUserId, Integer projectId, Integer targetUserId) throws CoreException {
        if (currentUserId.equals(targetUserId)) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        Project project = projectDao.getProjectById(projectId);
        if (null == project) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        projectMemberAdaptorFactory.create(project.getPmType())
                .checkProjectMemberRoleType(project.getTeamOwnerId(), project.getId(), targetUserId);

        delMember(currentUserId, project, targetUserId);
    }

    public void delMember(Integer currentUserId, Project project, Integer targetUserId) throws CoreException {
        ProjectMember member = getByProjectIdAndUserId(project.getId(), targetUserId);
        if (member == null) {
            log.error("User {} is not member of project {}", targetUserId, project.getId());
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        ProjectMember currentMember = getByProjectIdAndUserId(project.getId(), currentUserId);
        if (null == currentMember) {
            log.error("CurrentUser {} is not member of project {}", currentUserId, project.getId());
            throw CoreException.of(CoreException.ExceptionType.PROJECT_MEMBER_NOT_EXISTS);
        }
        projectMemberAdaptorFactory.create(project.getPmType())
                .checkDelProjectMemberType(member, currentMember.getType());

        delMember(currentUserId, project, targetUserId, member);

    }

    public void delMember(Integer currentUserId, Project project, Integer targetUserId, ProjectMember member) {
        projectMemberDao.deleteMember(project.getId(), targetUserId, BeanUtils.getDefaultDeletedAt());
        List<String> roleIdList = advancedRoleServiceGrpcClient.findUserRolesInProject
                (targetUserId,
                        project.getTeamOwnerId(),
                        project.getId()).stream().map(role -> String.valueOf(role.getId())).collect(toList());
        advancedRoleServiceGrpcClient.removeUserRoleRecordsInProject(project.getId(), targetUserId);
        projectMemberAdaptorFactory.create(project.getPmType())
                .postDeleteMemberEvent(currentUserId, project, member);
        deleteMemberEventTrigger.trigger(roleIdList, member, project.getId(), currentUserId);
        projectHandCacheService.handleProjectMemberCache(member, CacheTypeEnum.DELETE);
    }

    public int quit(Integer projectId) throws CoreException {
        Project project = projectDao.getProjectById(projectId);
        if (null == project) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        UserProto.User currentUser = SystemContextHolder.get();
        if (null == currentUser) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        projectMemberAdaptorFactory.create(project.getPmType())
                .checkProjectMemberRoleType(project.getTeamOwnerId(), project.getId(), currentUser.getId());
        if (!isMember(currentUser, project.getId())) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        ProjectMember targetProjectMember = getByProjectIdAndUserId(project.getId(), currentUser.getId());
        if (targetProjectMember == null) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        Integer userId = targetProjectMember.getUserId();
        int result = projectMemberDao.deleteMember(project.getId(), userId, BeanUtils.getDefaultDeletedAt());
        if (result > 0) {
            advancedRoleServiceGrpcClient.removeUserRoleRecordsInProject(project.getId(), userId);
            projectMemberAdaptorFactory.create(project.getPmType())
                    .postMemberQuitEvent(project, targetProjectMember);
            projectHandCacheService.handleProjectMemberCache(targetProjectMember, CacheTypeEnum.DELETE);
        }
        return result;
    }

    public boolean updateVisitTime(Integer projectMemberId) {
        return projectMemberDao.updateVisitTime(projectMemberId, BeanUtils.getDefaultDeletedAt()) == 1;
    }
}
