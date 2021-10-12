package net.coding.lib.project.common;


import net.coding.common.verification.resolver.VerifyArgumentResolver;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestAttributeMethodArgumentResolver;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
@Configuration
public class MvcConfigurer extends WebMvcConfigurerAdapter {

    private final VerifyArgumentResolver verifyArgumentResolver;


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(verifyArgumentResolver);
        argumentResolvers.add(new PagerMethodArgumentResolver());
        argumentResolvers.add(new RequestAttributeMethodArgumentResolver());
    }


    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false)
                .mediaType("html", MediaType.TEXT_HTML)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("*", MediaType.ALL);
    }


}
