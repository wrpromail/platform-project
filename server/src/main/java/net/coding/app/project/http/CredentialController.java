package net.coding.app.project.http;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.Result;
import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.credential.enums.CredentialType;
import net.coding.lib.project.credential.service.ProjectCredentialService;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.credential.AndroidCredentialForm;
import net.coding.lib.project.form.credential.CredentialForm;
import net.coding.lib.project.form.credential.TencentServerlessCredentialForm;
import net.coding.lib.project.pager.PagerResolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @GetMapping
    public Result queryList(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @RequestParam(required = false) String type,
            @PagerResolve PageRowBounds pager
    ) throws CoreException {
        CredentialType credentialType = null;
        if (StringUtils.isNotBlank(type)) {
            credentialType = CredentialType.valueOf(type);
        }
        return Result.success(projectCredentialService.list(projectId, credentialType, pager));
    }

    @ApiOperation(value = "delete", notes = "删除项目凭据")
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @DeleteMapping("/{id}")
    public Result delete(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id
    ) throws CoreException {
        if (SystemContextHolder.get() == null) {
            throw CoreException.of(CoreException.ExceptionType.USER_NOT_LOGIN);
        }
        projectCredentialService.delete(id, projectId);
        return Result.success();
    }

    @ApiOperation(value = "get", notes = "查询项目凭据详情")
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @GetMapping("/{id}")
    public Result get(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id
    ) throws CoreException {
        return Result.success(projectCredentialService.getCredential(projectId, id));
    }

    @ApiOperation(value = "createCredential", notes = "创建项目凭据")
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PostMapping
    public Result createCredential(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody CredentialForm form
    ) throws CoreException {
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "createAndroidCert", notes = "创建项目 android 凭据")
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PostMapping("/androidCredential")
    public Result createAndroidCert(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody AndroidCredentialForm form
    ) throws Exception {
        if (form.getFilePassword().getBytes().length > 80) {
            throw CoreException.of(CoreException.ExceptionType.CREDENTIAL_PASSWORD_BYTES_TOO_LONG);
        }
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "createServerlessCert", notes = "创建项目腾讯云凭据")
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PostMapping("/tencentServerlessCredential")
    public Result createServerlessCert(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @Valid @RequestBody TencentServerlessCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.addCredential(projectId, form));
    }

    @ApiOperation(value = "updateCredential", notes = "更新项目凭据")
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PutMapping("/{id}")
    public Result updateWithTasks(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id,
            @Valid @RequestBody CredentialForm form
    ) throws CoreException {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "updateAndroidCert", notes = "更新项目 android 凭据")
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PutMapping("/{id}/androidCredential")
    public Result updateAndroidCert(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id,
            @Valid @RequestBody AndroidCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "updateTencentServerlessCredential", notes = "更新项目 android 凭据")
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @PutMapping("/{id}/serverlessCredential")
    public Result updateTencentServerlessCredential(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id,
            @Valid @RequestBody TencentServerlessCredentialForm form
    ) throws Exception {
        return Result.success(projectCredentialService.updateCredential(projectId, id, form));
    }

    @ApiOperation(value = "showHiddenInfo", notes = "查看隐藏的密码/密钥信息")
    @ProtectedAPI(authMethod = AUTH_TYPE_DEFAULT)
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @GetMapping("/{id}/showHiddenInfo")
    public Result showHiddenInfo(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable(value = "projectId") Integer projectId,
            @ApiParam(value = "凭据 ID（必填）", required = true)
            @PathVariable("id") int id
    ) throws CoreException {
        return Result.success(projectCredentialService.showHiddenInfo(id, projectId));
    }
}
