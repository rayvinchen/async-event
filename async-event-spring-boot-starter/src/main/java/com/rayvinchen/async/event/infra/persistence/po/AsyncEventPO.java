package com.rayvinchen.async.event.infra.persistence.po;

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
 * @since 2025-11-04
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("async_event")
public class AsyncEventPO {

      /**
     * 主键ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

      /**
     * 事件ID
     */
      private Long eventId;

      /**
     * 事件状态
     */
      private Byte state;

      /**
     * 执行时间
     */
      private LocalDateTime executeTime;

      /**
     * 失败原因
     */
      private String failReason;

      /**
     * 创建时间
     */
      private LocalDateTime createTime;

      /**
     * 更新时间
     */
      private LocalDateTime updateTime;
}
