package net.coding.lib.project.service.member;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dto.ProjectJoinProjectMemberDTO;
import net.coding.lib.project.dto.ProjectMemberDTO;
import net.coding.lib.project.dto.ProjectTeamMemberDTO;
import net.coding.lib.project.dto.request.ProjectMemberQueryPageReqDTO;
import net.coding.lib.project.dto.response.ProjectMemberQueryPageRespDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.PlatformGrantObjectGRpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.pager.ResultPageFactor;
import net.coding.lib.project.service.project.ProjectsService;
import net.coding.lib.project.utils.UserUtil;
import net.coding.platform.ram.proto.grant.object.GrantObjectProto;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.platform.user.UserProto;

import static java.util.stream.Collectors.toList;
import static net.coding.lib.project.exception.CoreException.ExceptionType.PROJECT_NOT_EXIST;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberPrincipalService {

    private final ProjectDao projectDao;

    private final ProjectsService projectListService;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final ProjectMemberFilterService projectMemberFilterService;

    private final UserGrpcClient userGrpcClient;

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final PlatformGrantObjectGRpcClient platformGrantObjectGRpcClient;

    public ResultPage<ProjectMemberQueryPageRespDTO> findProjectMemberPrincipalPages(ProjectMemberQueryPageReqDTO reqDTO,
                                                                                     LimitedPager pager)
            throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(reqDTO.getProjectId(), reqDTO.getTeamId());
        if (Objects.isNull(project)) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        List<ProjectMemberQueryPageRespDTO> respDTOs = new ArrayList<>();
        List<ProjectMember> members = projectMemberFilterService.filterQueryParameterGrantMembers(
                reqDTO.getUserId(),
                project,
                reqDTO.getPolicyId(),
                reqDTO.getKeyword()
        );
        if (CollectionUtils.isEmpty(members)) {
            return new ResultPage<>(respDTOs, pager.getPage(), pager.getPageSize(), respDTOs.size());
        }
        respDTOs = StreamEx.of(members)
                .sorted(Comparator.comparing(ProjectMember::getPrincipalSort)
                        .thenComparing(ProjectMember::getCreatedAt, Comparator.reverseOrder()))
                .skip((pager.getPage() - 1) * pager.getPageSize())
                .limit(pager.getPageSize())
                .groupingBy(ProjectMember::getPrincipalType)
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    Set<String> principalIds = StreamEx.of(entry.getValue())
                            .map(ProjectMember::getPrincipalId)
                            .toSet();
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())) {
                        List<Long> groupIds = StreamEx.of(principalIds).map(Long::valueOf).collect(toList());
                        return projectMemberInspectService.getUserGroups(reqDTO.getUserId(), groupIds)
                                .stream()
                                .map(dto -> ProjectMemberQueryPageRespDTO.builder()
                                        .principalType(entry.getKey())
                                        .principalId(String.valueOf(dto.getId()))
                                        .principalName(dto.getName())
                                        .build()
                                );
                    }
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())) {
                        GrantObjectProto.GetGrantObjectNamesResponse response =
                                platformGrantObjectGRpcClient.getGrantObjectNames(entry.getKey(), principalIds);
                        return response.getGrantObjectIdNameMap()
                                .entrySet()
                                .stream()
                                .map(entryMap -> ProjectMemberQueryPageRespDTO.builder()
                                        .principalType(entry.getKey())
                                        .principalId(entryMap.getKey())
                                        .principalName(entryMap.getValue())
                                        .build());
                    }
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER.name())) {
                        List<Integer> userIds = StreamEx.of(principalIds).map(Integer::valueOf).toList();
                        UserProto.FindUserResponse response = userServiceGrpcClient.findUserByIds(userIds);
                        return StreamEx.of(response.getDataList())
                                .map(user -> ProjectMemberQueryPageRespDTO.builder()
                                        .principalType(entry.getKey())
                                        .principalId(String.valueOf(user.getId()))
                                        .principalName(user.getName())
                                        .avatar(user.getAvatar())
                                        .build()
                                );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .peek(respDTO -> StreamEx.of(members)
                        .filter(m -> m.getPrincipalId().equals(respDTO.getPrincipalId())
                                && m.getPrincipalType().equals(respDTO.getPrincipalType()))
                        .findFirst()
                        .ifPresent(member -> {
                            respDTO.setPrincipalSort(member.getPrincipalSort());
                            respDTO.setCreatedAt(member.getCreatedAt().getTime());
                        }))
                .sorted(Comparator.comparing(ProjectMemberQueryPageRespDTO::getPrincipalSort)
                        .thenComparing(ProjectMemberQueryPageRespDTO::getCreatedAt, Comparator.reverseOrder()))
                .collect(toList());
        return new ResultPage<>(respDTOs, pager.getPage(), pager.getPageSize(), members.size());
    }

    /**
     * 我参与的所有项目及每个项目下成员用户
     */
    public List<ProjectJoinProjectMemberDTO> findJoinProjectMembers(Integer teamId, Integer userId, String keyword) {
        return StreamEx.of(
                        projectListService.getJoinedPrincipalProjects(teamId, userId, keyword)
                )
                .limit(100)
                .map(project -> {
                    List<ProjectMember> members = projectMemberInspectService.getPrincipalUserMembers(project.getId());
                    return ProjectJoinProjectMemberDTO.builder()
                            .projectId(project.getId())
                            .projectName(project.getName())
                            .projectDisplayName(project.getDisplayName())
                            .projectIcon(project.getIcon())
                            .userIds(StreamEx.of(members).map(ProjectMember::getUserId).toList())
                            .build();
                })
                .collect(toList());
    }

    /**
     * 查询 授权体下所有的用户列表 ，兼容老项目成员接口
     */
    public ResultPage<ProjectMemberDTO> getProjectMembers(Integer teamId,
                                                          Integer projectId,
                                                          String keyword,
                                                          Integer roleId,
                                                          PageRowBounds pager) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        Set<Integer> memberUserIds = projectMemberFilterService.filterQueryParameterMembers(project, roleId, keyword);
        List<ProjectMemberDTO> projectMemberDTOS = StreamEx.of(memberUserIds)
                .sorted(Comparator.comparing(userId -> userId, Comparator.reverseOrder()))
                .skip(pager.getOffset())
                .limit(pager.getLimit())
                .map(userId -> {
                    UserProto.User user = userGrpcClient.getUserById(userId);
                    List<AclProto.Role> roles =
                            advancedRoleServiceGrpcClient.findUserRolesInProject(userId, teamId, projectId);
                    return ProjectMemberDTO.builder()
                            .project_id(project.getId())
                            .user_id(userId)
                            .user(UserUtil.toBuilderUser(user, false))
                            .roles(UserUtil.toRoleDTO(roles))
                            .build();
                })
                .toList();
        pager.setTotal((long) memberUserIds.size());
        return new ResultPageFactor<ProjectMemberDTO>().def(pager, projectMemberDTOS);
    }

    /**
     * 查询团队成员，如果在某个项目中则排序在前
     */
    public ResultPage<ProjectTeamMemberDTO> getMemberWithProjectAndTeam(
            Integer teamId,
            Integer projectId,
            String keyWord,
            PageRowBounds pager
    ) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(PROJECT_NOT_EXIST);
        }
        List<Integer> projectUserIds = projectMemberInspectService.getPrincipalUserMembers(project.getId())
                .stream()
                .map(ProjectMember::getUserId)
                .collect(toList());
        List<UserProto.User> teamUser = projectMemberInspectService.getTeamMemberId(teamId, keyWord);
        List<ProjectTeamMemberDTO> members = StreamEx.of(teamUser)
                .map(user ->
                        ProjectTeamMemberDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .namePinyin(user.getNamePinyin())
                                .avatar(user.getAvatar())
                                .isProjectMember(projectUserIds.contains(user.getId()))
                                .lastLoginedAt(user.getLastLoginedAt())
                                .build()
                )
                .sorted(Comparator.comparing(ProjectTeamMemberDTO::isProjectMember, Comparator.reverseOrder())
                        .thenComparing(ProjectTeamMemberDTO::getId, Comparator.reverseOrder()))
                .skip(pager.getOffset())
                .limit(pager.getLimit())
                .collect(toList());
        pager.setTotal((long) teamUser.size());
        return new ResultPageFactor<ProjectTeamMemberDTO>().def(pager, members);
    }
}
