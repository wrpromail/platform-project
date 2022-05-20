package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.framework.webapp.response.annotation.RestfulApi;
import net.coding.lib.project.dto.UserProjectSettingValueDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.UserProjectSettingService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;

@ResponseBody
@RestController
@Api(value = "用户项目设置", tags = "用户项目设置")
@AllArgsConstructor
@RequestMapping(value = "/api/platform/project/{projectId}/user-settings")
@RestfulApi
public class UserProjectSettingController {
    private final UserProjectSettingService userProjectSettingService;

    @ApiOperation(value = "查询用户项目设置信息", notes = "原api:/api/project/*/user-settings get")
    @ProtectedAPI
    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<UserProjectSettingValueDTO> search(
            @RequestHeader(GatewayHeader.PROJECT_ID) Integer projectId,
            @RequestHeader(GatewayHeader.USER_ID) Integer userId,
            @RequestParam List<String> codes) throws CoreException {
        // 根据 code 列表获取对应配置
        return userProjectSettingService.getValuesByCodes(codes, projectId, userId);
    }

    @ApiOperation(value = "修改用户项目设置信息", notes = "原api:/api/project/*/user-settings/{code} post")
    @ProtectedAPI
    @RequestMapping(value = "{code}", method = RequestMethod.POST)
    public UserProjectSettingValueDTO update(@RequestHeader(GatewayHeader.PROJECT_ID) Integer projectId,
                                             @RequestHeader(GatewayHeader.USER_ID) Integer userId,
                                             @PathVariable("code") String code,
                                             @RequestParam(value = "value") String value)
            throws CoreException {
        // 更新用户配置
        return userProjectSettingService.updateUserProjectSettingValue(code, value, projectId, userId);
    }
}
