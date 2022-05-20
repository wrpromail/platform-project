package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.exchange.dto.user.User;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.grpc.client.platform.UserServiceGrpcClient;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectRecentViewService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@RestController
@Api(value = "最近访问项目查询", tags = "最近访问项目查询")
@AllArgsConstructor
@RequestMapping("/api/platform/project/recent/views")
@RestfulApi
public class ProjectRecentViewController {

    private final UserServiceGrpcClient userServiceGrpcClient;

    private final ProjectRecentViewService projectRecentViewService;

    @ApiOperation("项目-最近访问项目查询")
    @GetMapping("/search")
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    public List<ProjectDTO> queryRecentViews(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(required = false) String function,
            @RequestParam(required = false) String action,
            @RequestParam(value = "pmType", defaultValue = "PROJECT") PmTypeEnums pmType
    ) {
        return projectRecentViewService.getProjectRecentViews(teamId, userId, function, action, pmType.getType(), FALSE);
    }

    @ApiOperation("项目-最近访问项目查询-指定用户")
    @GetMapping("/{userId}")
    @EnterpriseApiProtector(function = Function.EnterpriseTeamMeasurement, action = Action.View)
    public List<ProjectDTO> queryRecentViewsByUserId(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "pmType", defaultValue = "PROJECT") PmTypeEnums pmType
    ) throws CoreException {
        User user = userServiceGrpcClient.getUser(userId);
        if (Objects.isNull(user) || !user.getTeam_id().equals(teamId)) {
            throw CoreException.of(USER_NOT_EXISTS);
        }
        return projectRecentViewService.getProjectRecentViews(teamId, userId, EMPTY, EMPTY, pmType.getType(), TRUE);
    }
}
