package com.rayvinchen.async.event.core.valobj;


import lombok.Builder;
import lombok.Getter;

/**
 * Range
 *
 * @author rayvinchen
 * @since 2025/11/5 19:39
 */
@Getter
@Builder
public class Range<T extends Comparable<T>> {

    /**
     * 开始
     */
    private final T start;

    /**
     * 结束
     */
    private final T end;

    /**
     * 是否排除开始
     */
    private final boolean excludeStart;

    /**
     * 是否排除结束
     */
    private final boolean excludeEnd;

}
