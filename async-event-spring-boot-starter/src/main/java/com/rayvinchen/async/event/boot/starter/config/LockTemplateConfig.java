package com.rayvinchen.async.event.boot.starter.config;

import com.rayvinchen.async.event.core.template.LockTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * LockTemplate配置类
 *
 * @author rayvinchen
 * @since 2025/11/8 18:00
 */
public class LockTemplateConfig {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate lockTemplate(RedissonClient redissonClient) {
        return new LockTemplate(redissonClient);
    }

}