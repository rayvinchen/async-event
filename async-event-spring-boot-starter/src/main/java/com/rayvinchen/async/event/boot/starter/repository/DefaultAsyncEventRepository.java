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

import java.time.Instant;
import java.util.Collections;
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
    public List<AsyncEvent> listAsyncEvents(ListAsyncEventQuery query, int limit) {
        if (!query.needQuery()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DBAsyncEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(!CollectionUtils.isEmpty(query.getStatusSet()), DBAsyncEvent::getEventStatus, query.getStatusSet());

        if (Objects.nonNull(query.getExpectAtRange())) {
            if (Objects.nonNull(query.getExpectAtRange().getStart())) {
                if (query.getExpectAtRange().isExcludeStart()) {
                    queryWrapper.gt(DBAsyncEvent::getExpectExecAt, query.getExpectAtRange().getStart());
                } else {
                    queryWrapper.ge(DBAsyncEvent::getExpectExecAt, query.getExpectAtRange().getStart());
                }
            }
            if (Objects.nonNull(query.getExpectAtRange().getEnd())) {
                if (query.getExpectAtRange().isExcludeEnd()) {
                    queryWrapper.lt(DBAsyncEvent::getExpectExecAt, query.getExpectAtRange().getEnd());
                } else {
                    queryWrapper.le(DBAsyncEvent::getExpectExecAt, query.getExpectAtRange().getEnd());
                }
            }
        }

        if (Objects.nonNull(query.getHeartbeatAtRange())) {
            if (Objects.nonNull(query.getHeartbeatAtRange().getStart())) {
                if (query.getHeartbeatAtRange().isExcludeStart()) {
                    queryWrapper.gt(DBAsyncEvent::getHeartbeatAt, query.getHeartbeatAtRange().getStart());
                } else {
                    queryWrapper.ge(DBAsyncEvent::getHeartbeatAt, query.getHeartbeatAtRange().getStart());
                }
            }
            if (Objects.nonNull(query.getHeartbeatAtRange().getEnd())) {
                if (query.getHeartbeatAtRange().isExcludeEnd()) {
                    queryWrapper.lt(DBAsyncEvent::getHeartbeatAt, query.getHeartbeatAtRange().getEnd());
                } else {
                    queryWrapper.le(DBAsyncEvent::getHeartbeatAt, query.getHeartbeatAtRange().getEnd());
                }
            }
        }

        // 排序策略：
        // - 常规加载：按 expect_exec_at 升序（越早到期越先被加载）
        // - 恢复加载：按 heartbeat_at 升序（心跳越久远越先被恢复）；MySQL 在 ASC 时 NULL 会排在最前
        if (Objects.nonNull(query.getHeartbeatAtRange())) {
            queryWrapper.orderByAsc(DBAsyncEvent::getHeartbeatAt);
        } else if (Objects.nonNull(query.getExpectAtRange())) {
            queryWrapper.orderByAsc(DBAsyncEvent::getExpectExecAt);
        } else {
            // 默认按期望执行时间升序
            queryWrapper.orderByAsc(DBAsyncEvent::getExpectExecAt);
        }

        queryWrapper.last(limit > 0 ? "LIMIT " + limit : null);

        List<DBAsyncEvent> models = asyncEventMapper.selectList(queryWrapper);
        return models.stream().map(this::toEntity).toList();
    }

    @Override
    public AsyncEvent getAsyncEvent(Long id) {
        if (Objects.isNull(id)) {
            return null;
        }

        DBAsyncEvent model = asyncEventMapper.selectById(id);
        return toEntity(model);
    }

    @Override
    public int updateEventStatus(Long id, AsyncEventStatusEnum originStatus, AsyncEventStatusEnum newStatus) {
        if (Objects.isNull(id)) {
            return 0;
        }

        LambdaUpdateWrapper<DBAsyncEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(DBAsyncEvent::getEventStatus, newStatus.getCode());
        wrapper.eq(DBAsyncEvent::getId, id)
                .eq(DBAsyncEvent::getEventStatus, originStatus.getCode());

        return asyncEventMapper.update(null, wrapper);
    }

    @Override
    public int updateAsyncEventById(AsyncEvent event) {
        if (Objects.isNull(event) || Objects.isNull(event.getId())) {
            return 0;
        }

        DBAsyncEvent model = toModel(event);
        return asyncEventMapper.updateById(model);
    }

    @Override
    public int addAsyncEvent(AsyncEvent event) {
        DBAsyncEvent model = toModel(event);
        model.setId(null);
        int affectRows = asyncEventMapper.insert(model);
        event.setId(model.getId());
        return affectRows;
    }

    @Override
    public int updateHeartbeat(Long eventId, Instant heartbeatAt) {
        if (Objects.isNull(eventId)) {
            return 0;
        }

        // 仅当事件处于执行中(3)时更新心跳；为避免本地时钟偏移，直接使用数据库时间函数
        LambdaUpdateWrapper<DBAsyncEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DBAsyncEvent::getId, eventId)
                .eq(DBAsyncEvent::getEventStatus, AsyncEventStatusEnum.EXECUTING.getCode())
                // 使用 SQL 直接设置 NOW()
                .setSql("heartbeat_at = NOW()");
        return asyncEventMapper.update(null, wrapper);
    }

    private AsyncEvent toEntity(DBAsyncEvent model) {
        AsyncEvent entity = new AsyncEvent();
        entity.setId(model.getId());
        entity.setEventType(model.getEventType());
        entity.setEventData(model.getEventData());
        entity.setExpectExecAt(model.getExpectExecAt());
        entity.setExecAt(model.getExecAt());
        entity.setHeartbeatAt(model.getHeartbeatAt());
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
        model.setHeartbeatAt(event.getHeartbeatAt());
        model.setFinishedAt(event.getFinishedAt());
        model.setExecTimes(event.getExecTimes());
        model.setEventStatus(event.getEventStatus());
        model.setCreator(event.getCreator());
        return model;
    }

}
