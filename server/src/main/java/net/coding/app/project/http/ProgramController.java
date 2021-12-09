package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.LimitedPager;
import net.coding.common.util.Result;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dto.ProgramDTO;
import net.coding.lib.project.dto.ProgramPathDTO;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.dto.ProjectUserDTO;
import net.coding.lib.project.dto.request.ProjectMemberReqDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.CreateProgramForm;
import net.coding.lib.project.form.QueryProgramForm;
import net.coding.lib.project.parameter.ProgramPageQueryParameter;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.service.ProgramMemberService;
import net.coding.lib.project.service.ProgramService;
import net.coding.lib.project.service.RamTransformTeamService;
import net.coding.lib.project.service.member.ProgramMemberPrincipalService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;

@Api(value = "项目集", tags = "项目集")
@RestController
@AllArgsConstructor
@RequestMapping("/api/platform/program")
public class ProgramController {

    private final RamTransformTeamService ramTransformTeamService;

    private final ProgramService programService;

    private final ProgramMemberService programMemberService;

    private final ProgramMemberPrincipalService programMemberPrincipalService;

    @ApiOperation("项目集-创建")
    @EnterpriseApiProtector(function = Function.EnterpriseProgram, action = Action.Create)
    @PostMapping("/create")
    public ProgramPathDTO createProgram(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestBody @Valid CreateProgramForm form
    ) throws Exception {
        return programService.createProgram(teamId, userId, form);
    }


    @ApiOperation("项目集-分页查询")
    @GetMapping("/search")
    public ResultPage<ProgramDTO> search(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @ApiParam(value = "查询类型（ALL,JOINED,MANAGED）") @RequestParam(required = false, defaultValue = "JOINED") String type,
            @ApiParam(value = "状态") @RequestParam(required = false) boolean archived,
            @ApiParam(value = "负责人") @RequestParam(required = false) Set<Integer> user,
            @ApiParam(value = "涉及项目") @RequestParam(required = false) Set<Integer> project,
            @ApiParam(value = "开始时间") @RequestParam(required = false) String start,
            @ApiParam(value = "结束时间") @RequestParam(required = false) String end,
            @ApiParam(value = "项目集关键字（名称/标识/拼音）") @RequestParam(required = false) String keyword,
            @ApiParam(value = "排序字段（VISIT,CREATE,START,NAME）") @RequestParam(required = false, defaultValue = "CREATE") String sort,
            @ApiParam(value = "排序规则（ASC,DESC）") @RequestParam(required = false, defaultValue = "DESC") String order,
            LimitedPager pager
    ) throws CoreException {
        return programService.getProgramPages(
                ProgramPageQueryParameter.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .userIds(user)
                        .projectIds(project)
                        .startDate(start)
                        .endDate(end)
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

    @ApiOperation("项目集-分页查询（废弃）")
    @PostMapping("/pages")
    @Deprecated
    public ResultPage<ProgramDTO> queryProgramPages(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestBody @Valid QueryProgramForm form
    ) throws CoreException {
        return programService.getProgramPages(ProgramPageQueryParameter.builder()
                .teamId(teamId)
                .userId(userId)
                .userIds(form.getUserIds())
                .projectIds(form.getProjectIds())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .keyword(form.getKeyword())
                .queryType(form.getQueryType().name())
                .sortKey(form.getSortBy().getSortKey().name())
                .sortValue(form.getSortBy().getSortValue().name())
                .deletedAt(form.getArchived() ? BeanUtils.getDefaultArchivedAt()
                        : BeanUtils.getDefaultDeletedAt())
                .page(form.getPage())
                .pageSize(form.getPageSize())
                .build());
    }

    @ApiOperation("项目集-查询全部项目集")
    @GetMapping("/list")
    public List<ProgramDTO> queryPrograms(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(name = "projectId", required = false) Integer projectId
    ) throws CoreException {
        return programService.getProgramDTOs(teamId, projectId, userId);
    }

    @ApiOperation("项目集-查询关联项目(涉及所有我参与的项目集下关联项目)")
    @GetMapping("/projects")
    public List<ProjectDTO> getProjectsByProgram(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(defaultValue = "JOINED") String type,
            @RequestParam(defaultValue = "false") Boolean archived
    ) throws CoreException {
        return programService.getProgramAllProjects(ProgramProjectQueryParameter.builder()
                .teamId(teamId)
                .userId(userId)
                .queryType(type)
                .deletedAt(archived ? BeanUtils.getDefaultArchivedAt()
                        : BeanUtils.getDefaultDeletedAt())
                .build());
    }

    @Deprecated
    @ApiOperation("项目集-查询关联项目(涉及所有我参与的项目集下关联项目)")
    @PostMapping("/projects")
    public List<ProjectDTO> queryProgramAllProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(defaultValue = "JOINED") QueryProgramForm.QueryType queryType,
            @RequestParam(defaultValue = "false") Boolean archived) throws CoreException {
        return programService.getProgramAllProjects(ProgramProjectQueryParameter.builder()
                .teamId(teamId)
                .userId(userId)
                .queryType(queryType.name())
                .deletedAt(archived ? BeanUtils.getDefaultArchivedAt()
                        : BeanUtils.getDefaultDeletedAt())
                .build());
    }

    @ApiOperation("项目集-项目集下关联项目列表(单个项目集下关联项目)")
    @ProtectedAPI
    @GetMapping("/{programId}/projects")
    public List<ProjectDTO> queryProgramProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer programId,//项目集Id
            @RequestParam(defaultValue = "false") Boolean queryJoined
    ) throws CoreException {
        return programService.getProgramProjectDTOs(teamId, userId, programId, queryJoined);
    }

