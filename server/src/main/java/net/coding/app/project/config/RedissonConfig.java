package net.coding.app.project.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Value("${spring.redis.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%s", host + "", port + "");
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(redisUrl)
                .setTimeout(3000)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(32);
        if (StringUtils.isNotBlank(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
