package com.rayvinchen.async.event.core.repository;


import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.valobj.ListAsyncEventQuery;

import java.time.Instant;
import java.util.List;

/**
 * AsyncEventRepository
 *
 * @author rayvinchen
 * @since 2025/11/5 18:11
 */
public interface AsyncEventRepository {

    /**
     * 查询异步事件列表
     *
     * @param query 查询条件
     * @return 异步事件列表
     */
    List<AsyncEvent> listAsyncEvents(ListAsyncEventQuery query);

    /**
     * 根据ID查询异步事件
     *
     * @param id id
     * @return 异步事件
     */
    AsyncEvent getAsyncEvent(Long id);

    /**
     * 更新事件状态
     *
     * @param id 事件ID
     * @param originStatus 原始状态
     * @param newStatus 新状态
     * @return 响应行数
     */
    int updateEventStatus(Long id, AsyncEventStatusEnum originStatus, AsyncEventStatusEnum newStatus);

    /**
     * 根据ID更新事件信息
     *
     * @param event 事件信息
     * @return 响应行数
     */
    int updateAsyncEventById(AsyncEvent event);

    /**
     * 新增异步事件
     *
     * @param event 异步事件
     * @return 响应行数
     */
    int addAsyncEvent(AsyncEvent event);

    /**
     * 周期性心跳更新：仅当事件处于 EXECUTING 时更新心跳时间。
     *
     * 实现建议使用数据库时间（如 MySQL NOW(3)）以避免时钟偏移；本参数仅为 SPI 一致性保留。
     * 返回影响行数：1 表示成功，0 表示条件不满足或事件已不在执行中。
     *
     * @param eventId     事件ID
     * @param heartbeatAt 心跳时间（可选使用）
     * @return 影响行数
     */
    int updateHeartbeat(Long eventId, Instant heartbeatAt);

}
