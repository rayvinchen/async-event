package com.rayvinchen.async.event.core.executor.handler;


import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.valobj.ExecResult;

/**
 * AsyncEventHandler
 *
 * @author rayvinchen
 * @since 2025/11/8 19:14
 */
public interface AsyncEventHandler {

    /**
     * 处理异步事件
     *
     * @param event 事件
     * @return 处理结果
     */
    ExecResult handle(AsyncEvent event);

    /**
     * 是否可以重试
     *
     * @return 是否可重试
     */
    boolean retryable();

    /**
     * 执行器处理的事件类型
     *
     * @return 事件类型
     */
    String eventType();

}
