package com.rayvinchen.async.event.boot.starter.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 异步事件表
 * </p>
 *
 * @author rayvinchen
 * @since 2025-11-23
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("async_event")
public class DBAsyncEvent {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件数据
     */
    private String eventData;

    /**
     * 事件状态
     */
    private Byte eventStatus;

    /**
     * 期望执行时间
     */
    private LocalDateTime expectExecAt;

    /**
     * 执行时间
     */
    private LocalDateTime execAt;

    /**
     * 心跳时间
     */
    private LocalDateTime heartbeatAt;

    /**
     * 完成时间
     */
    private LocalDateTime finishedAt;

    /**
     * 执行次数
     */
    private Integer execTimes;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private LocalDateTime createAt;

    /**
     * 更新时间
     */
    private LocalDateTime updateAt;
}
