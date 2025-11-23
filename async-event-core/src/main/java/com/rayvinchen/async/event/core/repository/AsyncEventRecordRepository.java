package com.rayvinchen.async.event.core.repository;


import com.rayvinchen.async.event.core.entity.AsyncEventRecord;

/**
 * AsyncEventRecordRepository
 *
 * @author rayvinchen
 * @since 2025/11/5 18:12
 */
public interface AsyncEventRecordRepository {

    /**
     * 新增异步事件记录
     *
     * @param record 记录
     * @return 响应行数
     */
    int addAsyncEventRecord(AsyncEventRecord record);

}
