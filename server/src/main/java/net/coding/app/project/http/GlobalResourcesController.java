package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.util.Result;
import net.coding.e.grpcClient.collaboration.IssueGrpcClient;
import net.coding.e.grpcClient.collaboration.dto.Issue;
import net.coding.e.grpcClient.collaboration.exception.IssueNotException;
import net.coding.exchange.dto.team.Team;
import net.coding.grpc.client.platform.TeamServiceGrpcClient;
import net.coding.lib.project.dto.ResourceDTO;
import net.coding.lib.project.dto.ResourceDetailDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.enums.ResourceTypeEnum;
import net.coding.lib.project.enums.ScopeTypeEnum;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.ProjectGrpcClient;
import net.coding.lib.project.service.ProjectResourceLinkService;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ProjectService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.exception.CoreException.ExceptionType.TEAM_NOT_EXIST;

@RestController
@Slf4j
@ProtectedAPI
@RequestMapping("/api/platform")
public class GlobalResourcesController {

    @Autowired
    private ProjectResourceService projectResourceService;

    @Autowired
    private ProjectResourceLinkService projectResourceLinkService;

    @Autowired
    private ProjectGrpcClient projectGrpcClient;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamServiceGrpcClient teamServiceGrpcClient;

    @Autowired
    private IssueGrpcClient issueGrpcClient;

    /**
     * @Description: 输入 Space + # 或 Space + 项目GK + # 时呼出资源搜索菜单；按 Esc 取消菜单。 -
     * 在任意位置关联资源时，若指定项目ID则搜索指定项目资源+全局资源； - 在项目内页面关联资源时，如未指定项目则搜索当前项目资源 + 全局资源； -
     * 在非项目内页面关联资源时，如未指定项目则仅搜索全局资源。
     * <p>
     * 搜索结果按以下优先顺序显示，最多显示前 8 个结果： 1. 精确匹配资源编号的资源； 2. 资源编号以关键词开头的资源； 3. 资源编号包含关键词的资源； 4. 标题包含关键词的资源。
     * 以上同一分类中若包含多个符合匹配规则的资源，则按创建时间倒序展示。
     * @Author: Jyong <jiangyong@coding.net>
     * @Date: 2021/8/21
     */
    @GetMapping("/search/all-resource")
    public Result findResourceList(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> targetTypes,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String enterProjectGK,
            @RequestParam(required = false) String innerProjectGK
    ) throws CoreException {
        Team team = teamServiceGrpcClient.getTeam(teamId);
        if (team == null) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 8;
        }
        List<ResourceDTO> projectResourceList = new ArrayList<>();
        if (enterProjectGK != null) {
            //优先查询 输入gk 的项目资源
            Project project = projectService.getByNameAndTeamId(enterProjectGK, teamId);
            if (project != null) {
                // 1.根据 resourceId 匹配项目资源
                List<ProjectResource> infoWithResourceId = projectResourceService.findResourceList(project.getId(), keyword, null, targetTypes, pageSize, ScopeTypeEnum.PROJECT.value());
                if (infoWithResourceId != null && infoWithResourceId.size() > 0) {
                    projectResourceList.addAll(infoWithResourceId.stream().distinct().map(item -> new ResourceDTO(item, enterProjectGK)).collect(Collectors.toList()));
                }
                if (projectResourceList.size() == 8) {
                    projectResourceList = dealAgileResource(projectResourceList);
                    return Result.success(projectResourceList);

                }
                //2.根据title 匹配项目资源
                List<ProjectResource> infoWithTitle = projectResourceService.findResourceList(project.getId(), null, keyword, targetTypes, pageSize - projectResourceList.size(), ScopeTypeEnum.PROJECT.value());
                if (infoWithTitle != null && infoWithTitle.size() > 0) {
                    projectResourceList.addAll(infoWithTitle.stream().distinct().map(item -> new ResourceDTO(item, enterProjectGK)).collect(Collectors.toList()));
                    projectResourceList = projectResourceList.stream().distinct().collect(Collectors.toList());
                }
                if (projectResourceList.size() == 8) {
                    projectResourceList = dealAgileResource(projectResourceList);
                    return Result.success(projectResourceList);
                }
            }
        }

        if (innerProjectGK != null) {
            //查询 本项目资源
            Project project = projectService.getByNameAndTeamId(innerProjectGK, teamId);
            if (project != null) {
                // 3.根据 resourceId 匹配项目资源
                List<ProjectResource> infoWithResourceId = projectResourceService.findResourceList(project.getId(), keyword, null, targetTypes, pageSize - projectResourceList.size(), ScopeTypeEnum.PROJECT.value());
                if (infoWithResourceId != null && infoWithResourceId.size() > 0) {
                    projectResourceList.addAll(infoWithResourceId.stream().distinct().map(item -> new ResourceDTO(item, innerProjectGK)).collect(Collectors.toList()));
                    projectResourceList = projectResourceList.stream().distinct().collect(Collectors.toList());
                }
                if (projectResourceList.size() == 8) {
                    projectResourceList = dealAgileResource(projectResourceList);
                    return Result.success(projectResourceList);
                }
                //4.根据title 匹配项目资源
                List<ProjectResource> infoWithTitle = projectResourceService.findResourceList(project.getId(), null, keyword, targetTypes, pageSize - projectResourceList.size(), ScopeTypeEnum.PROJECT.value());
                if (infoWithTitle != null && infoWithTitle.size() > 0) {
                    projectResourceList.addAll(infoWithTitle.stream().distinct().map(item -> new ResourceDTO(item, innerProjectGK)).collect(Collectors.toList()));
                    projectResourceList = projectResourceList.stream().distinct().collect(Collectors.toList());
                }
                if (projectResourceList.size() == 8) {
                    projectResourceList = dealAgileResource(projectResourceList);
                    return Result.success(projectResourceList);
                }
            }
        }

