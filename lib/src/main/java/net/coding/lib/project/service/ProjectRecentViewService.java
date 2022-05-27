package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.grpc.client.permission.AclServiceGrpcClient;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectPinDao;
import net.coding.lib.project.dao.ProjectRecentViewDao;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectPin;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberInspectService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.acl.AclProto;
import proto.platform.permission.PermissionProto;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectRecentViewService {
    private final ProjectDao projectDao;
    private final ProjectPinDao projectPinDao;
    private final ProjectRecentViewDao projectRecentViewDao;
    private final ProjectDTOService projectDTOService;
    private final AclServiceGrpcClient aclServiceGrpcClient;
    private final ProjectMemberInspectService projectMemberInspectService;
    /**
     * 最近访问项目列表
     */
    public List<ProjectDTO> getProjectRecentViews(Integer teamId,
                                                  Integer userId,
                                                  String function,
                                                  String action,
                                                  Integer pmType,
                                                  Boolean noPin) {
        //我参与的项目
        Set<Integer> joinedProjectIds = projectMemberInspectService.getJoinedProjectIds(
                ProjectMemberPrincipalQueryParameter.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .pmType(pmType)
                        .deletedAt(BeanUtils.getDefaultDeletedAt())
                        .build());
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        //最近访问项目
        List<Project> recentViewProjects = projectRecentViewDao.getProjectRecentViews(userId, joinedProjectIds);
        if (CollectionUtils.isEmpty(recentViewProjects)) {
            return Collections.emptyList();
        }
        Set<Integer> recentViewProjectIds = StreamEx.of(recentViewProjects)
                .map(Project::getId)
                .toSet();
        List<Integer> ids = StreamEx.of(joinedProjectIds)
                .filter(id -> !recentViewProjectIds.contains(id))
                .collect(Collectors.toList());
        if (StringUtils.isNotBlank(function) && StringUtils.isNotBlank(action)) {
            try {
                Set<Integer> projectIds = aclServiceGrpcClient.getAccessProjectIdList(
                        StreamEx.of(PermissionProto.Permission.newBuilder()
                                .setFunction(PermissionProto.Function.valueOf(function))
                                .setAction(PermissionProto.Action.valueOf(action))
                                .build()
                        ).toList(),
                        userId
                )
                        .stream()
                        .map(AclProto.UserAclProjects::getProjectIdList)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                ids = StreamEx.of(projectIds)
                        .filter(id -> !recentViewProjectIds.contains(id))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("aclServiceGrpcClient getAccessProjectIdList error, userId = {}, function = {}, action = {}",
                        userId, function, action);
            }
        }
        //取最近查看的项目列表 id 和用户所有项目 id 合并，查询全部数据，最近访问访问顺序排前
        if (CollectionUtils.isNotEmpty(ids)) {
            recentViewProjects.addAll(projectDao.getByIds(ids, BeanUtils.getDefaultDeletedAt()));
        }
        List<ProjectDTO> projectDTOS = StreamEx.of(recentViewProjects)
                .map(projectDTOService::toDetailDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (noPin) {
            return projectDTOS;
        }
        List<ProjectPin> pins = projectPinDao.select(ProjectPin.builder()
                .userId(userId)
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build());
        return StreamEx.of(projectDTOS)
                .peek(p -> {
                    Optional<ProjectPin> pin = StreamEx.of(pins)
                            .filter(pp -> p.getId().equals(pp.getProjectId()))
                            .findFirst();
                    p.setPin(pin.isPresent());
                    p.setPinSort(pin.map(ProjectPin::getSort).orElse(0));
                })
                .collect(Collectors.toList());
    }
}
