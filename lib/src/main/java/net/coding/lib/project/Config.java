package net.coding.lib.project;

import javax.validation.Validation;
import javax.validation.Validator;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@ComponentScan(basePackageClasses = net.coding.lib.project.Config.class)
@MapperScan(basePackageClasses = Config.class, annotationClass = Mapper.class)
public class Config {

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
