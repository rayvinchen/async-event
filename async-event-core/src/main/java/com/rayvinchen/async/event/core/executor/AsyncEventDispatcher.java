package com.rayvinchen.async.event.core.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.properties.ThreadPoolProperties;
import com.rayvinchen.async.event.core.valobj.AsyncEventDelay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步事件任务调度器
 * 职责:
 * 1. 管理内存中的任务队列
 * 2. 使用DelayQueue实现延迟调度
 * 3. 将到期任务提交到线程池执行
 * 4. 监控队列容量,实现背压机制
 *
 * @author rayvinchen
 * @since 2025/11/28
 */
@Slf4j
public class AsyncEventDispatcher implements InitializingBean, DisposableBean {

    private final ThreadPoolProperties properties;
    private final AsyncEventExecutor executor;

    private final DelayQueue<AsyncEventDelay> taskQueue;
    private final ConcurrentHashMap<Long, AsyncEventDelay> loadedTasks;
    private final ConcurrentHashMap<Long, Future<?>> executingTasks;

    private Thread dispatcherThread;
    private ThreadPoolExecutor threadPool;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public AsyncEventDispatcher(ThreadPoolProperties properties,
                                AsyncEventExecutor executor) {
        this.properties = properties;
        this.executor = executor;

        this.taskQueue = new DelayQueue<>();
        this.loadedTasks = new ConcurrentHashMap<>();
        this.executingTasks = new ConcurrentHashMap<>();
    }

    /**
     * 启动调度器
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.threadPool = createThreadPool(properties);

            dispatcherThread = new Thread(this::dispatchLoop, "async-event-dispatcher");
            dispatcherThread.setDaemon(false);
            dispatcherThread.start();
            log.info("AsyncEventDispatcher started.");
        }
    }

    /**
     * 停止调度器
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // 1. 停止分发线程
            if (dispatcherThread != null) {
                dispatcherThread.interrupt();
                try {
                    dispatcherThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 2. 关闭线程池 (等待已提交任务完成)
            if (threadPool != null) {
                threadPool.shutdown();
                try {
                    int timeoutSeconds = properties.getShutdownTimeoutSeconds();
                    if (!threadPool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        log.warn("ThreadPool did not terminate in {}s, forcing shutdown...", timeoutSeconds);
                        threadPool.shutdownNow();

                        // 再等待一段时间
                        if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                            log.error("ThreadPool did not terminate after force shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for ThreadPool termination");
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            log.info("AsyncEventDispatcher stopped.");
        }
    }

    /**
     * 调度循环
     * 使用DelayQueue的take()方法,会自动阻塞到有任务到期
     */
    private void dispatchLoop() {
        while (isRunning()) {
            try {
                // 阻塞获取到期任务 (take()方法会自动等待直到有任务到期)
                AsyncEventDelay task = taskQueue.take();

                // 创建任务包装器
                AsyncEventTaskWrapper taskWrapper = new AsyncEventTaskWrapper(task, this, executor);

                // 提交到线程池执行
                Future<?> future = threadPool.submit(taskWrapper);

                // 记录执行中任务
                executingTasks.put(task.getEventId(), future);

                log.debug("Task dispatched to thread pool: {}", task);

            } catch (InterruptedException e) {
                log.info("AsyncEventDispatcher interrupted.");
                Thread.currentThread().interrupt();
                break;
            } catch (RejectedExecutionException e) {
                log.error("Task rejected by thread pool (pool may be shutting down)", e);
            } catch (Exception e) {
                log.error("Error occurred while dispatching task", e);
            }
        }
    }

    /**
     * 提交任务
     *
     * @param event 事件
     */
    public void offer(AsyncEvent event) {
        if (loadedTasks.containsKey(event.getId())) {
            return;
        }

        AsyncEventDelay task = new AsyncEventDelay(event);

        taskQueue.offer(task);
        loadedTasks.put(task.getEventId(), task);

        log.info("Task Offered: {}", event);
    }

    /**
     * 任务完成回调
     */
    public void onTaskCompleted(Long eventId) {
        executingTasks.remove(eventId);
        loadedTasks.remove(eventId);
        log.debug("Task completed and removed from memory: eventId={}", eventId);
    }

    /**
     * 任务失败回调
     */
    public void onTaskFailed(Long eventId) {
        executingTasks.remove(eventId);
        loadedTasks.remove(eventId);
        log.debug("Task failed: eventId={}", eventId);
    }

    /**
     * 创建异步事件处理线程池
     */
    private ThreadPoolExecutor createThreadPool(ThreadPoolProperties config) {

        int corePoolSize = config.getActualCorePoolSize();
        int maximumPoolSize = config.getActualMaxPoolSize();
        long keepAliveTime = config.getKeepAliveSeconds();
        int queueCapacity = config.getQueueCapacity();
        String threadNamePrefix = config.getThreadNamePrefix();

        // 创建线程工厂
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(threadNamePrefix + "-%d")
                .setDaemon(false)
                .setUncaughtExceptionHandler((thread, throwable) ->
                        log.error("Uncaught exception in thread: {}", thread.getName(), throwable))
                .build();

        // 创建工作队列
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

        // 使用CallerRunsPolicy保证任务不丢失
        // 当线程池满时,由调用线程执行任务,实现背压机制
        ThreadPoolExecutor.CallerRunsPolicy rejectedExecutionHandler =
                new ThreadPoolExecutor.CallerRunsPolicy();

        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectedExecutionHandler
        );

        // 预启动核心线程
        executor.prestartAllCoreThreads();

        log.info("AsyncEvent ThreadPool created: corePoolSize={}, maxPoolSize={}, queueCapacity={}, keepAliveSeconds={}",
                corePoolSize, maximumPoolSize, queueCapacity, keepAliveTime);

        return executor;
    }

    /**
     * 判断是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取队列大小
     */
    public int getTaskQueueSize() {
        return taskQueue.size();
    }

    /**
     * 获取已加载的任务数量
     *
     * @return 已加载的任务数量
     */
    public int getLoadedTaskCount() {
        return loadedTasks.size();
    }

    /**
     * 获取执行中任务数量
     */
    public int getExecutingTaskCount() {
        return executingTasks.size();
    }

    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        if (threadPool == null) {
            return "Not initialized";
        }

        return String.format("ThreadPool[active=%d, poolSize=%d, queueSize=%d, completed=%d]",
                threadPool.getActiveCount(),
                threadPool.getPoolSize(),
                threadPool.getQueue().size(),
                threadPool.getCompletedTaskCount());
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }
}
