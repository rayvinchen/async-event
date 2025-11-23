package com.rayvinchen.async.event.boot.starter.loader;

import com.rayvinchen.async.event.boot.starter.AsyncEventProperties;
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.AsyncEventWorker;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import com.rayvinchen.async.event.core.valobj.ListAsyncEventQuery;
import com.rayvinchen.async.event.core.valobj.Range;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步事件任务加载器
 * 职责:
 * 1. 定期从数据库批量加载待执行的异步事件到内存
 * 2. 避免重复加载已在内存中的任务
 * 3. 支持启动和停止控制
 *
 * @author rayvinchen
 * @since 2025/11/28
 */
@Slf4j
public class DefaultAsyncEventLoader implements AsyncEventLoader, InitializingBean, DisposableBean {

    private final AsyncEventRepository asyncEventRepository;
    private final AsyncEventProperties properties;

    private final AsyncEventWorker worker;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread loaderThread;

    public DefaultAsyncEventLoader(AsyncEventProperties properties,
                                   AsyncEventWorker worker,
                                   AsyncEventRepository asyncEventRepository) {
        this.properties = properties;
        this.worker = worker;
        this.asyncEventRepository = asyncEventRepository;
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    /**
     * 启动加载器
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            loaderThread = new Thread(this::loadLoop, "async-event-loader");
            loaderThread.setDaemon(false);
            loaderThread.start();
            log.info("AsyncEventLoader started. scanInterval={}ms, batchSize={}, lookAheadSeconds={}s",
                    properties.getLoader().getScanInterval(),
                    properties.getLoader().getBatchSize(),
                    properties.getLoader().getLookAheadSeconds());
        }
    }

    /**
     * 停止加载器
     */
    public void close() {
        if (running.compareAndSet(true, false)) {
            if (loaderThread != null) {
                loaderThread.interrupt();
                try {
                    loaderThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("AsyncEventLoader stopped.");
        }
    }

    /**
     * 加载循环
     */
    private void loadLoop() {
        while (isRunning()) {
            try {
                loadTasks();
                Thread.sleep(properties.getLoader().getScanInterval());
            } catch (InterruptedException e) {
                log.info("AsyncEventLoader interrupted.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error occurred while loading tasks", e);
            }
        }
    }

    /**
     * 加载任务
     */
    private void loadTasks() {
        try {
            // 计算加载时间范围: now ~ now + lookAheadSeconds
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(properties.getLoader().getLookAheadSeconds());

            // 构建查询条件
            ListAsyncEventQuery query = buildQuery(now, endTime);

            // 批量查询数据库
            List<AsyncEvent> events = asyncEventRepository.listAsyncEvents(query);

            if (events.isEmpty()) {
                log.debug("No async event to load at this time.");
                return;
            }

            // 加入优先级队列和索引
            int loadedCount = 0;
            for (AsyncEvent event : events) {
                // 检查队列容量
                int queueCapacity = properties.getWorker().getQueueCapacity();
                if (worker.getLoadedTaskCount() >= 2 * queueCapacity) {
                    log.warn("Task queue capacity reached limit: {}", 2 * queueCapacity);
                    break;
                }

                // 加入队列
                worker.offer(event);
                loadedCount++;
                log.debug("Task loaded: {}", event);
            }

            log.info("Loaded {} tasks to memory queue. Total loaded tasks: {}, Queue size: {}",
                    loadedCount, worker.getLoadedTaskCount(), worker.getTaskQueueSize());

        } catch (Exception e) {
            log.error("Error occurred while loading tasks", e);
        }
    }

    /**
     * 构建查询条件
     */
    private ListAsyncEventQuery buildQuery(LocalDateTime startTime, LocalDateTime endTime) {
        // 查询待执行和待重试的任务
        Set<Byte> statusSet = Set.of(
                AsyncEventStatusEnum.WAIT_EXEC.getCode(),
                AsyncEventStatusEnum.WAIT_RETRY.getCode()
        );

        // 转换为Date类型
        Date startDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant());

        Range<Date> timeRange = Range.<Date>builder()
                .start(startDate)
                .end(endDate)
                .excludeStart(false)
                .excludeEnd(false)
                .build();

        return ListAsyncEventQuery.builder()
                .statusSet(statusSet)
                .expectTimeRange(timeRange)
                .build();
    }

    /**
     * 判断是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

}
