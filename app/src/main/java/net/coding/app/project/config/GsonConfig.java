package net.coding.app.project.config;

import net.coding.app.project.utils.GsonHttpMessageConverter;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class GsonConfig extends WebMvcConfigurerAdapter {

    private final GsonHttpMessageConverter gsonHttpMessageConverter;

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        converters.add(gsonHttpMessageConverter);
    }
}
