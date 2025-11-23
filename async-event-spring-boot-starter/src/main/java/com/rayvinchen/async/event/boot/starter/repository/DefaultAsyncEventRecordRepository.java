package com.rayvinchen.async.event.boot.starter.repository;


import com.rayvinchen.async.event.boot.starter.mapper.AsyncEventRecordMapper;
import com.rayvinchen.async.event.boot.starter.model.DBAsyncEventRecord;
import com.rayvinchen.async.event.core.entity.AsyncEventRecord;
import com.rayvinchen.async.event.core.repository.AsyncEventRecordRepository;
import lombok.RequiredArgsConstructor;

/**
 * DefaultAsyncEventRecordRepository
 *
 * @author rayvinchen
 * @since 2025/11/8 20:35
 */
@RequiredArgsConstructor
public class DefaultAsyncEventRecordRepository implements AsyncEventRecordRepository {

    private final AsyncEventRecordMapper asyncEventRecordMapper;

    @Override
    public int addAsyncEventRecord(AsyncEventRecord record) {
        DBAsyncEventRecord model = new DBAsyncEventRecord();
        model.setEventId(record.getEventId());
        model.setExecuteStatus(record.getExecuteStatus());
        model.setExecuteTime(record.getExecuteTime());
        model.setFailReason(record.getFailReason());
        int affectRows = asyncEventRecordMapper.insert(model);
        record.setId(model.getId());
        return affectRows;
    }

}