    @Deprecated
    @ApiOperation("项目集-成员所在项目集关联项目列表")
    @ProtectedAPI
    @GetMapping("/batch/user/projects")
    public List<ProjectUserDTO> queryBatchUserProgramProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(name = "programId", required = false) Integer programId,
            @RequestParam(name = "userIds", required = false)
                    Set<Integer> userIds) throws CoreException {
        return programService.getBatchUserProgramProjects(teamId, userId, programId, userIds);
    }

    @ApiOperation("项目集-主体所在项目集关联项目列表")
    @ProtectedAPI
    @PostMapping("/{projectId}/principal/projects")
    public List<ProjectUserDTO> queryBatchPrincipalProgramProjects(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId,//项目集Id
            @RequestBody @Valid List<ProjectMemberReqDTO> principals)
            throws CoreException {
        return programService.getBatchPrincipalProgramProjects(teamId, userId, projectId, principals);
    }

    @ApiOperation("项目集-添加项目/项目集管理员")
    @ProjectApiProtector(function = Function.ProgramProject, action = Action.Create)
    @PostMapping("/{projectId}/add/project")
    public ProgramPathDTO addProgramProject(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId,//项目集Id
            @ApiParam("协作项目") @RequestParam(required = false) Set<Integer> projectIds,
            @ApiParam("管理员") @RequestParam(required = false) Set<Integer> userIds
    ) throws Exception {
        if (ramTransformTeamService.ramOnline(teamId)) {
            return programService.addProgramProjectPrincipal(teamId, userId, projectId, projectIds, userIds);
        } else {
            return programService.addProgramProject(teamId, userId, projectId, projectIds, userIds);
        }
    }

    @ApiOperation("项目集-移除项目")
    @ProjectApiProtector(function = Function.ProgramProject, action = Action.Delete)
    @PostMapping("/{projectId}/remove/project")
    public Result deleteProgramProject(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @PathVariable Integer projectId,//项目集Id
            @ApiParam("项目Id") @RequestParam(required = false) Integer removeProjectId) throws CoreException {
        if (ramTransformTeamService.ramOnline(teamId)) {
            programMemberPrincipalService.removeProgramProject(teamId, userId, projectId, removeProjectId);
        } else {
            programMemberService.removeProgramProject(teamId, userId, projectId, removeProjectId);
        }
        return Result.success();
    }
}
