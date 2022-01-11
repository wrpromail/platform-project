package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.lib.project.additional.ProjectAdditionalService;
import net.coding.lib.project.additional.dto.ProjectAdditionalDTO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;

@Api(tags = "项目附加信息")
@RestController
@RequestMapping("/api/platform/project/additional")
@AllArgsConstructor
public class ProjectAdditionalController {
    private final ProjectAdditionalService projectAdditionalService;

    @GetMapping("info")
    @ApiOperation("获取项目附加信息（功能开关和项目管理员）")
    public Map<Integer, ProjectAdditionalDTO> info(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestParam Set<Integer> project
    ) {
        return projectAdditionalService.getWithFunctionAndAdmin(teamId, project);
    }
}
