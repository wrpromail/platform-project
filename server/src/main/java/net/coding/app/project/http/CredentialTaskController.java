package net.coding.app.project.http;


import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.util.Result;
import net.coding.lib.project.credential.entity.Credential;
import net.coding.lib.project.credential.service.ProjectCredentialService;
import net.coding.lib.project.credential.service.ProjectCredentialTaskService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    @ProjectApiProtector(function = Function.ProjectServiceConn, action = Action.View)
    @GetMapping()
    public Result connectionJobTask(
            @ApiParam(value = "项目 ID（必填）", required = true)
            @PathVariable("projectId") int projectId,
            @ApiParam(value = "项目凭据 ID（必填）", required = true)
            @PathVariable("id") int id,
            @ApiParam(value = "类型")
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        Credential credential = projectCredentialService.get(id, projectId);
        return Result.success(projectCredentialTaskService.taskList(projectId, credential, type));
    }
}