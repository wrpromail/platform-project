package net.coding.app.project.http;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.Result;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.enums.CredentialTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.AndroidCredentialForm;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.form.credential.TencentServerlessCredentialForm;
import net.coding.lib.project.pager.PagerResolve;
import net.coding.lib.project.service.credential.ProjectCredentialService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ResponseBody
@Api(value = "项目凭据", tags = "项目凭据")
@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping(value = "/api/platform/project/{projectId}/credentials")
public class CredentialController {
    public static final String AUTH_TYPE_DEFAULT = "default";
    private final ProjectCredentialService projectCredentialService;

    @ApiOperation(value = "queryList", notes = "项目凭据列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true)
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Result queryList(
            @PathVariable(value = "projectId") Integer projectId,
            @RequestParam(required = false) String type,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {
        CredentialTypeEnums credentialType = null;
        if (StringUtils.isNotBlank(type)) {
            credentialType = CredentialTypeEnums.valueOf(type);
        }
        return Result.success(projectCredentialService.list(projectId, credentialType, pager));
    }

    @ApiOperation(value = "delete", notes = "删除项目凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true)
    })
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public Result delete(
            @PathVariable(value = "projectId") Integer projectId,
            @PathVariable("id") int id
    ) throws CoreException {
        if (SystemContextHolder.get() == null) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        projectCredentialService.delete(id, projectId);
        return Result.success();
    }

    @ApiOperation(value = "get", notes = "查询项目凭据详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true)
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Result get(
            @PathVariable(value = "projectId") Integer projectId,
            @PathVariable("id") int id
    ) throws CoreException {
        return Result.success(projectCredentialService.getCredential(projectId, id));
    }

    @ApiOperation(value = "createCredential", notes = "创建项目凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "CredentialForm.class", required = true)
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Result createCredential(
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody CredentialForm form
    ) throws CoreException {
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "createAndroidCert", notes = "创建项目 android 凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "AndroidCredentialForm.class", required = true)
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/androidCredential", method = RequestMethod.POST)
    public Result createAndroidCert(
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody AndroidCredentialForm form
    ) throws Exception {
        if (form.getFilePassword().getBytes().length > 80) {
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_PASSWORD_BYTES_TOO_LONG);
        }
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "createServerlessCert", notes = "创建项目腾讯云凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "TencentServerlessCredentialForm.class", required = true)
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/tencentServerlessCredential", method = RequestMethod.POST)
    public Result createServerlessCert(
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody TencentServerlessCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "updateCredential", notes = "更新项目凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "CredentialForm.class")
    })
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Result updateWithTasks(
            @PathVariable("id") int id,
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody CredentialForm form
    ) throws CoreException {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "updateAndroidCert", notes = "更新项目 android 凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "AndroidCredentialForm.class", required = true)
    })
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}/androidCredential", method = RequestMethod.PUT)
    public Result updateAndroidCert(
            @PathVariable("id") int id,
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody AndroidCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "updateTencentServerlessCredential", notes = "更新项目 android 凭据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "form", value = "凭据表单", paramType = "TencentServerlessCredentialForm.class", required = true)
    })
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}/serverlessCredential", method = RequestMethod.PUT)
    public Result updateTencentServerlessCredential(
            @PathVariable("id") int id,
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody TencentServerlessCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "showHiddenInfo", notes = "查看隐藏的密码/密钥信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "凭据 ID（必填）", paramType = "integer", required = true)
    })
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "/{id}/showHiddenInfo", method = RequestMethod.GET)
    public Result showHiddenInfo(
            @PathVariable(value = "projectId") Integer projectId,
            @PathVariable("id") int id
    ) throws CoreException {
        return Result.success(projectCredentialService.showHiddenInfo(id,projectId));
    }
}
