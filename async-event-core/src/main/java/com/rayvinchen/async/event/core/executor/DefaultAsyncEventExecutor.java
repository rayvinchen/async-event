package com.rayvinchen.async.event.core.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.entity.AsyncEventRecord;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.exception.AsyncEventException;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandlerDelegate;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import com.rayvinchen.async.event.core.template.LockTemplate;
import com.rayvinchen.async.event.core.util.Asserts;
import com.rayvinchen.async.event.core.valobj.AsyncEventExecContext;
import com.rayvinchen.async.event.core.valobj.ExecResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AbstractAsyncEventExecutor
 *
 * @author rayvinchen
 * @since 2025/11/5 19:49
 */
@Slf4j
public class DefaultAsyncEventExecutor implements AsyncEventExecutor {

    private final AsyncEventRecordRepository asyncEventRecordRepository;

    private final AsyncEventRepository asyncEventRepository;

    private final LockTemplate lockTemplate;

    private final AsyncEventHandlerDelegate handler;

    private static final int MAX_RETRY_TIMES = 5;

    // 心跳调度相关
    private final int heartbeatIntervalSeconds;
    private final ScheduledExecutorService heartbeatScheduler;

    public DefaultAsyncEventExecutor(AsyncEventRepository asyncEventRepository,
                                     AsyncEventRecordRepository asyncEventRecordRepository,
                                     LockTemplate lockTemplate,
                                     AsyncEventHandlerDelegate handler,
                                     int heartbeatIntervalSeconds) {
        this.asyncEventRecordRepository = asyncEventRecordRepository;
        this.asyncEventRepository = asyncEventRepository;
        this.lockTemplate = lockTemplate;
        this.handler = handler;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds > 0 ? heartbeatIntervalSeconds : 10;

        // 心跳调度线程池（单线程足够，主要是周期性小写操作）
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder().setNameFormat("async-heartbeat-%d").setDaemon(true).build());
        scheduler.setRemoveOnCancelPolicy(true);
        this.heartbeatScheduler = scheduler;
    }

    @Override
    public ExecResult execute(AsyncEventExecContext context) {
        log.debug("Start execute AsyncEvent. context: {}", context);

        Long eventId = context.getEventId();

        // 加分布式锁
        String lockKey = String.join(":", "ASYNC-EVENT", "EVENT-ID-" + eventId);
        LockTemplate.LockRecord lockRecord = lockTemplate.tryLock(lockKey, 0, 0, TimeUnit.MILLISECONDS);
        if (lockRecord == null) {
            return ExecResult.builder().success(false).failReason("Failed to acquire lock.").build();
        }

        AsyncEvent event = asyncEventRepository.getAsyncEvent(eventId);
        ExecResult execResult;
        ScheduledFuture<?> hbFuture = null;
        try {
            beforeHandle(event);

            // 启动心跳（仅在任务进入执行中后启动）
            hbFuture = heartbeatScheduler.scheduleAtFixedRate(() -> safeUpdateHeartbeat(eventId),
                    0, heartbeatIntervalSeconds, TimeUnit.SECONDS);

            execResult = handler.handle(event.getEventType(), event);
            afterHandleSuccess(event);
        } catch (AsyncEventException e) {
            log.warn("执行异步任务异常. context: {}", context, e);
            execResult = ExecResult.builder().success(false).failReason(e.getMessage()).build();
            afterHandleFailure(event, execResult);
        } catch (Exception e) {
            log.error("执行异步任务错误. context: {}", context, e);
            execResult = ExecResult.builder().success(false).failReason("Fail to execute async event.").build();
            afterHandleFailure(event, execResult);
        } finally {
            // 取消心跳
            if (hbFuture != null) {
                try {
                    hbFuture.cancel(true);
                } catch (Exception ignore) {
                }
            }
            lockTemplate.unlock(lockRecord);
        }

        return execResult;
    }

    /**
     * 安全更新心跳，任何异常均只记录日志，不影响业务执行
     */
    private void safeUpdateHeartbeat(Long eventId) {
        try {
            int n = asyncEventRepository.updateHeartbeat(eventId, Instant.now());
            if (n == 0) {
                log.debug("[heartbeat] no-op, event not in EXECUTING. eventId={}", eventId);
            }
        } catch (Exception e) {
            log.warn("[heartbeat] update failed. eventId={}", eventId, e);
        }
    }

    private void beforeHandle(AsyncEvent event) {
        if (Objects.equals(event.getEventStatus(), AsyncEventStatusEnum.WAIT_EXEC.getCode())) {
            // 将任务状态从待执行改为执行中
            int affectRows = asyncEventRepository.updateEventStatus(event.getId(), AsyncEventStatusEnum.WAIT_EXEC, AsyncEventStatusEnum.EXECUTING);
            Asserts.check(affectRows > 0, String.format("[AsyncEvent] Fail to update status. before: %s. event: %d", event.getEventStatus(), event.getId()));
            // 更新执行时间
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecAt(LocalDateTime.now());
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        } else if (Objects.equals(event.getEventStatus(), AsyncEventStatusEnum.WAIT_RETRY.getCode())) {
            // 将任务状态从待执行改为执行中
            int affectRows = asyncEventRepository.updateEventStatus(event.getId(), AsyncEventStatusEnum.WAIT_RETRY, AsyncEventStatusEnum.EXECUTING);
            Asserts.check(affectRows > 0, String.format("[AsyncEvent] Fail to update status. before: %s. event: %d", event.getEventStatus(), event.getId()));
        }

        event.setEventStatus(AsyncEventStatusEnum.EXECUTING.getCode());
    }

    private void afterHandleSuccess(AsyncEvent event) {
        AsyncEvent waitUpdateEvent = new AsyncEvent();
        waitUpdateEvent.setId(event.getId());
        waitUpdateEvent.setFinishedAt(LocalDateTime.now());
        waitUpdateEvent.setExecTimes(event.getExecTimes() + 1);
        waitUpdateEvent.setEventStatus(AsyncEventStatusEnum.EXECUTE_SUCCESS.getCode());
        asyncEventRepository.updateAsyncEventById(waitUpdateEvent);

        AsyncEventRecord record = new AsyncEventRecord();
        record.setEventId(event.getId());
        record.setEventStatus(AsyncEventStatusEnum.EXECUTE_SUCCESS.getCode());
        record.setExecAt(LocalDateTime.now());
        record.setOperator(event.getCreator());
        asyncEventRecordRepository.addAsyncEventRecord(record);
    }

    private void afterHandleFailure(AsyncEvent event, ExecResult execResult) {
        if (handler.existHandler(event.getEventType())
                && handler.retryable(event.getEventType())
                && event.getExecTimes() < MAX_RETRY_TIMES) {
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecTimes(event.getExecTimes() + 1);
            waitUpdateEvent.setEventStatus(AsyncEventStatusEnum.WAIT_RETRY.getCode());
            waitUpdateEvent.setExpectExecAt(getNextExecuteTime(event));
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        } else {
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecTimes(event.getExecTimes() + 1);
            waitUpdateEvent.setEventStatus(AsyncEventStatusEnum.EXECUTE_FAILURE.getCode());
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        }

        AsyncEventRecord record = new AsyncEventRecord();
        record.setEventId(event.getId());
        record.setEventStatus(AsyncEventStatusEnum.EXECUTE_FAILURE.getCode());
        record.setExecAt(LocalDateTime.now());
        record.setFailReason(execResult.getFailReason());
        record.setOperator(event.getCreator());

        asyncEventRecordRepository.addAsyncEventRecord(record);
    }

    private LocalDateTime getNextExecuteTime(AsyncEvent event) {
        int minute = (int) Math.pow(2.0, event.getExecTimes());
        return LocalDateTime.now().plusMinutes(minute);
    }

}
