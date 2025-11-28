# async-event 组件使用说明

`async-event` 是一个基于 Spring Boot + MyBatis-Plus 的“数据库驱动 + 内存分发”异步事件组件。它将需要延迟或异步执行的任务持久化到数据库，配合内存队列与线程池进行分发和执行，提供重试机制、执行轨迹记录与可配置化的扫描/线程池参数，适合在业务系统中实现可靠的异步任务与延迟任务处理。

## 核心角色

- Loader：周期扫描数据库，将满足条件的事件加载进内存队列，交给Worker进行事件的分发与执行。
- Worker：核心工作者，将Loader所加载的异步事件存储在内存队列中（delay queue），然后基于线程池异步执行delay queue中的任务，并管理队列与执行状态。
- Executor：异步事件执行器，负责异步事件的执行过程生命周期，包括状态流转、重试与记录执行日志等。
- Handler：真正的异步事件业务处理器，你的业务代码实现，按 `eventType` 路由。
- Template：业务入口模板，提供注册/取消事件的便捷方法。

## 快速开始

### 1. 安装依赖

在你的业务工程中引入 starter（以 Maven 为例）：

```xml
<dependency>
  <groupId>com.rayvinchen</groupId>
  <artifactId>async-event-spring-boot-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
  </dependency>
```

前置依赖说明：

- 数据源与 MyBatis-Plus：需要配置好你的 DataSource 与 MyBatis-Plus（starter 已做自动装配）。

### 2. 初始化数据库表

执行组件自带的 SQL 建表脚本：

- 事件表：`async-event-spring-boot-starter/src/main/resources/sql/async_event.sql`
- 事件执行记录表：`async-event-spring-boot-starter/src/main/resources/sql/async_event_record.sql`

MySQL 示例：

```sql
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

create table `async_event_record` (
    `id` bigint primary key auto_increment comment '主键ID',
    `event_id` bigint unsigned not null comment '事件ID',
    `event_status` tinyint not null default 0 comment '事件状态',
    `exec_at` datetime comment '执行时间',
    `fail_reason` varchar(128) comment '失败原因',
    `operator` varchar(32) comment '操作人',
    `create_at` datetime not null default current_timestamp comment '创建时间',
    `update_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    key `idx_event_id` (`event_id`)
) engine=innodb default charset=utf8mb4 comment='异步事件记录表';
```

### 3. 应用配置（application.yml）

```yaml
async-event:
  # Loader 扫描与预加载配置
  loader:
    scanIntervalSeconds: 10   # 扫描间隔(秒)
    batchSize: 200            # 每次批量加载大小
    lookAheadSeconds: 30      # 预加载窗口(秒)

  # worker/线程池配置（0 = 自动推导）
  worker:
    corePoolSize: 0           # 0=自动=CPU*2
    maxPoolSize: 0            # 0=自动=CPU*4
    queueCapacity: 2000       # 工作队列大小
    keepAliveSeconds: 60
    threadNamePrefix: async-event-worker
    maxInMemoryTasks: 0       # 内存任务上限，<=0 时按 3 * worker.queueCapacity 动态估算
    shutdownTimeoutSeconds: 60
    heartbeatIntervalSeconds: 10  # 执行中心跳写入间隔
