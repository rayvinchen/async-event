package com.rayvinchen.async.event.boot.starter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayvinchen.async.event.boot.starter.model.DBAsyncEventRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 异步事件记录表 Mapper 接口
 * </p>
 *
 * @author rayvinchen
 * @since 2025-11-22
 */
@Mapper
public interface AsyncEventRecordMapper extends BaseMapper<DBAsyncEventRecord> {

}
