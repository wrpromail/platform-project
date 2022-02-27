package net.coding.lib.project.service.member;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.grpc.client.PlatformGrantObjectGRpcClient;
import net.coding.lib.project.grpc.client.RamMappingGRpcClient;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.parameter.ProjectPrincipalParameter;
import net.coding.lib.project.service.RamTransformTeamService;
import net.coding.platform.ram.pojo.dto.GrantDTO;
import net.coding.platform.ram.pojo.dto.GrantObjectDTO;
import net.coding.platform.ram.pojo.dto.request.ListEntitiesRequestDTO;
import net.coding.platform.ram.pojo.dto.request.ListResourceRequestDTO;
import net.coding.platform.ram.pojo.dto.request.ResourceGrantDTO;
import net.coding.platform.ram.pojo.dto.request.UserGroupSearchRequestDTO;
import net.coding.platform.ram.pojo.dto.response.GrantObjectIdResponseDTO;
import net.coding.platform.ram.pojo.dto.response.PolicyResponseDTO;
import net.coding.platform.ram.pojo.dto.response.UserGroupResponseDTO;
import net.coding.platform.ram.proto.grant.object.GrantObjectProto;
import net.coding.platform.ram.service.PolicyGrantRemoteService;
import net.coding.platform.ram.service.PolicyRemoteService;
import net.coding.platform.ram.service.UserGroupRemoteService;
import net.coding.platform.ram.service.UserGroupUserRemoteService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.common.PagerProto;
import proto.platform.permission.PermissionProto;
import proto.platform.user.UserProto;
import proto.ram.mapping.RamMappingProto;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.coding.common.util.LimitedPager.DEFAULT_MAX_PAGE_SIZE;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberInspectService {

    private final UserGrpcClient userGrpcClient;

    private final ProjectMemberDao projectMemberDao;

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final PlatformGrantObjectGRpcClient platformGrantObjectGRpcClient;

    private final RamMappingGRpcClient ramMappingGRpcClient;

    private final PolicyRemoteService policyRemoteService;

    private final PolicyGrantRemoteService policyGrantRemoteService;

    private final UserGroupUserRemoteService userGroupUserRemoteService;

    private final UserGroupRemoteService userGroupRemoteService;

    private final RamTransformTeamService ramTransformTeamService;

    private final AclServiceGrpcClient aclServiceGrpcClient;

    public List<ProjectMember> findListByProjectId(Integer projectId) {
        return projectMemberDao.findListByProjectId(projectId, BeanUtils.getDefaultDeletedAt());
    }

    /**
     * 查询用户拥有指定身份的资源 ID
     * <p>
     * 包括用户所在的各个容器授权体对应的资源 ID
     * <p>
     * 使用场景：查询【我管理的】项目，即查询具有【项目管理员】权限组的 项目 ID
     * <p>
     */
    public Set<Integer> listResourcesOnUser(Integer operatorId,
                                            String resourceType,
                                            Set<Integer> resourceIdScope,
                                            Long userId,
                                            String policyName) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            if (!ramTransformTeamService.ramOnline(user.getTeamId())) {
                if (PmTypeEnums.PROJECT.name().equals(resourceType)) {
                    return getAccessProjectIdList(
                            PermissionProto.Function.ProjectDepotSetting,
                            PermissionProto.Action.Update,
                            userId.intValue());
                } else {
                    return getAccessProjectIdList(
                            PermissionProto.Function.ProjectBasicSetting,
                            PermissionProto.Action.Update,
                            userId.intValue());
                }
            }
            SystemContextHolder.set(user);
            int page = 1;
            Set<String> projectIds = new HashSet<>();
            while (true) {
                ResultPage<String> resultPage = policyGrantRemoteService.listResourcesOnUser(
                        resourceType,
                        StreamEx.of(resourceIdScope).map(String::valueOf).collect(toSet()),
                        userId,
                        StreamEx.of(policyName).toSet(),
                        new LimitedPager(page, DEFAULT_MAX_PAGE_SIZE));
                if (CollectionUtils.isNotEmpty(resultPage.getList())) {
                    projectIds.addAll(resultPage.getList());
                }
                if (resultPage.getTotalPage() <= page) {
                    break;
                }
                page++;
            }
            return StreamEx.of(projectIds)
                    .map(Integer::valueOf)
                    .toSet();
        } catch (Exception ex) {
            log.error("listResourcesOnUser error, message = {}", ex.getMessage());
        } finally {
            SystemContextHolder.remove();
        }
        return Collections.emptySet();
    }


    /**
     * 策略ID 在项目资源内的授权体 ID 列表
     */
    public List<GrantObjectIdResponseDTO> listGrantObjectIds(Integer operatorId, Project project, Long policyId) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            int page = 1;
            List<GrantObjectIdResponseDTO> grantDTOs = new ArrayList<>();
            while (true) {
                ResultPage<GrantObjectIdResponseDTO> resultPage = policyGrantRemoteService.listGrantObjectIds(
                        new ListEntitiesRequestDTO()
                                .setPolicyIds(StreamEx.of(policyId).toSet())
                                .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
                                .setResourceIds(StreamEx.of(project.getId()).map(String::valueOf).toSet()),
                        new LimitedPager(page, DEFAULT_MAX_PAGE_SIZE));
                if (CollectionUtils.isNotEmpty(resultPage.getList())) {
                    grantDTOs.addAll(resultPage.getList());
                }
                if (resultPage.getTotalPage() <= page) {
                    break;
                }
                page++;
            }
            return grantDTOs;
        } catch (Exception ex) {
            log.error("listGrantObjectIds error, message = {}", ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 成员主体有某个权限组的项目
     */
    public Set<Integer> listResource(Integer operatorId,
                                     String principalType,
                                     String principalId,
                                     Long policyId) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            int page = 1;
            GrantObjectDTO grantObjectDTO = new GrantObjectDTO()
                    .setGrantScope(principalType)
                    .setGrantObjectId(principalId);
            Set<Integer> projectIds = new HashSet<>();
            while (true) {
                ResultPage<String> resultPage = policyGrantRemoteService.listResource(
                        new ListResourceRequestDTO()
                                .setPolicyIds(StreamEx.of(policyId).toSet())
                                .setResourceType(PmTypeEnums.PROJECT.name().toLowerCase())
                                .setGrantObjectDTOS(StreamEx.of(grantObjectDTO).toSet()),
                        new LimitedPager(page, DEFAULT_MAX_PAGE_SIZE));
                if (CollectionUtils.isNotEmpty(resultPage.getList())) {
                    projectIds.addAll(StreamEx.of(resultPage.getList()).map(Integer::valueOf).toSet());
                }
                if (resultPage.getTotalPage() <= page) {
                    break;
                }
                page++;
            }
            return projectIds;
        } catch (Exception ex) {
            log.error("listResource error, message = {}", ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 获取团队成员
     */
    public List<UserProto.User> getTeamMemberId(Integer teamId, String keyword) {
        int page = 1;
        List<UserProto.User> userList = new ArrayList<>();
        while (true) {
            UserProto.PaginationUsersSearchResponse response =
                    userServiceGrpcClient.paginationUsersSearch(teamId, keyword, page, DEFAULT_MAX_PAGE_SIZE);
            if (CollectionUtils.isNotEmpty(response.getData().getDataList())) {
                userList.addAll(response.getData().getDataList());
            }
            PagerProto.PageInfo pagerInfo = response.getPagerInfo();
            if (pagerInfo.getTotalPage() <= page) {
                break;
            }
            page++;
        }
        return userList;
    }

    /**
     * 获取所有用户Id 组装 ProjectMember
     */
    public List<ProjectMember> getPrincipalUserMembers(Integer projectId) {
        List<ProjectMember> members = findListByProjectId(projectId);
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        return StreamEx.of(getPrincipalMemberUserIds(members))
                .map(userId -> ProjectMember.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .build())
                .toList();
    }

    /**
     * 获取所有用户Id
     */
    public Set<Integer> getPrincipalMemberUserIds(List<ProjectMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptySet();
        }
        return StreamEx.of(getUserIds(members), getUserGroupUserIds(members), getDepartmentUserIds(members))
                .flatMap(Collection::stream)
                .toSet();
    }

    /**
     * 获取在用户是否在授权体中,返回用户信息
     */
    public ProjectMember getPrincipalUserMember(Integer projectId, Integer userId) {
        List<ProjectMember> members = findListByProjectId(projectId);
        if (CollectionUtils.isEmpty(members)) {
            return null;
        }
        if (getUserIds(members).contains(userId)
                || getUserGroupUserIds(members).contains(userId)
                || getDepartmentUserIds(members).contains(userId)) {
            return ProjectMember.builder()
                    .projectId(projectId)
                    .userId(userId)
                    .build();
        }
        return null;
    }

    /**
     * 根据授权类型过滤授权体
     */
    public Set<String> getPrincipalIds(List<ProjectMember> members, ProjectMemberPrincipalTypeEnum principalTypeEnum) {
        return StreamEx.of(members)
                .filter(member -> member.getPrincipalType().equals(principalTypeEnum.name()))
                .map(ProjectMember::getPrincipalId)
                .toSet();
    }

    /**
     * 授权体用户组下用户Id
     */
    public Set<Integer> getUserGroupUserIds(List<ProjectMember> members) {
        Set<Long> grantObjectIds = getPrincipalIds(members, ProjectMemberPrincipalTypeEnum.USER_GROUP)
                .stream()
                .map(Long::valueOf)
                .collect(toSet());
        if (CollectionUtils.isEmpty(grantObjectIds)) {
            return new HashSet<>();
        }
        return userGroupUserRemoteService.getUserIdsForGroup(grantObjectIds, new HashSet<>())
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(Long::intValue)
                .collect(toSet());
    }

    /**
     * 授权体部门下用户Id
     */
    public Set<Integer> getDepartmentUserIds(List<ProjectMember> members) {
        Set<String> grantObjectIds = getPrincipalIds(members, ProjectMemberPrincipalTypeEnum.DEPARTMENT);
        if (CollectionUtils.isEmpty(grantObjectIds)) {
            return new HashSet<>();
        }
        GrantObjectProto.FindUserIdsResponse response =
                platformGrantObjectGRpcClient.findUserIds(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name(), grantObjectIds);
        return response.getUserIdsMap()
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().getUserIdList().stream())
                .map(Long::intValue)
                .collect(toSet());
    }

    /**
     * 授权体用户Id
     */
    public Set<Integer> getUserIds(List<ProjectMember> members) {
        return getPrincipalIds(members, ProjectMemberPrincipalTypeEnum.USER)
                .stream()
                .map(Integer::valueOf)
                .collect(toSet());
    }

    /**
     * 参与的项目Id 集合
     */
    public Set<Integer> getJoinedProjectIds(ProjectMemberPrincipalQueryParameter parameter) {
        List<ProjectMember> userGroupMembers = Optional.ofNullable(getGroupIdsForUser(parameter.getUserId()))
                .map(principalParameter -> {
                    parameter.setPrincipalType(principalParameter.getPrincipalType());
                    parameter.setPrincipalIds(principalParameter.getPrincipalIds());
                    return projectMemberDao.findPrincipalMembers(parameter);
                }).orElse(new ArrayList<>());

        List<ProjectMember> departmentMembers = Optional.ofNullable(getDepartmentIdsForUser(parameter.getTeamId(), parameter.getUserId()))
                .map(principalParameter -> {
                    parameter.setPrincipalType(principalParameter.getPrincipalType());
                    parameter.setPrincipalIds(principalParameter.getPrincipalIds());
                    return projectMemberDao.findPrincipalMembers(parameter);
                }).orElse(new ArrayList<>());

        List<ProjectMember> userMembers = Optional.ofNullable(getUserIdsForUser(parameter.getUserId()))
                .map(principalParameter -> {
                    parameter.setPrincipalType(principalParameter.getPrincipalType());
                    parameter.setPrincipalIds(principalParameter.getPrincipalIds());
                    return projectMemberDao.findPrincipalMembers(parameter);
                }).orElse(new ArrayList<>());
        // 兼容迁移前 principal 为空的情况
        List<ProjectMember> userJoinedMembers = projectMemberDao.findJoinPrincipalMembers(parameter);
        return StreamEx.of(userGroupMembers, departmentMembers, userMembers, userJoinedMembers)
                .flatMap(Collection::stream)
                .nonNull()
                .map(ProjectMember::getProjectId)
                .toSet();
    }


    /**
     * 用户所在用户组
     */
    public ProjectPrincipalParameter getGroupIdsForUser(Integer userId) {
        try {
            Set<String> userGroupIds = userGroupUserRemoteService.getGroupIdsForUser(userId.longValue(), new HashSet<>())
                    .stream()
                    .map(String::valueOf)
                    .collect(toSet());
            if (CollectionUtils.isEmpty(userGroupIds)) {
                return null;
            }
            return ProjectPrincipalParameter.builder()
                    .principalType(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())
                    .principalIds(userGroupIds)
                    .build();
        } catch (Exception e) {
            log.error("userGroupUserRemoteService.getGroupIdsForUser is error , userId = {} , message = {} ",
                    userId, e.getMessage());
            return null;
        }
    }

    /**
     * 用户所在部门
     */
    public ProjectPrincipalParameter getDepartmentIdsForUser(Integer teamId, Integer userId) {
        try {
            GrantObjectProto.FindGrantObjectIdsResponse response =
                    platformGrantObjectGRpcClient.findGrantObjectIds(
                            ProjectMemberPrincipalTypeEnum.DEPARTMENT.name(),
                            teamId,
                            userId
                    );
            if (CollectionUtils.isEmpty(response.getGrantObjectIdsList())) {
                return null;
            }
            return ProjectPrincipalParameter.builder()
                    .principalType(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())
                    .principalIds(StreamEx.of(response.getGrantObjectIdsList()).toSet())
                    .build();
        } catch (Exception e) {
            log.error("platformGrantObjectGRpcClient.findGrantObjectIds is error , userId = {} , message = {} ",
                    userId, e.getMessage());
            return null;
        }
    }

    /**
     * 用户自身
     */
    public ProjectPrincipalParameter getUserIdsForUser(Integer userId) {
        return ProjectPrincipalParameter.builder()
                .principalType(ProjectMemberPrincipalTypeEnum.USER.name())
                .principalIds(StreamEx.of(String.valueOf(userId)).toSet())
                .build();
    }

    /**
     * 拿到每个用户对应的权限
     */
    public Set<String> getUserIdAndRoleIds(List<ProjectMember> members,
                                           Map<String, Set<Integer>> grantRoleIdMap) {
        Set<String> userIdAndRoleIds = new HashSet<>();
        StreamEx.of(members)
                .groupingBy(ProjectMember::getPrincipalType)
                .forEach((key, value) -> {
                    if (key.equals(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())) {
                        StreamEx.of(value)
                                .forEach(member -> {
                                    Set<Integer> roleIds = grantRoleIdMap.get(StringUtils.join(member.getPrincipalId(), member.getPrincipalType()));
                                    if (CollectionUtils.isEmpty(roleIds)) {
                                        return;
                                    }
                                    userGroupUserRemoteService.getUserIdsForGroup(Long.valueOf(member.getPrincipalId()), new HashSet<>())
                                            .forEach(userId -> StreamEx.of(roleIds)
                                                    .forEach(roleId -> userIdAndRoleIds.add(StringUtils.join(Arrays.asList(userId, roleId), ":"))));
                                });
                    }
                    if (key.equals(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())) {
                        StreamEx.of(value)
                                .forEach(member -> {
                                    Set<Integer> roleIds = grantRoleIdMap.get(StringUtils.join(member.getPrincipalId(), member.getPrincipalType()));
                                    if (CollectionUtils.isEmpty(roleIds)) {
                                        return;
                                    }
                                    platformGrantObjectGRpcClient.findUserIds(key, StreamEx.of(member.getPrincipalId()).toSet())
                                            .getUserIdsMap()
                                            .entrySet()
                                            .stream()
                                            .flatMap(entry -> entry.getValue().getUserIdList().stream())
                                            .forEach(userId -> StreamEx.of(roleIds)
                                                    .forEach(roleId -> userIdAndRoleIds.add(StringUtils.join(Arrays.asList(userId, roleId), ":"))));
                                });
                    }
                    if (key.equals(ProjectMemberPrincipalTypeEnum.USER.name())) {
                        StreamEx.of(value)
                                .forEach(member -> {
                                    Set<Integer> roleIds = grantRoleIdMap.get(StringUtils.join(member.getPrincipalId(), member.getPrincipalType()));
                                    if (CollectionUtils.isEmpty(roleIds)) {
                                        return;
                                    }
                                    roleIds.forEach(roleId -> userIdAndRoleIds.add(StringUtils.join(Arrays.asList(member.getPrincipalId(), roleId), ":")));

                                });
                    }
                });
        return userIdAndRoleIds;
    }

    public Map<String, Set<Integer>> getGrantRoleIdMap(
            Integer operatorId,
            Project project,
            List<ProjectMember> members) {
        Map<ResourceGrantDTO, List<PolicyResponseDTO>> resourceGrantMap = getResourceGrantPolicies(operatorId, project, members, Collections.emptySet());
        Set<Long> policyIds = resourceGrantMap.entrySet()
                .stream()
                .flatMap(entry -> StreamEx.of(entry.getValue())
                        .map(PolicyResponseDTO::getPolicyId))
                .collect(toSet());
        RamMappingProto.GetRoleIdByPolicyResponse response = ramMappingGRpcClient.getRoleIdByPolicy(
                project.getTeamOwnerId(),
                project.getId(),
                PmTypeEnums.of(project.getPmType()).name().toLowerCase(),
                policyIds);
        Map<Long, RamMappingProto.RoleIds> policyIdRoleIdsMap = response.getPolicyIdRoleIdsMapMap();
        return resourceGrantMap
                .entrySet()
                .stream()
                .collect(toMap(
                        entry -> StringUtils.join(entry.getKey().getGrantObjectId(), entry.getKey().getGrantScope()),
                        entry -> StreamEx.of(entry.getValue())
                                .map(grantDTO -> policyIdRoleIdsMap.get(grantDTO.getPolicyId()))
                                .nonNull()
                                .flatMap(dto -> dto.getRoleIdList().stream())
                                .toSet()
                        )
                );
    }


    public Set<Integer> getAccessProjectIdList(PermissionProto.Function function, PermissionProto.Action action, Integer operatorId) throws Exception {
        return aclServiceGrpcClient.getAccessProjectIdList(
                StreamEx.of(PermissionProto.Permission.newBuilder()
                        .setFunction(function)
                        .setAction(action)
                        .build()
                ).toList(),
                operatorId
        )
                .stream()
                .map(AclProto.UserAclProjects::getProjectIdList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

    }

    /**
     * 获取授权体在资源中的权限组
     */
    public Map<ResourceGrantDTO, List<PolicyResponseDTO>> getResourceGrantPolicies(
            Integer operatorId,
            Project project,
            List<ProjectMember> members,
            Set<Long> policyIdScope) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            Set<ResourceGrantDTO> requestDTOS = StreamEx.of(members)
                    .map(member -> new ResourceGrantDTO()
                            .setGrantScope(member.getPrincipalType())
                            .setGrantObjectId(member.getPrincipalId())
                            .setResourceId(String.valueOf(project.getId()))
                            .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
                    ).toSet();
            return policyGrantRemoteService.getResourceGrantPolicies(requestDTOS, policyIdScope);
        } catch (Exception ex) {
            log.error("ResourceGrantPolicies error, operatorId = {}, message = {}", operatorId, ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 追加授权，原有其它权限不受影响，若授权已存在的不再进行追加
     */
    public boolean attachGrant(Integer operatorId, Collection<GrantDTO> grantDTOS) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            return policyGrantRemoteService.attachGrant(grantDTOS);
        } catch (Exception ex) {
            log.error("attachGrant error, message = {}", ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 收回授权，只收回参数指定的权限，已有的其它权限不受影响
     */
    public boolean detachGrant(Integer operatorId, Collection<GrantDTO> grantDTOS) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            return policyGrantRemoteService.detachGrant(grantDTOS);
        } catch (Exception ex) {
            log.error("detachGrant error, message = {}", ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 清除授权体在资源中的所有授权
     */
    public boolean removeResourceGrant(Integer operatorId, Project project, List<ProjectMember> members) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            List<ResourceGrantDTO> resourceGrantDTOS = StreamEx.of(members)
                    .map(m -> new ResourceGrantDTO()
                            .setGrantScope(m.getPrincipalType())
                            .setGrantObjectId(String.valueOf(m.getPrincipalId()))
                            .setResourceId(String.valueOf(project.getId()))
                            .setResourceType(PmTypeEnums.of(project.getPmType()).name().toLowerCase())
                    )
                    .toList();
            return policyGrantRemoteService.removeResourceGrant(resourceGrantDTOS);
        } catch (Exception ex) {
            log.error("removeResourceGrant error, operatorId = {}, projectId = {}, message = {}",
                    operatorId, project.getId(), ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 获取指定 name 对应的 policy
     */
    public PolicyResponseDTO getPolicyByName(Integer operatorId, String policyName) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            return policyRemoteService.getPolicyByName(policyName);
        } catch (Exception ex) {
            log.error("getPolicyByName error, operatorId = {}, policyName = {}, message = {}",
                    operatorId, policyName, ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 指定 ID 对应的用户组信息
     */
    public List<UserGroupResponseDTO> getUserGroups(Integer operatorId, Collection<Long> groupIds) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            return userGroupRemoteService.getUserGroups(groupIds);
        } catch (Exception ex) {
            log.error("getUserGroups error, message = {}", ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }

    /**
     * 查询用户组列表
     */
    public List<UserGroupResponseDTO> search(Integer operatorId, String keyword) {
        try {
            UserProto.User user = userGrpcClient.getUserById(operatorId);
            SystemContextHolder.set(user);
            return userGroupRemoteService.search(new UserGroupSearchRequestDTO()
                    .setName(keyword));
        } catch (Exception ex) {
            log.error("search error, operatorId = {}, name = {}, message = {}",
                    operatorId, keyword, ex.getMessage());
            throw ex;
        } finally {
            SystemContextHolder.remove();
        }
    }
}
