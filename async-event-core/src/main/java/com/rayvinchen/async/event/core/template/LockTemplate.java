package com.rayvinchen.async.event.core.template;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁模板类，基于Redisson实现
 *
 * @author rayvinchen
 * @since 2025/11/8 17:55
 */
@Slf4j
public class LockTemplate {

    private final RedissonClient redissonClient;

    public LockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 阻塞获取锁，直到获取成功
     *
     * @param key      锁的key
     * @param expire   锁的过期时间
     * @param timeUnit 时间单位
     * @return 锁记录对象
     */
    public LockRecord lock(String key, long expire, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(key);
        try {
            lock.lock(expire, timeUnit);
            log.debug("Successfully acquired lock: {}", key);
            return new LockRecord(lock, key);
        } catch (Exception e) {
            log.error("Failed to acquire lock: {}", key, e);
            return null;
        }
    }

    /**
     * 尝试获取锁，立即返回结果
     *
     * @param key      锁的key
     * @param expire   锁的过期时间
     * @param timeUnit 时间单位
     * @return 锁记录对象
     */
    public LockRecord tryLock(String key, long expire, TimeUnit timeUnit) {
        return tryLock(key, 0, expire, timeUnit);
    }

    /**
     * 尝试获取锁，带等待时间
     *
     * @param key       锁的key
     * @param waitTime  等待时间
     * @param leaseTime 锁的过期时间
     * @param timeUnit  时间单位
     * @return 锁记录对象
     */
    public LockRecord tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(key);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            log.debug("Try-acquired lock: {}, acquired: {}", key, acquired);
            return acquired ? new LockRecord(lock, key) : null;
        } catch (Exception e) {
            log.error("Exception occurred while trying to acquire lock with wait time: {}", key, e);
            return null;
        }
    }

    /**
     * 释放锁
     *
     * @param lockRecord 锁记录对象
     */
    public void unlock(LockRecord lockRecord) {
        if (lockRecord == null || lockRecord.lock() == null) {
            log.warn("Attempting to unlock null or invalid lock record");
            return;
        }

        try {
            RLock lock = lockRecord.lock();
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Successfully released lock: {}", lockRecord.key());
            } else {
                log.warn("Lock not held by current thread: {}", lockRecord.key());
            }
        } catch (Exception e) {
            log.error("Failed to release lock: {}", lockRecord.key(), e);
        }
    }

    /**
     * 锁记录对象，包含锁实例和相关信息
     */
    public record LockRecord(RLock lock, String key) {}

}
