package net.coding.app.project;

import net.coding.common.verification.VerificationAutoConfiguration;
import net.coding.framework.autoconfigure.webapp.RestfulApiResponseSupportConfig;
import net.coding.platform.degradation.ServiceDegradationAutoConfiguration;
import net.coding.platform.redis.RedisLettuceAutoConfiguration;

import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import springfox.documentation.oas.annotations.EnableOpenApi;

@Import({
        net.coding.common.rpc.server.Config.class,
        net.coding.lib.project.Config.class,
        net.coding.common.eventbus.Config.class,
        net.coding.common.rabbitmq.Config.class,
        net.coding.grpc.client.activity.Config.class,
        net.coding.grpc.client.template.Config.class,
        net.coding.common.i18n.Config.class,
        net.coding.grpc.client.depot.Config.class,
        net.coding.common.util.Config.class,
        net.coding.common.redis.Config.class,
        net.coding.common.cache.evict.Config.class,
        net.coding.grpc.client.platform.Config.class,
        net.coding.grpc.client.permission.Config.class,
        net.coding.e.grpcClient.collaboration.Config.class,
        net.coding.service.hook.definition.ServiceHookConfigurer.class,
        net.coding.grpc.client.platform.infra.text.pinyin.Config.class,
        net.coding.grpc.client.platform.infra.text.moderation.Config.class,
        net.coding.platform.charge.client.grpc.EnterpriseGrpcClient.class,
        proto.git.Config.class,
        net.coding.platform.ram.Config.class

})
@ImportAutoConfiguration(
        value = {
                GRpcAutoConfiguration.class,
                VerificationAutoConfiguration.class,
                GsonAutoConfiguration.class,
                ServiceDegradationAutoConfiguration.class,
                RestfulApiResponseSupportConfig.class,
                RedisLettuceAutoConfiguration.class
        },
        exclude = {
                HttpMessageConvertersAutoConfiguration.class
        }
)
@EnableAsync
@EnableScheduling
@EnableOpenApi
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
