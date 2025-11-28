package com.rayvinchen.async.event.core.properties;

import lombok.Data;

/**
 * 线程池配置
 *
 * @author rayvinchen
 * @since 2025/11/28 19:26
 */
@Data
public class WorkerProperties {
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
     * 心跳更新间隔(秒)
     */
    private int heartbeatIntervalSeconds = 10;

    /**
     * Loader 在内存中允许的最大任务数（综合阈值）
     *
     * 说明：
     * - 当该值 > 0 时，作为绝对上限生效；
     * - 当该值 <= 0 时，采用 3 * worker.queueCapacity 作为动态上限；
     *
     * 目的：避免仅按线程池工作队列容量进行估算导致的内存堆积。
     */
    private int maxInMemoryTasks = 0;

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
