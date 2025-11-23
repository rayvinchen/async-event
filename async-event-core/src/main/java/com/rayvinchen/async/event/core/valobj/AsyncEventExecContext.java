package com.rayvinchen.async.event.core.valobj;


import lombok.Builder;
import lombok.Getter;

/**
 * AsyncEventExecContext
 *
 * @author rayvinchen
 * @since 2025/11/5 19:51
 */
@Getter
@Builder
public class AsyncEventExecContext {

    private final Long eventId;

}
