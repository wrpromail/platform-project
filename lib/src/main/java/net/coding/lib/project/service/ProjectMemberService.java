package net.coding.lib.project.service;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.RoleDTO;
import net.coding.lib.project.dto.UserDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.entity.ProjectTweet;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddMemberForm;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.helper.ProjectServiceHelper;
import net.coding.lib.project.pager.ResultPageFactor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.acl.AclProto;
import proto.advanced_role.AdvancedRoleProto;
import proto.platform.user.UserProto;


import static java.util.stream.Collectors.toList;
import static net.coding.common.constants.ProjectConstants.PROJECT_PRIVATE;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberService {

    public static final Pattern AT_REG = Pattern.compile("(@([^@\\s<>()（）：:，,。…~!！？?'‘\"]+))(.{0}|\\s)");

    private final ProjectMemberDao projectMemberDao;

    private final ProjectDao projectDao;

    private final UserGrpcClient userGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final ProjectServiceHelper projectServiceHelper;

    private final ProjectGrpcClient projectGrpcClient;

    private final short MEMBER_TYPE = 80;


    public ProjectMember getById(Integer id) {
        return projectMemberDao.getById(id);
    }

    public int insert(ProjectMember projectMember) {
        return projectMemberDao.insert(projectMember);
    }

    public int update(ProjectMember projectMember) {
        return projectMemberDao.update(projectMember);
    }

    public boolean updateProjectMemberType(Integer projectId, Integer targetUserId, short type) throws CoreException {
        return projectMemberDao.updateProjectMemberType(projectId, targetUserId, type, BeanUtils.getDefaultDeletedAt()) > 0;

    }

    public List<ProjectMember> findListByProjectId(Integer projectId) {
        return projectMemberDao.findListByProjectId(projectId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public ProjectMember getByProjectIdAndUserId(Integer projectId, Integer userId) {
        return projectMemberDao.getByProjectIdAndUserId(projectId, userId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public ResultPage<ProjectMemberDTO> getProjectMembers(Integer projectId, String keyWord, PageRowBounds pager) throws CoreException {
        if (Objects.isNull(projectDao.getProjectById(projectId))) {
            CoreException.of(CoreException.ExceptionType.PROJECT_NOT_EXIST);
        }
        List<ProjectMember> projectMemberList = projectMemberDao.getProjectMembers(projectId, keyWord, pager);
        List<ProjectMemberDTO> projectMembers = new ArrayList<>();
        projectMemberList.stream().forEach(projectMember ->
                projectMembers.add(ProjectMemberDTO.builder()
                        .id(projectMember.getId())
                        .project_id(projectMember.getProjectId())
                        .user_id(projectMember.getUserId())
                        .created_at(projectMember.getCreatedAt().getTime())
                        .last_visit_at(projectMember.getLastVisitAt().getTime()).build()));
        if (!CollectionUtils.isEmpty(projectMembers)) {
            projectMembers.stream().forEach(projectMemberDTO ->
                    projectMemberDTO.setUser(new UserDTO(userGrpcClient.getUserById(projectMemberDTO.getUser_id())
                    )));
            projectMembers.stream().forEach(projectMemberDTO ->
                    projectMemberDTO.setRoles(
                            toRoleDTO(
                                    advancedRoleServiceGrpcClient.findUserRolesInProject(projectMemberDTO.getUser().getId(),
                                            projectMemberDTO.getUser().getTeamId(), projectId))));
        }
        return new ResultPageFactor<ProjectMemberDTO>().def(pager, projectMembers);

    }

    public List<RoleDTO> findMemberCountByProjectId(Integer projectId) throws CoreException {
        List<RoleDTO> roleDTOList = new ArrayList<>();
        try {
            List<AdvancedRoleProto.RoleMemberCount> roleMemberCounts =
                    advancedRoleServiceGrpcClient.findMemberCountByProjectId(projectId);
            roleMemberCounts.stream().forEach(roleMemberCount -> roleDTOList.add(toRoleMemberDTO(roleMemberCount)));

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
        ProjectMember projectMember = projectMemberDao.getProjectMemberByUserAndProject(currentUser.getId(),
                projectId, BeanUtils.getDefaultDeletedAt());
        if (projectMember == null || projectMember.getType() <= MEMBER_TYPE) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }


        List<Integer> targetUserIds = new ArrayList<>();
        Arrays.stream(addMemberForm.getUsers().split(",")).forEach(targetUserIdStr -> {
            UserProto.User user = userGrpcClient.getUserByGlobalKey(targetUserIdStr);
            if (Objects.nonNull(user) || user.getId() != currentUser.getId()) {
                Integer id = user.getId();
                targetUserIds.add(id);
            }
        });
        doAddMember(currentUser.getId(), targetUserIds, addMemberForm.getType(), project, false);
    }

    public void doAddMember(Integer currentUserId, List<Integer> targetUserIds,
                            short type, Project project, boolean isInvite) throws CoreException {
        Timestamp init_at = new Timestamp(System.currentTimeMillis());
        List<Integer> memberUserIdList = targetUserIds.stream()
                .filter(targetUserId ->
                        Objects.isNull(projectMemberDao.getProjectMemberByUserAndProject(
                                targetUserId, project.getId(), BeanUtils.getDefaultDeletedAt())))
                .collect(toList());
        if (CollectionUtils.isEmpty(memberUserIdList)) {
            return;
        }
        ProjectMember targetProjectMember = ProjectMember.builder()
                .projectId(project.getId())
                .type(type)
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .createdAt(init_at)
                .lastVisitAt(init_at)
                .alias("").build();

        projectMemberDao.insertList(memberUserIdList, targetProjectMember);

        try {
            Optional<AdvancedRoleProto.FindProjectRoleByRoleAndProjectResponse> response =
                    advancedRoleServiceGrpcClient.findProjectRoleByRoleAndProject(project.getId(), type);
            targetUserIds.stream().forEach(userId -> {
                AtomicInteger insertRole = new AtomicInteger(0);
                try {
                    advancedRoleServiceGrpcClient.insertProjectRoleRecord(project.getId(),
                            response.get().getRole(),
                            project.getTeamOwnerId(),
                            userId);
                } catch (Exception e) {
                    log.error("advancedRoleServiceGrpcClient insertProjectRoleRecord is error{} ", e.getMessage());
                }
                insertRole.set(response.get().getRole().getId());
                //发送消息
                ProjectMember projectMember = getByProjectIdAndUserId(project.getId(), userId);
                projectServiceHelper.postAddMembersEvent(currentUserId, project.getId(), projectMember, userId, isInvite);
            });

        } catch (Exception e) {
            log.error("advancedRoleServiceGrpcClient findProjectRoleByRoleAndProject is error{} ", e.getMessage());
        }


    }


    private List<RoleDTO> toRoleDTO(List<AclProto.Role> roles) {
        List<RoleDTO> list = new ArrayList<>();
        roles.stream().forEach(role -> list.add(RoleDTO.builder()
                .name(role.getName())
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
            if (project.getType().equals(PROJECT_PRIVATE) && !this.isMember(user, project.getId())) {
                continue;
            }
            if (Objects.equals(Integer.valueOf(user.getId()), targetOwnerId)) {
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
        if (projectGrpcClient.isProjectRobotUser(user.getGlobalKey())) {
            return true;
        }
        return getByProjectIdAndUserId(projectId, user.getId()) != null;
    }


}
