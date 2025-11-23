package com.rayvinchen.async.event.core;

import com.rayvinchen.async.event.core.executor.AsyncEventExecutor;
import com.rayvinchen.async.event.core.valobj.AsyncEventExecContext;
import com.rayvinchen.async.event.core.valobj.ExecResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步事件任务包装器
 * 职责:
 * 1. 封装现有的AsyncEventExecutor执行逻辑
 * 2. 提供执行超时控制
 * 3. 异常捕获与处理
 * 4. 执行监控与统计
 * 5. 失败重试调度
 *
 * @author rayvinchen
 * @since 2025/11/28
 */
@Slf4j
public class AsyncEventTask implements Runnable {

    private final Long eventId;
    private final AsyncEventWorker worker;
    private final AsyncEventExecutor executor;

    public AsyncEventTask(Long eventId,
                          AsyncEventWorker worker,
                          AsyncEventExecutor executor) {
        this.eventId = eventId;
        this.worker = worker;
        this.executor = executor;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Start executing async event task: {}", eventId);

            // 检查执行器是否已注入
            if (executor == null) {
                log.error("AsyncEventExecutor not set for task: {}", eventId);
                handleFailure("Executor not configured", startTime);
                return;
            }

            // 构建执行上下文
            AsyncEventExecContext context = AsyncEventExecContext.builder()
                    .eventId(eventId)
                    .build();

            // 调用现有执行器执行任务
            ExecResult execResult = executor.execute(context);

            // 处理执行结果
            if (execResult.isSuccess()) {
                handleSuccess(startTime);
            } else {
                handleFailure(execResult.getFailReason(), startTime);
            }

        } catch (Exception e) {
            log.error("Unexpected error occurred while executing task: {}", eventId, e);
            handleFailure("Unexpected error: " + e.getMessage(), startTime);
        }
    }

    /**
     * 处理成功情况
     */
    private void handleSuccess(long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        // 从内存中移除任务
        worker.onTaskCompleted(eventId);

        log.info("Task executed successfully: {}, duration={}ms", eventId, duration);
    }

    /**
     * 处理失败情况
     */
    private void handleFailure(String failReason, long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        log.warn("Task execution failed: {}, reason={}, duration={}ms",
                eventId, failReason, duration);

        worker.onTaskFailed(eventId);

        log.error("Task execution failed: {}", eventId);
    }

}
