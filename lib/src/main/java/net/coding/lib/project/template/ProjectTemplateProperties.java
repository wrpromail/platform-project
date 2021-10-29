package net.coding.lib.project.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Component
@PropertySource(
        value = "classpath:project-template.json",
        factory = ProjectTemplateProperties.JsonPropertySourceFactory.class
)
@ConfigurationProperties
public class ProjectTemplateProperties {
    private Set<String> devOps;
    private Set<String> projectManage;
    private Set<String> testingManage;
    private Set<String> codeManage;

    static class JsonPropertySourceFactory implements PropertySourceFactory {
        private final static TypeReference<Map<String, Set<String>>> TYPE = new TypeReference<Map<String, Set<String>>>() {
        };

        @Override
        public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
            Map value = new ObjectMapper().readValue(resource.getInputStream(), TYPE);
            return new MapPropertySource("project-template", value);
        }
    }
}
