package net.coding.app.project.http;

import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.common.constants.TwoFactorAuthConstants;
import net.coding.common.util.Result;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.dto.DeployTokenScopeDTO;
import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddDeployTokenForm;
import net.coding.lib.project.service.DeployTokenService;

import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import proto.platform.user.UserProto;

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

    @ProtectedAPI
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.Create)
    public Result addDeployToken(
            @PathVariable Integer projectId,
            @Valid AddDeployTokenForm form
    ) throws CoreException {

        deployTokenService.validateCreateForm(form);
        UserProto.User user = SystemContextHolder.get();
        if (!Objects.nonNull(user)) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        return Result.success(deployTokenService.addDeployToken(projectId, user, form, (short) 0));
    }

    @ProtectedAPI
    @RequestMapping(value = "/{id}/scope", method = RequestMethod.PUT)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.Update)
    public boolean modifyDeployTokenScope(
            @PathVariable Integer projectId,
            @PathVariable Integer id,
            @ModelAttribute AddDeployTokenForm form,
            Errors errors
    ) throws CoreException {
        deployTokenService.validateUpdateForm(form);
        if (errors.hasErrors()) {
            throw CoreException.of(errors);
        }
        return deployTokenService.modifyDeployTokenScope(projectId, id, form);
    }


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
     * 删除令牌
     */
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_DEFAULT)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.Delete)
    public boolean deleteDeployToken(
            @PathVariable Integer projectId,
            @PathVariable Integer id
    ) throws CoreException {
        return deployTokenService.deleteDeployToken(projectId, id);
    }

    /**
     * 禁用令牌
     */
    @ProtectedAPI
    @RequestMapping(value = "/{id}/isEnable", method = RequestMethod.PUT)
    @ProjectApiProtector(function = Function.ProjectDeployToken, action = Action.Update)
    public boolean isEnable(
            @PathVariable Integer projectId,
            @PathVariable Integer id,
            boolean enableFlag
    ) throws CoreException {
        return deployTokenService.enableDeployToken(projectId, id, enableFlag);
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
                .map(e -> DeployTokenScopeDTO.builder().value(e.getValue()).text(e.getText()).build())
                .collect(Collectors.toList()));
    }

}