```

说明：

- `AsyncEventAutoConfiguration` 会根据以上属性创建 `AsyncEventDispatcher`、`AsyncEventLoader`、`AsyncEventExecutor`、`AsyncEventTemplate` 等核心 Bean。
- MyBatis-Plus 与 Repository（`DefaultAsyncEventRepository` 等）通过 starter 的自动配置类装配。

## 编写你的事件处理器（Handler）

实现接口 `com.rayvinchen.async.event.core.executor.handler.AsyncEventHandler`，并注册为 Spring Bean：

```java
import com.rayvinchen.async.event.core.entity.AsyncEvent;
import com.rayvinchen.async.event.core.executor.handler.AsyncEventHandler;
import com.rayvinchen.async.event.core.valobj.ExecResult;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedHandler implements AsyncEventHandler {

    @Override
    public ExecResult handle(AsyncEvent event) {
        // 解析事件数据（JSON/字符串自定义）
        String payload = event.getEventData();
        // 执行业务逻辑...
        boolean ok = true; // 你的处理结果
        return ok ? ExecResult.success(true) : ExecResult.failure("biz error");
    }

    @Override
    public boolean retryable() {
        // 返回是否允许重试（失败后由组件触发指数退避重试）
        return true;
    }

    @Override
    public String eventType() {
        // 与注册事件时的 eventType 一致
        return "USER_CREATED";
    }
}
```

组件会通过 `AsyncEventHandlerDelegate` 自动路由到匹配 `eventType()` 的 Handler。

## 注册与取消事件（Template）

在业务代码中注入 `AsyncEventTemplate` 并调用：

```java
import com.rayvinchen.async.event.core.AsyncEventTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final AsyncEventTemplate asyncEventTemplate;

    public UserService(AsyncEventTemplate asyncEventTemplate) {
        this.asyncEventTemplate = asyncEventTemplate;
    }

    // 示例：在本地事务内注册事件
    public void createUserAndSendEvent(Long userId) {
        // 1. 本地业务（写库）...
        // 2. 同一事务内注册异步事件
        asyncEventTemplate.registerAsyncEvent(
                "USER_CREATED",             // 事件类型
                "{\"userId\": " + userId + "}", // 事件数据
                LocalDateTime.now(),         // 期望执行时间（现在或未来）
                "system"                    // 创建人/操作者
        );
    }

    // 取消事件（仅限待执行状态）
    public void cancelEvent(Long eventId) {
        asyncEventTemplate.cancelAsyncEvent(eventId, "admin");
    }
}
```

重要：`registerAsyncEvent` 建议与本地事务在同一事务中，保证“业务数据与事件”要么都成功，要么都失败，避免数据/事件不一致。

此外，Template 内部有“近实时”优化：当期望执行时间在当前时间之前或 1 分钟之内时，会直接将事件投递到内存分发器。

## 运行机制与状态机

状态枚举 `AsyncEventStatusEnum`：

- `WAIT_EXEC(1)` 待执行
- `WAIT_RETRY(2)` 待重试
- `EXECUTING(3)` 执行中
- `EXECUTE_SUCCESS(4)` 执行成功
- `EXECUTE_FAILURE(5)` 执行失败（达到最大重试或不可重试）
- `CANCEL(-1)` 已取消

流程简介：

1. 注册事件，初始为 `WAIT_EXEC`，记录一条 `AsyncEventRecord`。
2. Loader 周期扫描，或 Template 直接投递，Worker 将事件放入延时队列中，等待到达执行时间后，放入线程池执行队列。
3. Executor 拉取事件最新状态并原子流转：`WAIT_EXEC/WAIT_RETRY -> EXECUTING`，更新执行时间，并在执行期间按间隔写入心跳时间（`heartbeat_at`）。
4. 调用 Handler 处理：
   - 成功：更新为 `EXECUTE_SUCCESS`，记录成功轨迹。
   - 失败：若 Handler 可重试且未达最大次数（默认最多 5 次），进入 `WAIT_RETRY`，按指数退避 `2^(executeTimes)` 分钟后重试；否则置为 `EXECUTE_FAILURE`，记录失败轨迹。
5. 恢复与自愈：Loader 会在周期扫描中发现“心跳超时”的执行中任务并将其恢复为待执行，避免因进程异常或节点宕机造成任务长期卡死。

## 配置项与调优

- `async-event.loader.scanIntervalSeconds`：数据库扫描周期（秒）。任务量大/实时性高可调小；考虑数据库压力适度调大。
- `async-event.loader.batchSize`：每次批量加载数量，结合线程池吞吐调优。
- `async-event.loader.lookAheadSeconds`：预加载窗口，适当加大可降低触发延迟。
- `async-event.loader.maxInMemoryTasks`：Loader 在内存中的任务上限；<=0 时按 3 倍 `worker.queueCapacity` 计算，避免内存堆积。
- 线程池（`async-event.worker.*`）：
  - `corePoolSize/maxPoolSize` 为 0 时自动按 CPU 计算（核心=CPU*2，最大=CPU*4）。
  - `queueCapacity`：工作队列大小，过小易拒绝，过大可能拉长等待。
  - `keepAliveSeconds/threadNamePrefix/shutdownTimeoutSeconds`：常规参数。
  - `heartbeatIntervalSeconds`：执行中心跳写入间隔；Loader 会据此与内部策略识别“失联”任务进行恢复。

## 并发与一致性

组件通过数据库原子状态流转与执行中心跳，保证在多实例环境下同一事件仅有一个有效执行路径；在极端情况下可依赖 Handler 的幂等性确保业务一致性。

## MyBatis-Plus 与数据访问

- 默认 Repository 实现（如 `DefaultAsyncEventRepository`）基于 MyBatis-Plus Mapper（XML 在 `async-event-spring-boot-starter/src/main/resources/mapper`）。
- 若需自定义存储或分库分表，可替换 `AsyncEventRepository/AsyncEventRecordRepository` 的 Bean 实现，并保留接口语义。

## 最佳实践

- 事务一致性：务必将 `registerAsyncEvent` 与你的业务库写操作置于同一事务内。
- 幂等处理：Handler 应保证幂等性（根据业务主键或事件内容去重），避免因重试造成重复影响。
- 事件数据大小：尽量只传必要信息（业务主键等），大数据建议存储外部并在 Handler 中查。
- 可观测性：结合 `async_event_record` 表与日志进行排错；必要时为 Handler 增加必要埋点。
- 重试策略：默认最多 5 次，指数退避（2^n 分钟）。若你的业务不适合重试，将 `retryable()` 返回 false。
- 取消事件：仅在 `WAIT_EXEC` 状态可取消，API 已内置校验（否则抛出异常）。

## 常见问题（FAQ）

- Q：为什么事件没有立即执行？
  - A：检查 `expect_exec_at` 是否在未来；Loader 扫描周期是否过大；或 Template 判断“1 分钟内”投递是否未命中。
- Q：重复执行了怎么办？
  - A：组件通过“状态流转 + 心跳 + 恢复”降低重复执行概率；同时务必保证 Handler 幂等以应对极端情况（如节点抖动、瞬时网络异常）。
- Q：如何自定义最大重试次数或间隔？
  - A：当前默认在 `DefaultAsyncEventExecutor` 内部固定（`MAX_RETRY_TIMES=5`，指数退避），如需外部化可在后续版本中扩展或在你分支中按需调整实现。
- Q：如何对接告警？
  - A：建议基于 `async_event_record` 表定期扫描失败记录，并对 `EXECUTE_FAILURE` 进行告警；也可在 Handler 中接入告警组件。

## 关键类参考

- `AsyncEventAutoConfiguration`：自动装配入口，创建核心 Bean。
- `AsyncEventTemplate`：注册/取消事件的业务入口。
- `AsyncEventDispatcher`：线程池与队列管理、生命周期控制与监控日志。
- `DefaultAsyncEventExecutor`：状态机、心跳、重试与记录。
- `DefaultAsyncEventLoader`：周期加载、预加载与“失联任务”恢复。
- `AsyncEventHandler`/`AsyncEventHandlerDelegate`：事件处理与路由。
- `AsyncEventProperties`/`WorkerProperties`：配置项。

## 版本与环境

- JDK：与工程一致（建议 17+）。
- 数据库：以 MySQL 为例（InnoDB/utf8mb4），其他数据库需自行调整 SQL。

## 示例清单

- SQL：`async-event-spring-boot-starter/src/main/resources/sql/*.sql`
- Mapper XML：`async-event-spring-boot-starter/src/main/resources/mapper/*.xml`
- 配置属性元数据：`async-event-spring-boot-starter/target/classes/META-INF/spring-configuration-metadata.json`
