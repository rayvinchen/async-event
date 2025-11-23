package com.rayvinchen.async.event.core.properties;

import lombok.Data;

/**
 * 线程池配置
 *
 * @author rayvinchen
 * @since 2025/11/28 19:26
 */
@Data
public class ThreadPoolProperties {
    /**
     * 核心线程数 (0=自动根据CPU核心数*2)
     */
    private int corePoolSize = 0;

    /**
     * 最大线程数 (0=自动根据CPU核心数*4)
     */
    private int maxPoolSize = 0;

    /**
     * 队列容量
     */
    private int queueCapacity = 2000;

    /**
     * 线程空闲保活时间(秒)
     */
    private int keepAliveSeconds = 60;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "async-event-worker";

    /**
     * 关闭超时时间(秒)
     */
    private int shutdownTimeoutSeconds;

    /**
     * 获取实际的核心线程数
     */
    public int getActualCorePoolSize() {
        return corePoolSize > 0 ? corePoolSize : Runtime.getRuntime().availableProcessors() * 2;
    }

    /**
     * 获取实际的最大线程数
     */
    public int getActualMaxPoolSize() {
        return maxPoolSize > 0 ? maxPoolSize : Runtime.getRuntime().availableProcessors() * 4;
    }
}
