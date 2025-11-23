package com.rayvinchen.async.event.boot.starter;

import com.rayvinchen.async.event.core.properties.ThreadPoolProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步事件内存处理配置属性
 *
 * @author rayvinchen
 * @since 2025/11/28
 */
@Data
@ConfigurationProperties(prefix = "async-event")
public class AsyncEventProperties {

    /**
     * 任务加载器配置
     */
    private LoaderProperties loader = new LoaderProperties();

    /**
     * 线程池配置
     */
    private ThreadPoolProperties threadPool = new ThreadPoolProperties();

    /**
     * 队列配置
     */
    private QueueProperties queue = new QueueProperties();

    /**
     * 监控配置
     */
    private MonitorProperties monitor = new MonitorProperties();

    /**
     * 优雅关闭配置
     */
    private ShutdownProperties shutdown = new ShutdownProperties();

    /**
     * 任务加载器配置
     */
    @Data
    public static class LoaderProperties {
        /**
         * 扫描间隔(毫秒)
         */
        private long scanInterval = 10000;

        /**
         * 批量加载大小
         */
        private int batchSize = 1000;

        /**
         * 预加载时间窗口(秒)
         */
        private int lookAheadSeconds = 30;
    }

    /**
     * 队列配置
     */
    @Data
    public static class QueueProperties {
        /**
         * 内存队列容量
         */
        private int capacity = 10000;
    }

    /**
     * 监控配置
     */
    @Data
    public static class MonitorProperties {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 日志打印间隔(毫秒)
         */
        private long logInterval = 60000;
    }

    /**
     * 优雅关闭配置
     */
    @Data
    public static class ShutdownProperties {
        /**
         * 关闭超时时间(秒)
         */
        private int timeoutSeconds = 60;
    }
}
