package net.coding.app.project.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%s", host+"", port+"");
        config.useSingleServer().setAddress(redisUrl).setTimeout(3000).setConnectionMinimumIdleSize(2).setConnectionPoolSize(32);
        return Redisson.create(config);
    }
}
