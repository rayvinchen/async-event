create table `async_event_record` (
    `id` bigint primary key auto_increment comment '主键ID',
    `event_id` bigint unsigned not null comment '事件ID',
    `execute_status` tinyint not null default 0 comment '事件状态',
    `execute_time` datetime comment '执行时间',
    `fail_reason` varchar(128) comment '失败原因',
    `operator` varchar(32) comment '操作人',
    `create_time` datetime not null default current_timestamp comment '创建时间',
    `update_time` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    key `idx_event_id` (`event_id`)
) engine=innodb default charset=utf8mb4 comment='异步事件记录表';