package net.coding.app.project.http;

import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.common.constants.TwoFactorAuthConstants;
import net.coding.common.util.Result;
import net.coding.lib.project.dto.DeployTokenDepotDTO;
import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.DeployTokenService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;

/**
 * @Author liuying
 * @Date 2021/1/7 10:43 上午
 * @Version 1.0
 */
@ResponseBody
@RestController
@Api(value = "项目令牌", tags = "项目令牌")
@AllArgsConstructor
@RequestMapping(value = "/api/platform/project/{projectId}/deploy-tokens")
public class DeployTokenController {

    private final DeployTokenService deployTokenService;


    /**
     * 获取部署令牌列表
     */
    @ApiOperation(value = "getDeployTokens", notes = "获取部署令牌列表")
    @ProtectedAPI
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.View)
    public Result getDeployTokens(@PathVariable Integer projectId) {
        return Result.success(deployTokenService.getUserDeployTokens(projectId));
    }

    /**
     * 获取令牌密码
     */
    @ApiOperation(value = "getDeployTokenPassword", notes = "获取令牌密码")
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_DEFAULT)
    @RequestMapping(value = "/{id}/token", method = RequestMethod.GET)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.View)
    public Result getToken(@PathVariable Integer projectId, @PathVariable Integer id
    ) throws CoreException {
        DeployTokens deployToken = deployTokenService.getDeployToken(projectId, id);
        return Result.success(deployToken.getToken());
    }

    /**
     * 获取权限列表
     */
    @ApiOperation(value = "getScopes", notes = "获取权限列表")
    @RequestMapping(value = "/scopes", method = RequestMethod.GET)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.View)
    public Result getScopes() {
        return Result.success(Stream.of(DeployTokenScopeEnum.values())
                .map(e -> DeployTokenDepotDTO.builder().depotId(e.getValue()).scope(e.getText()).build())
                .collect(Collectors.toList()));
    }

}
