package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.additional.ProjectAdditionalPredicate;
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
@RestfulApi
public class ProjectAdditionalController {
    private final ProjectAdditionalService projectAdditionalService;

    @GetMapping("info")
    @ApiOperation("获取项目附加信息（功能开关和项目管理员）")
    public Map<Integer, ProjectAdditionalDTO> info(
            @RequestHeader(GatewayHeader.TEAM_ID) Integer teamId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam(required = false) Set<Integer> project,
            @RequestParam(required = false) boolean withFunction,
            @RequestParam(required = false) boolean withMemberCount,
            @RequestParam(required = false) boolean withGroup,
            @RequestParam(required = false) boolean withAdmin
    ) {
        return projectAdditionalService.getWithFunctionAndAdmin(
                teamId,
                userId,
                project,
                new ProjectAdditionalPredicate() {
                    @Override
                    public boolean withFunction() {
                        return withFunction;
                    }

                    @Override
                    public boolean withAdmin() {
                        return withAdmin;
                    }

                    @Override
                    public boolean withMemberCount() {
                        return withMemberCount;
                    }

                    @Override
                    public boolean withGroup() {
                        return withGroup;
                    }
                }
        );
    }
}
