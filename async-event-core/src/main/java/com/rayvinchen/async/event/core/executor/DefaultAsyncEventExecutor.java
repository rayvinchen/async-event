package com.rayvinchen.async.event.core.executor;

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

import java.time.LocalDateTime;
import java.util.Objects;
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

    public DefaultAsyncEventExecutor(AsyncEventRepository asyncEventRepository,
                                     AsyncEventRecordRepository asyncEventRecordRepository,
                                     LockTemplate lockTemplate, AsyncEventHandlerDelegate handler) {
        this.asyncEventRecordRepository = asyncEventRecordRepository;
        this.asyncEventRepository = asyncEventRepository;
        this.lockTemplate = lockTemplate;
        this.handler = handler;
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
        try {
            beforeHandle(event);
            execResult = handler.handle(event.getEventType(), event);
        } catch (AsyncEventException e) {
            log.warn("执行异步任务异常. context: {}", context, e);
            execResult = ExecResult.builder().success(false).failReason(e.getMessage()).build();
        } catch (Exception e) {
            log.error("执行异步任务错误. context: {}", context, e);
            execResult = ExecResult.builder().success(false).failReason("Fail to execute async event.").build();
        } finally {
            lockTemplate.unlock(lockRecord);
        }

        afterHandle(event, execResult);
        return execResult;
    }

    private void beforeHandle(AsyncEvent event) {
        if (Objects.equals(event.getExecuteStatus(), AsyncEventStatusEnum.WAIT_EXEC.getCode())) {
            // 将任务状态从待执行改为执行中
            int affectRows = asyncEventRepository.updateEventStatus(event.getId(), AsyncEventStatusEnum.WAIT_EXEC, AsyncEventStatusEnum.EXECUTING);
            Asserts.check(affectRows > 0, String.format("[AsyncEvent] Fail to update status. before: %s. event: %d", event.getExecuteStatus(), event.getId()));
            // 更新执行时间
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecuteTime(LocalDateTime.now());
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        } else if (Objects.equals(event.getExecuteStatus(), AsyncEventStatusEnum.WAIT_RETRY.getCode())) {
            // 将任务状态从待执行改为执行中
            int affectRows = asyncEventRepository.updateEventStatus(event.getId(), AsyncEventStatusEnum.WAIT_RETRY, AsyncEventStatusEnum.EXECUTING);
            Asserts.check(affectRows > 0, String.format("[AsyncEvent] Fail to update status. before: %s. event: %d", event.getExecuteStatus(), event.getId()));
        } else if (Objects.equals(event.getExecuteStatus(), AsyncEventStatusEnum.EXECUTING.getCode())) {
            event = asyncEventRepository.getAsyncEvent(event.getId());
            // 加锁后查询数据库最新状态，如果还是执行中，说明任务执行过程出现异常，重新执行
            Asserts.check(Objects.equals(event.getExecuteStatus(), AsyncEventStatusEnum.EXECUTING.getCode()),
                    String.format("[AsyncEvent] Fail to Update status. event: %d", event.getId()));
        }
    }

    private void afterHandle(AsyncEvent event, ExecResult execResult) {
        if (Objects.equals(execResult.isSuccess(), true)) {
            afterHandleSuccess(event);
        } else {
            afterHandleFailure(event, execResult);
        }
    }

    private void afterHandleSuccess(AsyncEvent event) {
        AsyncEvent waitUpdateEvent = new AsyncEvent();
        waitUpdateEvent.setId(event.getId());
        waitUpdateEvent.setFinishedTime(LocalDateTime.now());
        waitUpdateEvent.setExecuteTimes(event.getExecuteTimes() + 1);
        waitUpdateEvent.setExecuteStatus(AsyncEventStatusEnum.EXECUTE_SUCCESS.getCode());
        asyncEventRepository.updateAsyncEventById(waitUpdateEvent);

        AsyncEventRecord record = new AsyncEventRecord();
        record.setEventId(event.getId());
        record.setExecuteStatus(AsyncEventStatusEnum.EXECUTE_SUCCESS.getCode());
        record.setExecuteTime(LocalDateTime.now());
        record.setOperator(event.getCreator());
        asyncEventRecordRepository.addAsyncEventRecord(record);
    }

    private void afterHandleFailure(AsyncEvent event, ExecResult execResult) {
        if (handler.existHandler(event.getEventType())
                && handler.retryable(event.getEventType())
                && event.getExecuteTimes() < MAX_RETRY_TIMES) {
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecuteTimes(event.getExecuteTimes() + 1);
            waitUpdateEvent.setExecuteStatus(AsyncEventStatusEnum.WAIT_RETRY.getCode());
            waitUpdateEvent.setExpectTime(getNextExecuteTime(event));
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        } else {
            AsyncEvent waitUpdateEvent = new AsyncEvent();
            waitUpdateEvent.setId(event.getId());
            waitUpdateEvent.setExecuteTimes(event.getExecuteTimes() + 1);
            waitUpdateEvent.setExecuteStatus(AsyncEventStatusEnum.EXECUTE_FAILURE.getCode());
            asyncEventRepository.updateAsyncEventById(waitUpdateEvent);
        }

        AsyncEventRecord record = new AsyncEventRecord();
        record.setEventId(event.getId());
        record.setExecuteStatus(AsyncEventStatusEnum.EXECUTE_FAILURE.getCode());
        record.setExecuteTime(LocalDateTime.now());
        record.setFailReason(execResult.getFailReason());
        record.setOperator(event.getCreator());

        asyncEventRecordRepository.addAsyncEventRecord(record);
    }

    private LocalDateTime getNextExecuteTime(AsyncEvent event) {
        int minute = (int) Math.pow(2.0, event.getExecuteTimes());
        return LocalDateTime.now().plusMinutes(minute);
    }

}
