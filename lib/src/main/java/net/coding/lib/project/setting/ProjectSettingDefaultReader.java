package net.coding.lib.project.setting;

import com.google.gson.Gson;

import net.coding.grpc.client.platform.SystemSettingGrpcClient;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import proto.platform.system.setting.SystemSettingProto;

@Slf4j
@Component
@AllArgsConstructor
public class ProjectSettingDefaultReader {
    private final static String KEY = "platform_feature_default_project_setting";

    private final ProjectSettingDefaultProperties projectSettingDefaultProperties;
    private final SystemSettingGrpcClient systemSettingGrpcClient;
    private final Gson gson;

    public Collection<ProjectSettingDefault> read() {
        ProjectSettingDefaultProperties read = Optional
                .ofNullable(getProjectSettingDefaultProperties())
                .orElse(projectSettingDefaultProperties);
        return Optional.ofNullable(read.getDefines())
                .orElse(Collections.emptyList());
    }

    public Optional<ProjectSettingDefault> read(String code) {
        ProjectSettingDefaultProperties read = Optional
                .ofNullable(getProjectSettingDefaultProperties())
                .orElse(projectSettingDefaultProperties);
        return StreamEx.of(read.getDefines())
                .filterBy(ProjectSettingDefault::getCode, code)
                .findFirst();
    }

    private String getValue() {
        try {
            return systemSettingGrpcClient.get(
                    SystemSettingProto.SystemSettingRequest
                            .newBuilder()
                            .setCode(KEY)
                            .build()
            ).getSetting().getValue();
        } catch (Exception e) {
            log.warn("Read system setting {} failure, cause of {}", KEY, e.getMessage());
        }
        return null;
    }

    private ProjectSettingDefaultProperties getProjectSettingDefaultProperties() {
        String value = getValue();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return gson.fromJson(value, ProjectSettingDefaultProperties.class);
        } catch (Exception e) {
            log.warn(
                    "Read system setting {} to {} failure, cause of {}",
                    KEY,
                    ProjectSettingDefaultProperties.class.getName(),
                    e.getMessage()
            );
        }
        return null;
    }
}
