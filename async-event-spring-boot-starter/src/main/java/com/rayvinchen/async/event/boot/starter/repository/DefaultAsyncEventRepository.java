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
        queryWrapper.in(!CollectionUtils.isEmpty(query.getStatusSet()), DBAsyncEvent::getExecuteStatus, query.getStatusSet());

        if (Objects.nonNull(query.getExpectTimeRange())) {
            if (Objects.nonNull(query.getExpectTimeRange().getStart())) {
                if (query.getExpectTimeRange().isExcludeStart()) {
                    queryWrapper.gt(DBAsyncEvent::getExpectTime, query.getExpectTimeRange().getStart());
                } else {
                    queryWrapper.ge(DBAsyncEvent::getExpectTime, query.getExpectTimeRange().getStart());
                }
            }
            if (Objects.nonNull(query.getExpectTimeRange().getEnd())) {
                if (query.getExpectTimeRange().isExcludeEnd()) {
                    queryWrapper.lt(DBAsyncEvent::getExpectTime, query.getExpectTimeRange().getEnd());
                } else {
                    queryWrapper.le(DBAsyncEvent::getExpectTime, query.getExpectTimeRange().getEnd());
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
        wrapper.set(DBAsyncEvent::getExecuteStatus, newStatus.getCode());
        wrapper.eq(DBAsyncEvent::getId, id)
                .eq(DBAsyncEvent::getExecuteStatus, originStatus.getCode());

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

    private AsyncEvent toEntity(DBAsyncEvent model) {
        AsyncEvent entity = new AsyncEvent();
        entity.setId(model.getId());
        entity.setEventType(model.getEventType());
        entity.setEventData(model.getEventData());
        entity.setExpectTime(model.getExpectTime());
        entity.setExecuteTime(model.getExecuteTime());
        entity.setFinishedTime(model.getFinishedTime());
        entity.setExecuteTimes(model.getExecuteTimes());
        entity.setExecuteStatus(model.getExecuteStatus());
        entity.setCreator(model.getCreator());
        return entity;
    }

    private DBAsyncEvent toModel(AsyncEvent event) {
        DBAsyncEvent model = new DBAsyncEvent();
        model.setId(event.getId());
        model.setEventType(event.getEventType());
        model.setEventData(event.getEventData());
        model.setExpectTime(event.getExpectTime());
        model.setExecuteTime(event.getExecuteTime());
        model.setFinishedTime(event.getFinishedTime());
        model.setExecuteTimes(event.getExecuteTimes());
        model.setExecuteStatus(event.getExecuteStatus());
        model.setCreator(event.getCreator());
        return model;
    }

}
