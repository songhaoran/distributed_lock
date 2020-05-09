package com.song.distributedlock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by Song on 2020/04/29.
 */
@Component
@Slf4j
public class BusinessService {
    private static final String lockName = "lock_name";

    public static int lock_media_sql = 1;
    public static int lock_media_redis = 2;
    public static int lock_media_zookeeper = 3;

    @Resource
    private SqlDistributedLockServiceImpl sqlDistributedLockService;
    @Resource
    private RedisDistributedLockServiceImpl redisDistributedLockService;
    @Resource
    private JedisPool jedisPool;

    public boolean doBusiness(Integer lockMediaType) {
        DistributedLockService distributedLockService = this.getLockMedia(lockMediaType);
        if (distributedLockService == null) {
            log.error("[doBusiness]获取锁的操作类失败!");
            return false;
        }

        // 获取锁
        String ownerId = this.getOwnerId();
        boolean getLockResult = distributedLockService.getLock(lockName, ownerId);
        if (getLockResult == false) {
            log.error("[doBusiness]获取锁失败!");
            return false;
        }

        // redis锁,开启守护线程
        RedisLockDaemonRunnable daemonRunnable = null;
        Thread daemonThread = null;
        if (lockMediaType == lock_media_redis) {
            daemonRunnable = this.getDaemonRunnable(jedisPool, ownerId, lockName, 2000);
            daemonThread = this.openDaemonThread(daemonRunnable);
        }

        try {
            // 执行业务
            log.info("[doBusiness]do something");
            return true;
        } catch (Exception e) {
            log.error("[doBusiness]do something error");
            return false;
        } finally {
            // 释放锁
            boolean delLockResult = distributedLockService.delLock(lockName, ownerId);
            if (delLockResult == false) {
                log.error("[doBusiness]释放锁失败!");
            } else {
                log.info("[doBusiness]释放锁成功!");
            }

            // 关闭守护线程
            if (lockMediaType == lock_media_redis) {
                this.closeLockDaemonThread(daemonRunnable, daemonThread);
            }
        }
    }

    private DistributedLockService getLockMedia(Integer lockMediaType) {
        if (lockMediaType == null) {
            return null;
        }
        if (lockMediaType == lock_media_sql) {
            return sqlDistributedLockService;
        } else if (lockMediaType == lock_media_redis) {
            return redisDistributedLockService;
        }

        return null;
    }

    private String getOwnerId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public RedisLockDaemonRunnable getDaemonRunnable(JedisPool jedisPool, String ownerId, String lockName, long lockExpireMills) {
        RedisLockDaemonRunnable runnable = new RedisLockDaemonRunnable(jedisPool, ownerId, lockName, lockExpireMills);
        return runnable;
    }

    public Thread openDaemonThread(RedisLockDaemonRunnable runnable) {
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
        return daemonThread;
    }

    public void closeLockDaemonThread(RedisLockDaemonRunnable runnable, Thread daemon) {
        runnable.stop();
        daemon.interrupt();
    }
}
