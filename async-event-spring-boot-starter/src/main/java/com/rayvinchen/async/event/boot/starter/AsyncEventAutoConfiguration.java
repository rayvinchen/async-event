package com.rayvinchen.async.event.boot.starter;


import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.rayvinchen.async.event.boot.starter.config.LockTemplateConfig;
import com.rayvinchen.async.event.boot.starter.config.MybatisConfig;
import com.rayvinchen.async.event.boot.starter.config.RepositoryConfig;
import com.rayvinchen.async.event.boot.starter.loader.AsyncEventLoader;
import com.rayvinchen.async.event.boot.starter.loader.DefaultAsyncEventLoader;
import com.rayvinchen.async.event.core.executor.AsyncEventDispatcher;
import com.rayvinchen.async.event.core.executor.AsyncEventExecutor;
import com.rayvinchen.async.event.core.executor.DefaultAsyncEventExecutor;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandler;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandlerDelegate;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import com.rayvinchen.async.event.core.template.AsyncEventTemplate;
import com.rayvinchen.async.event.core.template.LockTemplate;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * AsyncEventAutoConfiguration
 *
 * @author rayvinchen
 * @since 2025/11/9 11:41
 */
@Import({LockTemplateConfig.class, MybatisConfig.class, RepositoryConfig.class})
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class, RedissonAutoConfiguration.class})
@EnableConfigurationProperties(AsyncEventProperties.class)
public class AsyncEventAutoConfiguration {

    /**
     * 异步事件处理器代理
     *
     * @param handlers 异步事件处理器列表
     * @return 异步事件处理器代理
     */
    @Bean
    public AsyncEventHandlerDelegate asyncEventHandlerDelegate(List<AsyncEventHandler> handlers) {
        return new AsyncEventHandlerDelegate(handlers);
    }

    /**
     * 异步事件执行器
     *
     * @param asyncEventRepository 异步事件仓库
     * @param asyncEventRecordRepository 异步事件记录仓库
     * @param lockTemplate 分布式锁Template
     * @param asyncEventHandlerDelegate 异步事件处理器代理
     * @return 异步事件执行器
     */
    @Bean
    @ConditionalOnMissingBean(AsyncEventExecutor.class)
    public AsyncEventExecutor asyncEventExecutor(AsyncEventRepository asyncEventRepository,
                                                 AsyncEventRecordRepository asyncEventRecordRepository,
                                                 LockTemplate lockTemplate,
                                                 AsyncEventHandlerDelegate asyncEventHandlerDelegate) {
        return new DefaultAsyncEventExecutor(asyncEventRepository, asyncEventRecordRepository,
                lockTemplate, asyncEventHandlerDelegate);
    }

    /**
     * 异步事件分发器
     *
     * @param properties 异步事件参数
     * @param executor 异步事件执行器
     * @return 异步事件分发器
     */
    @Bean
    public AsyncEventDispatcher asyncEventDispatcher(AsyncEventProperties properties,
                                                     AsyncEventExecutor executor) {
        return new AsyncEventDispatcher(properties.getThreadPool(), executor);
    }

    /**
     * 异步事件加载器
     *
     * @param properties properties
     * @param dispatcher 异步事件分发器
     * @param asyncEventRepository 异步事件仓库
     * @return 异步事件加载器
     */
    @Bean
    @ConditionalOnMissingBean(AsyncEventLoader.class)
    public AsyncEventLoader asyncEventLoader(AsyncEventProperties properties,
                                             AsyncEventDispatcher dispatcher,
                                             AsyncEventRepository asyncEventRepository) {
        return new DefaultAsyncEventLoader(properties, dispatcher, asyncEventRepository);
    }

    /**
     * 异步事件Template
     *
     * @param asyncEventRepository 异步事件仓库
     * @param asyncEventRecordRepository 异步事件记录仓库
     * @param asyncEventDispatcher 异步事件分发器
     * @return 异步事件Template
     */
    @Bean
    public AsyncEventTemplate asyncEventTemplate(AsyncEventRepository asyncEventRepository,
                                                 AsyncEventRecordRepository asyncEventRecordRepository,
                                                 AsyncEventDispatcher asyncEventDispatcher) {
        return new AsyncEventTemplate(asyncEventRepository, asyncEventRecordRepository, asyncEventDispatcher);
    }

}
