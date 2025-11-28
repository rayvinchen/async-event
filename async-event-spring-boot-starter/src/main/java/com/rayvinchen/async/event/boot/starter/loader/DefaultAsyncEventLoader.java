package com.rayvinchen.async.event.boot.starter.loader;

import com.rayvinchen.async.event.boot.starter.AsyncEventProperties;
import com.rayvinchen.async.event.core.AsyncEventWorker;
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import com.rayvinchen.async.event.core.valobj.ListAsyncEventQuery;
import com.rayvinchen.async.event.core.valobj.Range;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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
    
    private final AtomicInteger loadTimes = new AtomicInteger(0);

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
            log.info("AsyncEventLoader started. scanInterval={}s, batchSize={}, lookAheadSeconds={}s",
                    properties.getLoader().getScanIntervalSeconds(),
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
                int times = loadTimes.incrementAndGet();
                loadUpcomingTasks();
                
                if (times % 10 == 0) {
                    loadRecoverTasks();
                }

                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(properties.getLoader().getScanIntervalSeconds()));
            } catch (Exception e) {
                log.error("Error occurred while loading tasks", e);
            }
        }
    }

    /**
     * 加载任务
     */
    private void loadUpcomingTasks() {
        try {
            // 计算加载时间范围: now ~ now + lookAheadSeconds
            int lookAheadSeconds = properties.getLoader().getLookAheadSeconds();
            int batchSize = properties.getLoader().getBatchSize();
            List<AsyncEvent> events = loadUpcomingEvents(lookAheadSeconds, batchSize);

            offerEvents("Upcoming", events);
        } catch (Exception e) {
            log.error("Error occurred while loading upcoming tasks", e);
        }
    }

    private void loadRecoverTasks() {
        try {
            // 计算加载时间范围: now ~ now + lookAheadSeconds
            int heartbeatIntervalSeconds = properties.getWorker().getHeartbeatIntervalSeconds();
            int batchSize = properties.getLoader().getBatchSize();
            List<AsyncEvent> events = loadRecoverEvents(3 * heartbeatIntervalSeconds, batchSize);

            offerEvents("Recover", events);
        } catch (Exception e) {
            log.error("Error occurred while loading recover tasks", e);
        }
    }

    private void offerEvents(String title, List<AsyncEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            log.debug("No async event to load at this time.");
            return;
        }

        // 加入优先级队列和索引
        int loadedCount = 0;
        for (AsyncEvent event : events) {
            // 加入队列
            if (!worker.offer(event)) {
                break;
            }
            loadedCount++;
            log.debug("[{}] Task loaded: {}", title, event);
        }

        log.info("[{}] Loaded {} tasks to memory queue. Total loaded tasks: {}, Queue size: {}",
                title, loadedCount, worker.getLoadedTaskCount(), worker.getTaskQueueSize());
    }

    @Override
    public List<AsyncEvent> loadUpcomingEvents(int lookAheadSeconds, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime = now.minusHours(3);
        LocalDateTime endTime = now.plusSeconds(Math.max(0, lookAheadSeconds));

        ListAsyncEventQuery query = buildQuery(beginTime, endTime);
        List<AsyncEvent> events = asyncEventRepository.listAsyncEvents(query, limit);
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events;
    }

    @Override
    public List<AsyncEvent> loadRecoverEvents(int staleSeconds, int limit) {
        // 计算“失联”阈值时间点：当前时间 - staleSeconds
        LocalDateTime heartbeatBefore = LocalDateTime.now().minusSeconds(Math.max(1, staleSeconds));
        Range<LocalDateTime> hearbeatAtRange = Range.<LocalDateTime>builder()
                .end(heartbeatBefore)
                .build();

        ListAsyncEventQuery recoverEventQuery = ListAsyncEventQuery.builder()
                .heartbeatAtRange(hearbeatAtRange)
                .statusSet(Set.of(AsyncEventStatusEnum.EXECUTING.getCode()))
                .build();
        List<AsyncEvent> events = asyncEventRepository.listAsyncEvents(recoverEventQuery, limit);
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        List<AsyncEvent> recovered = new ArrayList<>(events.size());
        for (AsyncEvent e : events) {
            // 根据执行次数判断回退到 待执行 或 待重试
            AsyncEventStatusEnum newStatus = (e.getExecTimes() == null || e.getExecTimes() == 0)
                    ? AsyncEventStatusEnum.WAIT_EXEC : AsyncEventStatusEnum.WAIT_RETRY;

            int affectRows = asyncEventRepository.updateEventStatus(e.getId(), AsyncEventStatusEnum.EXECUTING, newStatus);

            if (affectRows > 0) {
                e.setEventStatus(newStatus.getCode());
                recovered.add(e);
            }
        }
        return recovered;
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

        Range<LocalDateTime> timeRange = Range.<LocalDateTime>builder()
                .start(startTime)
                .end(endTime)
                .excludeStart(false)
                .excludeEnd(false)
                .build();

        return ListAsyncEventQuery.builder()
                .statusSet(statusSet)
                .expectAtRange(timeRange)
                .build();
    }

    /**
     * 判断是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

}
