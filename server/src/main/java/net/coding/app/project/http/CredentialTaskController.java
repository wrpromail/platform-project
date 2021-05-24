package net.coding.app.project.http;


import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.Result;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.service.credential.ProjectCredentialService;
import net.coding.lib.project.service.credential.ProjectCredentialTaskService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@RestController
@Api(value = "项目凭据与task关系", tags = "项目凭据")
@RequestMapping("/api/platform/project/{projectId}/credentials/{id}/jobTask")
public class CredentialTaskController {

    private final ProjectCredentialTaskService projectCredentialTaskService;
    private final ProjectCredentialService projectCredentialService;

    @ApiOperation(value = "connectionJobTask", notes = "项目凭据与task关系列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "id", value = "项目凭据 ID（必填）", paramType = "integer", required = true),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "string")
    })
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Result connectionJobTask(
            @PathVariable("projectId") int projectId,
            @PathVariable("id") int id,
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        Credential credential = projectCredentialService.get(id, projectId);
        return Result.success(projectCredentialTaskService.taskList(projectId, credential, type));
    }
}