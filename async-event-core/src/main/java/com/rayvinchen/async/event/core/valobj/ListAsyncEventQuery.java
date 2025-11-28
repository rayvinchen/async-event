package com.rayvinchen.async.event.core.valobj;


import lombok.Builder;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * ListAsyncEventQuery
 *
 * @author rayvinchen
 * @since 2025/11/5 19:38
 */
@Getter
@Builder
public class ListAsyncEventQuery {

    /**
     * 状态
     */
    private final Set<Byte> statusSet;

    /**
     * 期望执行时间范围
     */
    private final Range<LocalDateTime> expectAtRange;

    /**
     * 心跳时间范围
     */
    private final Range<LocalDateTime> heartbeatAtRange;

    public boolean needQuery() {
        return !CollectionUtils.isEmpty(statusSet) || Objects.nonNull(expectAtRange) || Objects.nonNull(heartbeatAtRange);
    }

}
