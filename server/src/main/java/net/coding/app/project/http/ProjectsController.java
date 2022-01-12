package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.service.ProjectService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Api(value = "项目查询", tags = "项目查询")
@AllArgsConstructor
@RequestMapping("/api/platform/project/projects")
public class ProjectsController {
    private final ProjectService projectService;

    @ApiOperation("项目-分页查询")
    @GetMapping("search")
    public ResultPage<ProjectDTO> search(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam("查询类型(ALL,JOINED,MANAGED,ARCHIVED)")
            @RequestParam(required = false, defaultValue = "JOINED") String type,
            @ApiParam("是否归档")
            @RequestParam(required = false, defaultValue = "false") boolean archived,
            @ApiParam("关键词")
            @RequestParam(required = false) String keyword,
            @ApiParam("分组")
            @RequestParam(required = false) Integer groupId,
            @ApiParam("排序字段（VISIT,CREATE,START,NAME,ARCHIVED）")
            @RequestParam(required = false) String sort,
            @ApiParam("排序规则（ASC,DESC)")
            @RequestParam(required = false) String order,
            @ApiParam("分页") LimitedPager pager
    ) throws CoreException {
        return projectService.getProjectPages(
                ProjectPageQueryParameter.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .groupId(groupId)
                        .keyword(keyword)
                        .queryType(type)
                        .sortKey(sort)
                        .sortValue(order)
                        .deletedAt(archived ? BeanUtils.getDefaultArchivedAt()
                                : BeanUtils.getDefaultDeletedAt())
                        .page(pager.getPage())
                        .pageSize(pager.getPageSize())
                        .build()
        );
    }
}
