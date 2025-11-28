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

    /**
     * 成功结果
     *
     * @return 成功结果
     */
    public static ExecResult success() {
        return ExecResult.builder()
                .success(true)
                .build();
    }

    /**
     * 创建一个表示执行失败的ExecResult对象
     *
     * @param failReason 失败原因描述信息
     * @return 包含失败状态和原因的ExecResult对象
     */
    public static ExecResult failure(String failReason) {
        return ExecResult.builder()
                .success(false)
                .failReason(failReason)
                .build();
    }

}
