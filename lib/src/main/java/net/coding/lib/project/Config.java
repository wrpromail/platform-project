package net.coding.lib.project;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import javax.validation.Validation;
import javax.validation.Validator;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import tk.mybatis.spring.annotation.MapperScan;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;


@ComponentScan(basePackageClasses = net.coding.lib.project.Config.class)
@MapperScan(basePackageClasses = Config.class, annotationClass = Mapper.class)
public class Config {

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BODY);
        return new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory());
        return restTemplate;
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }
}
