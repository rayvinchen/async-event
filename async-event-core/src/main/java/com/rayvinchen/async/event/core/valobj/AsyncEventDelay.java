package com.rayvinchen.async.event.core.valobj;

import com.rayvinchen.async.event.core.entity.AsyncEvent;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 内存异步任务对象
 * 封装数据库中的AsyncEvent,添加内存调度所需的额外信息
 *
 * @author rayvinchen
 * @since 2025/11/28
 */
@Data
public class AsyncEventDelay implements Delayed {

    /**
     * 事件ID
     */
    private Long eventId;

    /**
     * 期望执行时间的毫秒时间戳(用于Delayed接口)
     */
    private long executeTimeMillis;

    public AsyncEventDelay(AsyncEvent asyncEvent) {
        this.eventId = asyncEvent.getId();
        this.executeTimeMillis = toMillis(asyncEvent.getExpectExecAt());
    }

    /**
     * 获取延迟时间(用于DelayQueue)
     */
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTimeMillis - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 比较方法(用于优先级队列排序)
     * Delayed接口要求实现compareTo(Delayed)方法
     */
    @Override
    public int compareTo(Delayed other) {
        if (!(other instanceof AsyncEventDelay that)) {
            return 0;
        }
        if (this.executeTimeMillis < that.executeTimeMillis) {
            return -1;
        } else if (this.executeTimeMillis > that.executeTimeMillis) {
            return 1;
        }
        return Long.compare(this.eventId, that.eventId);
    }

    /**
     * 将LocalDateTime转换为毫秒时间戳
     */
    private long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public String toString() {
        return "MemoryAsyncTask{" +
                "eventId=" + eventId +
                '}';
    }
}
