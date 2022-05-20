package net.coding.app.project.config;


import net.coding.app.project.utils.GsonHttpMessageConverter;
import net.coding.common.verification.resolver.VerifyArgumentResolver;
import net.coding.lib.project.common.PagerMethodArgumentResolver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestAttributeMethodArgumentResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
@Configuration
public class MvcConfigurer extends WebMvcConfigurerAdapter {

    private final VerifyArgumentResolver verifyArgumentResolver;

    private final GsonHttpMessageConverter gsonHttpMessageConverter;

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

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseTrailingSlashMatch(false);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(gsonHttpMessageConverter);
        converters.add(stringHttpMessageConverter());
        converters.add(byteArrayHttpMessageConverter());
    }


    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setWriteAcceptCharset(false);
        return converter;
    }


    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.ALL);
        converter.setSupportedMediaTypes(list);
        return converter;
    }

}
