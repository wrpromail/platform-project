package net.coding.app.project.http;

import net.coding.app.project.constant.GatewayHeader;
import net.coding.common.util.Result;
import net.coding.common.util.StringUtils;
import net.coding.lib.project.entity.ProjectPersonalPreference;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.service.ProjectPersonalPreferenceService;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping(value = "/api/platform/project/{projectId}/personal-preference")
@AllArgsConstructor
@Api(value = "项目个人偏好", tags = "项目个人偏好")
public class ProjectPersonalPreferenceController {
    private final String NAME = "stickMenu";
    private final ProjectPersonalPreferenceService projectPersonalPreferenceService;

    @GetMapping(value = "")
    public Result getByKey(
            @PathVariable("projectId") Integer projectId,
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            @RequestParam String key // 目前仅支持单个 key=xxxx 的请求方式
    ) throws CoreException {
        if (StringUtils.isBlank(key) || !NAME.equals(key)) {
            throw CoreException.of(CoreException.ExceptionType.PARAMETER_INVALID);
        }
        ProjectPersonalPreference preference = projectPersonalPreferenceService
                .findBy(projectId, userId, key);
        return Result.success(toMap(preference));
    }

    private Map<String, String> toMap(ProjectPersonalPreference preference) {
        if (preference == null) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(preference.getKey(), preference.getValue());
    }

    @PutMapping(value = "")
    public Result setByMap(
            @PathVariable("projectId") Integer projectId,
            @RequestHeader(name = GatewayHeader.USER_ID) Integer userId,
            HttpServletRequest request
    ) throws CoreException {
        Map<String, String> puttingPreferences = parseKeys(request);
        if (puttingPreferences.isEmpty()) {
            throw CoreException.of(CoreException.ExceptionType.PROJECT_PERSONAL_PREFERENCE_KEYS_NOT_EXIST);
        }
        List<ProjectPersonalPreference> preferences = projectPersonalPreferenceService
                .put(projectId, userId, puttingPreferences);
        return Result.success(toMap(preferences));
    }

    @NotNull
    private Map<String, String> parseKeys(HttpServletRequest request) {
        Map<String, String> puttingPreferences = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (StringUtils.isNotBlank(name) && NAME.equals(name)) {
                String parameter = request.getParameter(name);
                puttingPreferences.put(name, parameter);
            }
        }
        return puttingPreferences;
    }

    private Map<String, String> toMap(List<ProjectPersonalPreference> preferences) {
        Map<String, String> result = new HashMap<>(preferences.size());
        preferences.forEach(projectPersonalPreference ->
                result.put(projectPersonalPreference.getKey(), projectPersonalPreference.getValue()));
        return result;
    }

}
