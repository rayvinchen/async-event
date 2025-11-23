package com.rayvinchen.async.event.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AsyncEventStatusEnum
 *
 * @author rayvinchen
 * @since 2025/11/8 17:35
 */
@Getter
@AllArgsConstructor
public enum AsyncEventStatusEnum {

    CANCEL((byte) -1, "取消"),
    WAIT_EXEC((byte) 1, "待执行"),
    WAIT_RETRY((byte) 2, "待重试"),
    EXECUTING((byte) 3, "执行中"),
    EXECUTE_SUCCESS((byte) 4, "执行成功"),
    EXECUTE_FAILURE((byte) 5, "执行失败"),
    ;

    private final byte code;

    private final String desc;


}
