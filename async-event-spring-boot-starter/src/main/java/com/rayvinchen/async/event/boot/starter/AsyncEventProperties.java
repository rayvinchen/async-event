package com.rayvinchen.async.event.boot.starter;

import com.rayvinchen.async.event.core.properties.WorkerProperties;
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
    private WorkerProperties worker = new WorkerProperties();

    /**
     * 任务加载器配置
     */
    @Data
    public static class LoaderProperties {
        /**
         * 扫描间隔(秒)
         */
        private long scanIntervalSeconds = 10;

        /**
         * 批量加载大小
         */
        private int batchSize = 200;

        /**
         * 预加载时间窗口(秒)
         */
        private int lookAheadSeconds = 30;

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

}
