package com.rayvinchen.async.event.core.repository;


import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.valobj.ListAsyncEventQuery;

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

}
