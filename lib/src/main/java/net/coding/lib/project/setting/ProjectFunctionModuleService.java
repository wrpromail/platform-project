package net.coding.lib.project.setting;

import net.coding.lib.project.dto.ProjectFunctionModuleDTO;

import org.springframework.stereotype.Component;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
@Component
@AllArgsConstructor
public class ProjectFunctionModuleService {
    private final ProjectFunctionModuleProperties projectFunctionModuleProperties;

    private final ProjectSettingService projectSettingService;

    public List<ProjectFunctionModuleDTO> getProjectFunctionModules() {
        return StreamEx.of(projectFunctionModuleProperties.getFunctions())
                .filter(function ->
                        StreamEx.of(function.getCodes())
                                .anyMatch(code -> !projectSettingService.isMenuDisabled(code)))
                .map(function -> ProjectFunctionModuleDTO.builder()
                        .code(function.getCode())
                        .name(function.getName())
                        .description(function.getDescription())
                        .codes(function.getCodes())
                        .build()
                )
                .nonNull()
                .toList();
    }
}
