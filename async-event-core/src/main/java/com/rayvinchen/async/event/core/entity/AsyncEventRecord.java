package com.rayvinchen.async.event.core.entity;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * AsyncEventRecord
 *
 * @author rayvinchen
 * @since 2025/11/5 18:09
 */
@Data
public class AsyncEventRecord {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 异步事件ID
     */
    private Long eventId;

    /**
     * 事件状态
     */
    private Byte eventStatus;

    /**
     * 执行时间
     */
    private LocalDateTime execAt;

    /**
     * 执行失败原因
     */
    private String failReason;

    /**
     * 操作人
     */
    private String operator;

}
