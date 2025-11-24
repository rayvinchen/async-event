package com.rayvinchen.async.event.boot.starter;


import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.rayvinchen.async.event.boot.starter.config.MybatisConfig;
import com.rayvinchen.async.event.boot.starter.config.RepositoryConfig;
import com.rayvinchen.async.event.boot.starter.loader.AsyncEventLoader;
import com.rayvinchen.async.event.boot.starter.loader.DefaultAsyncEventLoader;
import com.rayvinchen.async.event.core.AsyncEventTemplate;
import com.rayvinchen.async.event.core.AsyncEventWorker;
import com.rayvinchen.async.event.core.executor.AsyncEventExecutor;
import com.rayvinchen.async.event.core.executor.DefaultAsyncEventExecutor;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandler;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandlerDelegate;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * AsyncEventAutoConfiguration
 *
 * @author rayvinchen
 * @since 2025/11/9 11:41
 */
@Import({MybatisConfig.class, RepositoryConfig.class})
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class})
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
     * @param asyncEventHandlerDelegate 异步事件处理器代理
     * @return 异步事件执行器
     */
    @Bean
    @ConditionalOnMissingBean(AsyncEventExecutor.class)
    public AsyncEventExecutor asyncEventExecutor(AsyncEventRepository asyncEventRepository,
                                                 AsyncEventRecordRepository asyncEventRecordRepository,
                                                 AsyncEventHandlerDelegate asyncEventHandlerDelegate,
                                                 AsyncEventProperties properties,
                                                 PlatformTransactionManager txManager) {
        return new DefaultAsyncEventExecutor(
                asyncEventRepository,
                asyncEventRecordRepository,
                asyncEventHandlerDelegate,
                properties.getWorker().getHeartbeatIntervalSeconds(),
                new TransactionTemplate(txManager)
        );
    }

    /**
     * 异步事件分发器
     *
     * @param properties 异步事件参数
     * @param executor 异步事件执行器
     * @return 异步事件分发器
     */
    @Bean
    public AsyncEventWorker asyncEventDispatcher(AsyncEventProperties properties,
                                                 AsyncEventExecutor executor) {
        return new AsyncEventWorker(properties.getWorker(), executor);
    }

    /**
     * 异步事件加载器
     *
     * @param properties properties
     * @param worker 异步事件分发器
     * @param asyncEventRepository 异步事件仓库
     * @return 异步事件加载器
     */
    @Bean
    @ConditionalOnMissingBean(AsyncEventLoader.class)
    public AsyncEventLoader asyncEventLoader(AsyncEventProperties properties,
                                             AsyncEventWorker worker,
                                             AsyncEventRepository asyncEventRepository) {
        return new DefaultAsyncEventLoader(properties, worker, asyncEventRepository);
    }

    /**
     * 异步事件Template
     *
     * @param asyncEventRepository 异步事件仓库
     * @param asyncEventRecordRepository 异步事件记录仓库
     * @param asyncEventWorker 异步事件分发器
     * @return 异步事件Template
     */
    @Bean
    public AsyncEventTemplate asyncEventTemplate(AsyncEventRepository asyncEventRepository,
                                                 AsyncEventRecordRepository asyncEventRecordRepository,
                                                 AsyncEventWorker asyncEventWorker) {
        return new AsyncEventTemplate(asyncEventRepository, asyncEventRecordRepository, asyncEventWorker);
    }

}
