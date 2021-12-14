package net.coding.lib.project.setting.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Component
@PropertySource(
        value = "classpath:user-project-setting-default.json",
        factory = UserProjectSettingDefaultProperties.JsonPropertySourceFactory.class
)
@ConfigurationProperties
public class UserProjectSettingDefaultProperties {
    private List<UserProjectSettingDefault> mails;

    static class JsonPropertySourceFactory implements PropertySourceFactory {
        private final static TypeReference<Map<String, List<UserProjectSettingDefault>>> TYPE = new TypeReference<Map<String, List<UserProjectSettingDefault>>>() {
        };

        @Override
        public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
            Map value = new ObjectMapper().readValue(resource.getInputStream(), TYPE);
            log.info("Loaded user project setting default config value {} ", value);
            return new MapPropertySource("user-project-setting-default", value);
        }
    }
}
