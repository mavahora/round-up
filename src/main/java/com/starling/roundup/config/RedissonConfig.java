package com.starling.roundup.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            //logger.info("Starting embedded Redis server on port {}", redisPort);
            redisServer = new RedisServer(redisPort);
            redisServer.start();
        } catch (Exception e) {
            // .error("Failed to start embedded Redis server", e);
            if (e.getMessage().contains("Address already in use")) {
//                logger.info("Redis server is already running on port {}", redisPort);
            }
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
//            logger.info("Stopping embedded Redis server");
            redisServer.stop();
        }
    }
    
    @Bean
    public RedissonClient redissonClient(@Value("${spring.redis.host}") String host,
                                         @Value("${spring.redis.port}") int port) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectTimeout(5000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        
        return Redisson.create(config);
    }
}