package net.coding.lib.project.additional;

import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.permission.AdvancedRoleServiceGrpcClient;
import net.coding.lib.project.additional.dto.ProjectAdditionalDTO;
import net.coding.lib.project.additional.dto.ProjectMemberDTO;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.dao.TeamProjectDao;
import net.coding.lib.project.group.ProjectGroupDTO;
import net.coding.lib.project.group.ProjectGroupDTOService;
import net.coding.lib.project.group.ProjectGroupService;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.setting.ProjectSettingFunctionService;
import net.coding.platform.permission.api.RoleType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.acl.AclProto;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectAdditionalService {
    private final ProjectSettingFunctionService projectSettingFunctionService;
    private final AdvancedRoleServiceGrpcClient advancedRoleServiceGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final TeamProjectDao teamProjectDao;
    private final ProjectMemberDao projectMemberDao;
    private final ProjectGroupService projectGroupService;
    private final ProjectGroupDTOService projectGroupDTOService;

    public Map<Integer, ProjectAdditionalDTO> getWithFunctionAndAdmin(
            Integer teamId,
            Integer userId,
            Set<Integer> projects,
            ProjectAdditionalPredicate predicate
    ) {
        return Optional.ofNullable(projects)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .filter(p ->
                        teamProjectDao.existByTeamIdAndProjectId(
                                teamId,
                                p,
                                BeanUtils.getDefaultDeletedAt(),
                                BeanUtils.getDefaultArchivedAt()
                        )
                )
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                p -> ProjectAdditionalDTO
                                        .builder()
                                        .function(
                                                predicate.withFunction() ?
                                                        projectSettingFunctionService.getFunctions(p) :
                                                        Collections.emptyList()
                                        )
                                        .admin(
                                                predicate.withAdmin() ? findAdmin(p) :
                                                        Collections.emptyList()
                                        )
                                        .memberCount(
                                                predicate.withMemberCount() ?
                                                        projectMemberDao.countByProjectId(p)
                                                        : 0L
                                        )
                                        .group(
                                                predicate.withGroup() ?
                                                        Optional.ofNullable(projectGroupDTOService.toDTO(
                                                                projectGroupService.getByProjectAndUser(p, userId),
                                                                null
                                                        )).orElse(ProjectGroupDTO.builder().build())
                                                        : ProjectGroupDTO.builder().build()
                                        )
                                        .build(),
                                (a, b) -> a
                        )
                );
    }

    private List<ProjectMemberDTO> findAdmin(Integer project) {
        try {
            AclProto.Role adminRole = advancedRoleServiceGrpcClient.findProjectRoles(project)
                    .stream()
                    .filter(r -> StringUtils.equals(r.getType(), RoleType.ProjectAdmin.name()))
                    .findFirst()
                    .orElse(null);
            if (adminRole == null) {
                return Collections.emptyList();
            }
            List<Integer> adminUsers = advancedRoleServiceGrpcClient.findUsersOfRole(adminRole);
            return Optional.ofNullable(userGrpcClient.findUserByIds(adminUsers))
                    .map(Collection::stream)
                    .orElse(Stream.empty())
                    .map(user -> ProjectMemberDTO.builder()
                            .id(user.getId())
                            .avatar(user.getAvatar())
                            .namePinyin(user.getNamePinyin())
                            .name(user.getName())
                            .build()
                    ).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Find project {} admin failure, cause of {}", project, e.getMessage());
        }
        return Collections.emptyList();
    }


    /**
     * 查询项目管理员
     *
     * @param project 项目id
     * @return Map<Integer: 项目id, List < Integer> : 管理员userId>
     */
    public Map<Integer, List<Integer>> findProjectAdmin(List<Integer> project) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        project.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(p -> {
                    AclProto.Role adminRole;
                    try {
                        adminRole = advancedRoleServiceGrpcClient.findProjectRoles(p)
                                .stream()
                                .filter(r -> StringUtils.equals(r.getType(), RoleType.ProjectAdmin.name()))
                                .findFirst()
                                .orElse(null);
                        List<Integer> usersOfRole = advancedRoleServiceGrpcClient.findUsersOfRole(adminRole);
                        map.put(p, usersOfRole);
                    } catch (Exception e) {
                        log.warn("Find project admin user error", e);
                    }
                });
        return map;
    }
}
