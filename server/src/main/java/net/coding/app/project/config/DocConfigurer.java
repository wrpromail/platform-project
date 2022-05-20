package net.coding.app.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;

import static springfox.documentation.builders.PathSelectors.regex;


@Configuration
@Slf4j
public class DocConfigurer {

    @Value("${production:false}")
    private boolean production;

    @Bean
    public Docket docket() {
        ApiSelectorBuilder builder = new Docket(DocumentationType.OAS_30)
                .forCodeGeneration(true)
                .pathMapping("/")
                .select();

        if (!production) {
            builder.apis(RequestHandlerSelectors.withClassAnnotation(RestController.class));
        } else {
            builder.apis(RequestHandlerSelectors.none());
        }

        builder.paths(regex("/api.*"));

        return builder.build().ignoredParameterTypes(RequestAttribute.class);
    }
}

