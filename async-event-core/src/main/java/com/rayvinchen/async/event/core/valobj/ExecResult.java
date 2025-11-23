package com.rayvinchen.async.event.core.valobj;


import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * ExecResult
 *
 * @author rayvinchen
 * @since 2025/11/5 19:50
 */
@Getter
@Builder
@ToString
public class ExecResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 失败原因
     */
    private String failReason;

}
