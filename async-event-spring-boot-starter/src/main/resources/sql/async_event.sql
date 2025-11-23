create table `async_event` (
    `id` bigint unsigned primary key auto_increment comment '主键ID',
    `event_type` varchar(32) not null comment '事件类型',
    `event_data` text not null comment '事件数据',
    `event_status` tinyint not null default 0 comment '事件状态',
    `expect_exec_at` datetime not null comment '期望执行时间',
    `exec_at` datetime comment '执行时间',
    `heartbeat_at` datetime comment '心跳时间',
    `finished_at` datetime comment '完成时间',
    `exec_times` int not null default 0 comment '执行次数',
    `creator` varchar(32) not null comment '创建者',
    `create_at` datetime not null default current_timestamp comment '创建时间',
    `update_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间'
) engine=innodb default charset=utf8mb4 comment='异步事件表';