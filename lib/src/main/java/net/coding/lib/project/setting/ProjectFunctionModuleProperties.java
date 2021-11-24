package net.coding.lib.project.setting;

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
import java.util.Set;

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
        value = "classpath:project-function-module.json",
        factory = ProjectFunctionModuleProperties.JsonPropertySourceFactory.class
)
@ConfigurationProperties
public class ProjectFunctionModuleProperties {
    private List<ProjectFunctionModuleDefault> functions;

    static class JsonPropertySourceFactory implements PropertySourceFactory {
        private final static TypeReference<Map<String, List<ProjectFunctionModuleDefault>>> TYPE = new TypeReference<Map<String, List<ProjectFunctionModuleDefault>>>() {
        };

        @Override
        public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
            Map value = new ObjectMapper().readValue(resource.getInputStream(), TYPE);
            log.info("Loaded project function module config value {} ", value);
            return new MapPropertySource("project-function-module", value);
        }
    }
}
