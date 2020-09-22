package net.coding.app.project;

import net.coding.common.server.BaseConfig;
import net.coding.common.server.BaseServer;

import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * created by wang007 on 2020/7/26
 */
@ComponentScan(basePackageClasses = {
        Application.class,
})
@Import({
        BaseConfig.class,
        net.coding.common.rpc.Config.class,
        net.coding.lib.project.Config.class
})
@ImportAutoConfiguration({GRpcAutoConfiguration.class})
@MapperScan("net.coding.lib.project")
public class Application {

    public static void main(String[] args) {
        BaseServer.run(Application.class, args);
    }

}
