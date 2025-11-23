package com.rayvinchen.async.event.core.template;


import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.entity.AsyncEventRecord;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.exception.AsyncEventException;
import com.rayvinchen.async.event.core.executor.AsyncEventDispatcher;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * AsyncEventTemplate
 *
 * @author rayvinchen
 * @since 2025/11/22 10:26
 */
@RequiredArgsConstructor
public class AsyncEventTemplate {

    private final AsyncEventRepository asyncEventRepository;
    private final AsyncEventRecordRepository asyncEventRecordRepository;
    private final AsyncEventDispatcher asyncEventDispatcher;

    /**
     * 注册异步事件
     * 请保证异步事件的注册与本地事务在一个事务内
     *
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param expectExecuteTime 期望执行的时间点
     * @param creator 创建人
     */
    public void registerAsyncEvent(String eventType, String eventData, LocalDateTime expectExecuteTime, String creator) {
        AsyncEvent asyncEvent = new AsyncEvent();
        asyncEvent.setEventType(eventType);
        asyncEvent.setEventData(eventData);
        asyncEvent.setExecuteStatus(AsyncEventStatusEnum.WAIT_EXEC.getCode());
        asyncEvent.setExpectTime(expectExecuteTime);
        asyncEvent.setCreator(creator);
        asyncEventRepository.addAsyncEvent(asyncEvent);

        AsyncEventRecord asyncEventRecord = new AsyncEventRecord();
        asyncEventRecord.setEventId(asyncEvent.getId());
        asyncEventRecord.setExecuteStatus(asyncEvent.getExecuteStatus());
        asyncEventRecord.setOperator(creator);
        asyncEventRecordRepository.addAsyncEventRecord(asyncEventRecord);

        // 如果达到执行时间或即将达到(1分钟内),则直接丢入待执行事件池中,等待执行
        // 这样可以减少数据库扫描压力,让新注册的任务快速进入执行队列
        if (shouldOfferToDispatcher(expectExecuteTime)) {
            asyncEventDispatcher.offer(asyncEvent);
        }
    }

    /**
     * 取消异步事件
     *
     * @param eventId 事件ID
     * @param operator 操作人
     */
    public void cancelAsyncEvent(Long eventId, String operator) {
        int affectRows = asyncEventRepository.updateEventStatus(eventId,
                AsyncEventStatusEnum.WAIT_EXEC, AsyncEventStatusEnum.CANCEL);
        if (affectRows == 0) {
            throw new AsyncEventException("当前异步事件不为待执行状态，无法取消");
        }
        AsyncEvent waitUpdateEvent = new AsyncEvent();
        waitUpdateEvent.setId(eventId);
        waitUpdateEvent.setExecuteStatus(AsyncEventStatusEnum.CANCEL.getCode());
        asyncEventRepository.updateAsyncEventById(waitUpdateEvent);

        AsyncEventRecord asyncEventRecord = new AsyncEventRecord();
        asyncEventRecord.setEventId(eventId);
        asyncEventRecord.setExecuteStatus(waitUpdateEvent.getExecuteStatus());
        asyncEventRecord.setOperator(operator);
        asyncEventRecordRepository.addAsyncEventRecord(asyncEventRecord);
    }

    /**
     * 判断是否应该立即将任务提交到Dispatcher
     * 策略: 如果期望执行时间在当前时间之前或1分钟之内,则立即提交
     * 这样可以:
     * 1. 让立即执行的任务快速进入执行队列,无需等待Loader扫描
     * 2. 让即将执行的任务提前加载到内存,减少延迟
     * 3. 对于较远未来的任务,仍然由Loader定期扫描加载,避免内存占用过大
     *
     * @param expectExecuteTime 期望执行时间
     * @return 是否应该立即提交
     */
    private boolean shouldOfferToDispatcher(LocalDateTime expectExecuteTime) {
        if (expectExecuteTime == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(1);

        // 如果期望执行时间在当前时间之前,或在1分钟之内,则立即提交
        return !expectExecuteTime.isAfter(threshold);
    }

}
