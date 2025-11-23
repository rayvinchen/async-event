package com.rayvinchen.async.event.boot.starter.config;


import com.rayvinchen.async.event.boot.starter.mapper.AsyncEventMapper;
import com.rayvinchen.async.event.boot.starter.mapper.AsyncEventRecordMapper;
import com.rayvinchen.async.event.boot.starter.repository.DefaultAsyncEventRecordRepository;
import com.rayvinchen.async.event.boot.starter.repository.DefaultAsyncEventRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * RepositoryConfig
 *
 * @author rayvinchen
 * @since 2025/11/9 11:46
 */
public class RepositoryConfig {

    @Bean
    @ConditionalOnMissingBean(AsyncEventRepository.class)
    public AsyncEventRepository asyncEventRepository(AsyncEventMapper asyncEventMapper) {
        return new DefaultAsyncEventRepository(asyncEventMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AsyncEventRecordRepository.class)
    public AsyncEventRecordRepository asyncEventRecordRepository(AsyncEventRecordMapper asyncEventRecordMapper) {
        return new DefaultAsyncEventRecordRepository(asyncEventRecordMapper);
    }

}
