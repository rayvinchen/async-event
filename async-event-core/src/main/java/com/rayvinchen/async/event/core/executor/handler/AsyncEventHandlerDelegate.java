package com.rayvinchen.async.event.core.executor.handler;


import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.util.Asserts;
import com.rayvinchen.async.event.core.valobj.ExecResult;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AsyncEventHandlerDelegate
 *
 * @author rayvinchen
 * @since 2025/11/8 19:53
 */
public class AsyncEventHandlerDelegate {

    private final Map<String, AsyncEventHandler> handlerMap;

    public AsyncEventHandlerDelegate(List<AsyncEventHandler> handlers) {
        this.handlerMap = handlers.stream().collect(Collectors.toMap(AsyncEventHandler::eventType, Function.identity()));
    }

    /**
     * 处理异步事件
     *
     * @param eventType 事件类型
     * @param event 异步事件
     * @return 处理结果
     */
    public ExecResult handle(String eventType, AsyncEvent event) {
        return getHandler(eventType).handle(event);
    }

    /**
     * 是否可以重试
     *
     * @param eventType 事件类型
     * @return 是否可以重试
     */
    public boolean retryable(String eventType) {
        AsyncEventHandler handler = getHandler(eventType);
        return handler.retryable();
    }

    /**
     * 检查指定事件类型是否存在对应的处理器
     *
     * @param eventType 事件类型字符串
     * @return 如果存在对应处理器则返回true，否则返回false
     */
    public boolean existHandler(String eventType) {
        return handlerMap.containsKey(eventType);
    }

    private AsyncEventHandler getHandler(String eventType) {
        AsyncEventHandler handler = handlerMap.get(eventType);
        Asserts.notNull(handler, String.format("AsyncEventHandler not found for event type %s", eventType));

        return handler;
    }

}
