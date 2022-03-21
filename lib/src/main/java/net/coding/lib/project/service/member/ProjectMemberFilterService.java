package net.coding.lib.project.service.member;

import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.grpc.client.platform.department.DepartmentGrpcClient;
import net.coding.lib.project.dto.request.ProjectMemberAddReqDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.enums.ProjectMemberPrincipalTypeEnum;
import net.coding.lib.project.grpc.client.PlatformGrantObjectGRpcClient;
import net.coding.platform.ram.pojo.dto.response.UserGroupResponseDTO;
import net.coding.platform.ram.proto.grant.object.GrantObjectProto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.platform.department.DepartmentProto;
import proto.platform.user.UserProto;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@AllArgsConstructor
@Slf4j
public class ProjectMemberFilterService {

    private final DepartmentGrpcClient departmentGrpcClient;

    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;

    private final ProjectMemberInspectService projectMemberInspectService;

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final PlatformGrantObjectGRpcClient platformGrantObjectGRpcClient;

    /**
     * 根据条件过滤
     */
    public List<ProjectMember> filterQueryParameterGrantMembers(Integer operatorId, Project project, Long policyId, String keyword) {
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        if (CollectionUtils.isEmpty(members)) {
            return members;
        }
        if (Objects.nonNull(policyId) && policyId > 0) {
            Set<String> grants = StreamEx.of(projectMemberInspectService.listGrantObjectIds(operatorId, project, policyId))
                    .map(dto -> StringUtils.join(dto.getGrantScope(), dto.getGrantObjectId()))
                    .toSet();
            members = StreamEx.of(members)
                    .filter(member -> grants.contains(StringUtils.join(member.getPrincipalType(), member.getPrincipalId())))
                    .toList();
        }
        if (StringUtils.isNotBlank(keyword)) {
            return StreamEx.of(members)
                    .groupingBy(ProjectMember::getPrincipalType)
                    .entrySet()
                    .stream()
                    .flatMap(entry -> {
                        if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER_GROUP.name())) {
                            Set<String> userGroupIds = projectMemberInspectService.search(operatorId, keyword)
                                    .stream()
                                    .map(UserGroupResponseDTO::getId)
                                    .map(String::valueOf)
                                    .collect(Collectors.toSet());
                            return StreamEx.of(entry.getValue())
                                    .filter(member -> userGroupIds.contains(member.getPrincipalId()));
                        }
                        if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.DEPARTMENT.name())) {
                            DepartmentProto.FindByNameResponse response =
                                    departmentGrpcClient.findByName(project.getTeamOwnerId(), keyword);
                            return StreamEx.of(entry.getValue())
                                    .filter(member -> response.getDescribeIdList().contains(member.getPrincipalId()));
                        }
                        if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER.name())) {
                            Set<String> userIds = StreamEx.of(
                                    projectMemberInspectService.getTeamMemberId(project.getTeamOwnerId(), keyword)
                            )
                                    .map(UserProto.User::getId)
                                    .map(String::valueOf)
                                    .toSet();
                            return StreamEx.of(entry.getValue())
                                    .filter(member -> userIds.contains(member.getPrincipalId()));
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
        return members;
    }

    public Set<Integer> filterQueryParameterMembers(Project project, Integer roleId, String keyword) {
        List<ProjectMember> members = projectMemberInspectService.findListByProjectId(project.getId());
        if (CollectionUtils.isEmpty(members)) {
            return new HashSet<>();
        }
        Set<Integer> grantMemberUserIds = projectMemberInspectService.getPrincipalMemberUserIds(members);
        if (Objects.nonNull(roleId) && roleId > 0) {
            try {
                List<Integer> userIds = advancedRoleServiceGrpcClient.findUsersOfRole(AclProto.Role.newBuilder().setId(roleId).build());
                grantMemberUserIds = StreamEx.of(grantMemberUserIds)
                        .filter(userIds::contains)
                        .toSet();
            } catch (Exception e) {
                log.error("advancedRoleServiceGrpcClient findUsersOfRole Exception Error, projectId = {}, roleId = {}",
                        project.getId(),
                        roleId);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            Set<Integer> userIds = StreamEx.of(
                    projectMemberInspectService.getTeamMemberId(project.getTeamOwnerId(), keyword)
            )
                    .map(UserProto.User::getId)
                    .toSet();
            grantMemberUserIds = StreamEx.of(grantMemberUserIds)
                    .filter(userIds::contains)
                    .toSet();
        }
        return grantMemberUserIds;
    }

    /**
     * 校验添加数据团队内是否存在
     */
    public List<ProjectMemberAddReqDTO> checkAddProjectMember(Integer teamId, Integer operatorId, List<ProjectMemberAddReqDTO> reqDTOs) {
        return StreamEx.of(reqDTOs)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(principal -> principal.getPrincipalType()
                                + ":" + principal.getPrincipalId()))),
                        ArrayList::new))
                .stream()
                .collect(groupingBy(ProjectMemberAddReqDTO::getPrincipalType))
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    //待添加授权体
                    Set<String> addPrincipalIds = StreamEx.of(entry.getValue())
                            .map(ProjectMemberAddReqDTO::getPrincipalId)
                            .toSet();
                    if (org.springframework.util.CollectionUtils.isEmpty(addPrincipalIds)) {
                        return null;
                    }
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER_GROUP)) {
                        Set<Long> groupIds = StreamEx.of(addPrincipalIds).map(Long::valueOf).toSet();
                        List<String> userGroupIds = projectMemberInspectService.getUserGroups(operatorId, groupIds)
                                .stream()
                                .filter(Objects::nonNull)
                                .map(UserGroupResponseDTO::getId)
                                .map(String::valueOf)
                                .collect(toList());
                        return StreamEx.of(entry.getValue())
                                .filter(dto -> userGroupIds.contains(dto.getPrincipalId()));
                    }
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.DEPARTMENT)) {
                        GrantObjectProto.GetGrantObjectNamesResponse response =
                                platformGrantObjectGRpcClient.getGrantObjectNames(entry.getKey().name(), addPrincipalIds);
                        Set<String> departmentIds = response.getGrantObjectIdNameMap()
                                .entrySet()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(Map.Entry::getKey)
                                .collect(toSet());
                        return StreamEx.of(entry.getValue())
                                .filter(dto -> departmentIds.contains(dto.getPrincipalId()));
                    }
                    if (entry.getKey().equals(ProjectMemberPrincipalTypeEnum.USER)) {
                        List<Integer> ids = StreamEx.of(addPrincipalIds).map(Integer::valueOf).toList();
                        UserProto.FindUserResponse response = userServiceGrpcClient.findUserByIds(ids);
                        //可添加用户授权体
                        Set<String> userIds = StreamEx.of(response.getDataList())
                                .nonNull()
                                .filter(user -> user.getTeamId() == teamId)
                                .map(UserProto.User::getId)
                                .map(String::valueOf)
                                .toSet();
                        return StreamEx.of(entry.getValue())
                                .filter(dto -> userIds.contains(dto.getPrincipalId()));
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