        //5.resourceId 匹配全局资源
        List<ProjectResource> globalInfoWithResourceId = projectResourceService.findResourceList(teamId, keyword, null, targetTypes, pageSize - projectResourceList.size(), ScopeTypeEnum.TEAM.value());
        if (globalInfoWithResourceId != null && globalInfoWithResourceId.size() > 0) {
            projectResourceList.addAll(globalInfoWithResourceId.stream().distinct().map(item -> new ResourceDTO(item, null)).collect(Collectors.toList()));
            projectResourceList = projectResourceList.stream().distinct().collect(Collectors.toList());
        }
        if (projectResourceList.size() == 8) {
            projectResourceList = dealAgileResource(projectResourceList);
            return Result.success(projectResourceList);
        }
        //6.根据title 匹配全局资源
        List<ProjectResource> globalInfoWithTitle = projectResourceService.findResourceList(teamId, null, keyword, targetTypes, pageSize - projectResourceList.size(), ScopeTypeEnum.TEAM.value());
        if (globalInfoWithTitle != null && globalInfoWithTitle.size() > 0) {
            projectResourceList.addAll(globalInfoWithTitle.stream().distinct().map(item -> new ResourceDTO(item, null)).collect(Collectors.toList()));
            projectResourceList = projectResourceList.stream().distinct().collect(Collectors.toList());
        }
        projectResourceList = dealAgileResource(projectResourceList);
        return Result.success(projectResourceList);
    }

    private List<ResourceDTO> dealAgileResource(List<ResourceDTO> projectResourceList) {
        for (ResourceDTO projectResource : projectResourceList) {
            if (ResourceTypeEnum.Defect.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.Epic.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.Requirement.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.Mission.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.SubTask.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.WorkItem.getType().equalsIgnoreCase(projectResource.getTargetType())
                    || ResourceTypeEnum.Risk.getType().equalsIgnoreCase(projectResource.getTargetType())
            ) {
                try {
                    Issue issueResponse = issueGrpcClient.getIssueById(projectResource.getTargetId(), false);
                    projectResource.setTargetType(issueResponse.getIssueTypeDetail().getIconType());
                } catch (IssueNotException e) {
                    continue;
                }
            }
        }

        return projectResourceList;
    }

    @GetMapping("/search/resource/detail")
    public Result findResourceDetail(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestParam(required = true) String code,
            @RequestParam(required = false) String projectGK
    ) throws CoreException {
        Team team = teamServiceGrpcClient.getTeam(teamId);
        if (team == null) {
            throw CoreException.of(TEAM_NOT_EXIST);
        }
        if (projectGK != null) {
            //项目资源
            Project project = projectService.getByNameAndTeamId(projectGK, teamId);
            if (project == null) {
                return Result.success(null);
            }
            ProjectResource projectResource = projectResourceService.findProjectResourceDetail(project.getId(), code, ScopeTypeEnum.PROJECT.value());
            if (projectResource == null) {
                return Result.success(null);
            }
            ResourceDetailDTO resourceDetailDTO = new ResourceDetailDTO(projectResource, projectGK);
            resourceDetailDTO.setScopeName(project.getDisplayName());
            resourceDetailDTO.setScopeAvatar(project.getIcon());
            resourceDetailDTO = dealAgileResourceDetail(resourceDetailDTO);
            return Result.success(resourceDetailDTO);
        }
        // 全局资源
        ProjectResource projectResource = projectResourceService.findProjectResourceDetail(team.getId(), code, ScopeTypeEnum.TEAM.value());
        if (projectResource == null) {
            return Result.success(null);
        }
        ResourceDetailDTO resourceDetailDTO = new ResourceDetailDTO(projectResource, null);
        resourceDetailDTO.setScopeName(team.getName());
        resourceDetailDTO.setScopeAvatar(team.getAvatar());
        resourceDetailDTO = dealAgileResourceDetail(resourceDetailDTO);
        return Result.success(resourceDetailDTO);
    }

    private ResourceDetailDTO dealAgileResourceDetail(ResourceDetailDTO projectResource) {
        if (ResourceTypeEnum.Defect.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.Epic.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.Requirement.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.Mission.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.SubTask.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.WorkItem.getType().equalsIgnoreCase(projectResource.getTargetType())
                || ResourceTypeEnum.Risk.getType().equalsIgnoreCase(projectResource.getTargetType())
        ) {
            try {
                Issue issueResponse = issueGrpcClient.getIssueById(projectResource.getTargetId(), false);
                projectResource.setTargetType(issueResponse.getIssueTypeDetail().getIconType());
            } catch (IssueNotException e) {
                return projectResource;
            }
        }
        return projectResource;
    }

}
