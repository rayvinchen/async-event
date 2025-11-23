package com.rayvinchen.async.event.core.executor;


import com.rayvinchen.async.event.core.valobj.AsyncEventExecContext;
import com.rayvinchen.async.event.core.valobj.ExecResult;

/**
 * AsyncEventExecutor
 *
 * @author rayvinchen
 * @since 2025/11/5 19:49
 */
public interface AsyncEventExecutor {

    /**
     * 执行异步事件
     *
     * @param context 上下文
     * @return 执行结果
     */
    ExecResult execute(AsyncEventExecContext context);

}
