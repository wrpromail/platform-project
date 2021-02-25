package net.coding.app.project;

import net.coding.common.server.BaseConfig;
import net.coding.common.server.BaseServer;
import net.coding.common.verification.VerificationAutoConfiguration;

import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

/**
 * created by wang007 on 2020/7/26
 */
@ComponentScan(basePackageClasses = {
        Application.class,
})
@Import({
        BaseConfig.class,
        net.coding.common.rpc.server.Config.class,
        net.coding.lib.project.Config.class,
        net.coding.common.eventbus.Config.class,
        net.coding.common.rabbitmq.Config.class,
        net.coding.grpc.client.activity.Config.class,
        net.coding.grpc.client.template.Config.class,
        net.coding.grpc.client.pinyin.Config.class,
        net.coding.common.i18n.Config.class,
        net.coding.grpc.client.depot.Config.class,
        net.coding.common.util.Config.class,
        net.coding.common.storage.Config.class,
        net.coding.common.config.Config.class,
        net.coding.common.redis.Config.class,
        net.coding.common.cache.evict.Config.class,
        net.coding.grpc.client.platform.Config.class,
        net.coding.grpc.client.permission.Config.class

})
@ImportAutoConfiguration({GRpcAutoConfiguration.class, VerificationAutoConfiguration.class})
@EnableScheduling
@EnableSwagger2
public class Application {
    @Value("${production:false}")
    private boolean production;

    public static void main(String[] args) {
        BaseServer.run(Application.class, args);
    }

    @Bean
    public Docket docket() {
        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2)
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
