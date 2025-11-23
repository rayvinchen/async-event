package com.rayvinchen.async.event.core.entity;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * AsyncEvent
 *
 * @author rayvinchen
 * @since 2025/11/5 18:09
 */
@Data
public class AsyncEvent {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 异步事件类型
     */
    private String eventType;

    /**
     * 事件数据
     */
    private String eventData;

    /**
     * 期望的执行时间
     */
    private LocalDateTime expectExecAt;

    /**
     * 执行时间
     */
    private LocalDateTime execAt;

    /**
     * 完成时间
     */
    private LocalDateTime finishedAt;

    /**
     * 执行次数
     */
    private Integer execTimes;

    /**
     * 事件状态
     */
    private Byte eventStatus;

    /**
     * 创建人
     */
    private String creator;

}
