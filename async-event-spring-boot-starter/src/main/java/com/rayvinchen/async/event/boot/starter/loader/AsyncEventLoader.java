package com.rayvinchen.async.event.boot.starter.loader;

import com.rayvinchen.async.event.core.entity.AsyncEvent;

import java.util.List;


/**
 * 异步事件加载器
 *
 * @author rayvinchen
 * @since 2025/11/28 18:07
 */
public interface AsyncEventLoader {
    /**
     * 正常场景：按时间窗口预加载待执行/待重试的异步任务。
     *
     * @param lookAheadSeconds 向前看的时间窗口（秒），会加载 [now, now + lookAheadSeconds] 期望执行时间内的任务
     * @param limit            最多加载数量
     * @return 符合条件的异步任务列表（未入队，由调用方决定后续处理）
     */
    List<AsyncEvent> loadUpcomingEvents(int lookAheadSeconds, int limit);

    /**
     * 异常场景恢复：针对执行中但超过心跳阈值未更新（疑似中断）的任务进行回收，
     * 将其状态改为待执行（execTimes=0）或待重试（execTimes>0），并返回可处理列表。
     *
     * @param staleSeconds 判定“失联”的阈值（秒），通常取心跳间隔的 3 倍
     * @param limit        最多处理数量
     * @return 被回收并改回可调度状态的异步任务列表
     */
    List<AsyncEvent> loadRecoverEvents(int staleSeconds, int limit);
}
