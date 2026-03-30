package com.han.back.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.embedded.RedisServer;

import java.io.IOException;

@Profile("test")
@Configuration
public class TestRedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(port);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @Bean
    public RedisConnectionFactory testRedisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    // Key-Value 구조 문자열 데이터를 캐싱/저장
    @Bean(name = "customStringRedisTemplate")
    public RedisTemplate<String, String> customStringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(testRedisConnectionFactory());

        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());

        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());

        return template;
    }

    // DTO, Entity 등 자바 객체를 JSON 형태로 캐싱/저장
    @Bean(name = "customJsonRedisTemplate")
    public RedisTemplate<String, Object> customJsonRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(testRedisConnectionFactory());

        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());

        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());

        return template;
    }

}