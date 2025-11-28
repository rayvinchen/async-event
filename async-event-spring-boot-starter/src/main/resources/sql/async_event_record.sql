create table `async_event_record` (
    `id` bigint primary key auto_increment comment '主键ID',
    `event_id` bigint unsigned not null comment '事件ID',
    `event_status` tinyint not null default 0 comment '事件状态',
    `exec_at` datetime comment '执行时间',
    `fail_reason` varchar(128) comment '失败原因',
    `operator` varchar(32) comment '操作人',
    `create_at` datetime not null default current_timestamp comment '创建时间',
    `update_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    -- 事件记录查询：通常按事件ID查看执行历史，并按执行时间排序
    key `idx_event_id` (`event_id`, `exec_at`, `id`)
) engine=innodb default charset=utf8mb4 comment='异步事件记录表';