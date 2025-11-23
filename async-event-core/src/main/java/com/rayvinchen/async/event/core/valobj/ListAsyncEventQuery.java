package com.rayvinchen.async.event.core.valobj;


import lombok.Builder;
import lombok.Getter;

import java.util.Date;
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
    private final Range<Date> expectTimeRange;

}
