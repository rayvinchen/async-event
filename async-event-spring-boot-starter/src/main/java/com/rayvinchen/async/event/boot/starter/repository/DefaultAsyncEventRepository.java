package com.rayvinchen.async.event.boot.starter.repository;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rayvinchen.async.event.boot.starter.mapper.AsyncEventMapper;
import com.rayvinchen.async.event.boot.starter.model.DBAsyncEvent;
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.enums.AsyncEventStatusEnum;
import com.rayvinchen.async.event.core.repository.AsyncEventRepository;
import com.rayvinchen.async.event.core.valobj.ListAsyncEventQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.time.Instant;

/**
 * DefaultAsyncEventRepository
 *
 * @author rayvinchen
 * @since 2025/11/8 20:35
 */
@RequiredArgsConstructor
public class DefaultAsyncEventRepository implements AsyncEventRepository {

    private final AsyncEventMapper asyncEventMapper;

    @Override
    public List<AsyncEvent> listAsyncEvents(ListAsyncEventQuery query) {
        LambdaQueryWrapper<DBAsyncEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(!CollectionUtils.isEmpty(query.getStatusSet()), DBAsyncEvent::getEventStatus, query.getStatusSet());

        if (Objects.nonNull(query.getExpectTimeRange())) {
            if (Objects.nonNull(query.getExpectTimeRange().getStart())) {
                if (query.getExpectTimeRange().isExcludeStart()) {
                    queryWrapper.gt(DBAsyncEvent::getExpectExecAt, query.getExpectTimeRange().getStart());
                } else {
                    queryWrapper.ge(DBAsyncEvent::getExpectExecAt, query.getExpectTimeRange().getStart());
                }
            }
            if (Objects.nonNull(query.getExpectTimeRange().getEnd())) {
                if (query.getExpectTimeRange().isExcludeEnd()) {
                    queryWrapper.lt(DBAsyncEvent::getExpectExecAt, query.getExpectTimeRange().getEnd());
                } else {
                    queryWrapper.le(DBAsyncEvent::getExpectExecAt, query.getExpectTimeRange().getEnd());
                }
            }
        }
        List<DBAsyncEvent> models = asyncEventMapper.selectList(queryWrapper);
        return models.stream().map(this::toEntity).toList();
    }

    @Override
    public AsyncEvent getAsyncEvent(Long id) {
        DBAsyncEvent model = asyncEventMapper.selectById(id);
        return toEntity(model);
    }

    @Override
    public int updateEventStatus(Long id, AsyncEventStatusEnum originStatus, AsyncEventStatusEnum newStatus) {
        LambdaUpdateWrapper<DBAsyncEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(DBAsyncEvent::getEventStatus, newStatus.getCode());
        wrapper.eq(DBAsyncEvent::getId, id)
                .eq(DBAsyncEvent::getEventStatus, originStatus.getCode());

        return asyncEventMapper.update(null, wrapper);
    }

    @Override
    public int updateAsyncEventById(AsyncEvent event) {
        DBAsyncEvent model = toModel(event);
        return asyncEventMapper.updateById(model);
    }

    @Override
    public int addAsyncEvent(AsyncEvent event) {
        DBAsyncEvent model = toModel(event);
        int affectRows = asyncEventMapper.insert(model);
        event.setId(model.getId());
        return affectRows;
    }

    @Override
    public int updateHeartbeat(Long eventId, Instant heartbeatAt) {
        // 仅当事件处于执行中(3)时更新心跳；为避免本地时钟偏移，直接使用数据库时间函数
        LambdaUpdateWrapper<DBAsyncEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DBAsyncEvent::getId, eventId)
                .eq(DBAsyncEvent::getEventStatus, AsyncEventStatusEnum.EXECUTING.getCode())
                // 使用 SQL 直接设置 NOW(3)
                .setSql("heartbeat_at = NOW(3)");
        return asyncEventMapper.update(null, wrapper);
    }

    private AsyncEvent toEntity(DBAsyncEvent model) {
        AsyncEvent entity = new AsyncEvent();
        entity.setId(model.getId());
        entity.setEventType(model.getEventType());
        entity.setEventData(model.getEventData());
        entity.setExpectExecAt(model.getExpectExecAt());
        entity.setExecAt(model.getExecAt());
        entity.setFinishedAt(model.getFinishedAt());
        entity.setExecTimes(model.getExecTimes());
        entity.setEventStatus(model.getEventStatus());
        entity.setCreator(model.getCreator());
        return entity;
    }

    private DBAsyncEvent toModel(AsyncEvent event) {
        DBAsyncEvent model = new DBAsyncEvent();
        model.setId(event.getId());
        model.setEventType(event.getEventType());
        model.setEventData(event.getEventData());
        model.setExpectExecAt(event.getExpectExecAt());
        model.setExecAt(event.getExecAt());
        model.setFinishedAt(event.getFinishedAt());
        model.setExecTimes(event.getExecTimes());
        model.setEventStatus(event.getEventStatus());
        model.setCreator(event.getCreator());
        return model;
    }

}
