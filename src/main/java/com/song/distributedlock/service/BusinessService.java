package com.song.distributedlock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public boolean doBusiness(Integer lockMediaType) {
        DistributedLockService distributedLockService = this.getLockMedia(lockMediaType);
        if (distributedLockService == null) {
            log.error("[doBusiness]获取锁的操作类失败!");
            return false;
        }

        // 获取锁
        String ownerId = UUID.randomUUID().toString().replaceAll("-", "");
        boolean getLockResult = distributedLockService.getLock(lockName, ownerId);
        if (getLockResult == false) {
            log.error("[doBusiness]获取锁失败!");
            return false;
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




    public boolean testLock(Integer lockMediaType) {
        DistributedLockService distributedLockService = this.getLockMedia(lockMediaType);
        if (distributedLockService == null) {
            log.error("{}获取锁的操作类失败!", Thread.currentThread().getName());
            return false;
        }

        // 获取锁
        String ownerId = UUID.randomUUID().toString().replaceAll("-", "");
        boolean getLockResult = distributedLockService.getLock(lockName, ownerId);
        if (getLockResult == false) {
            log.error("{}获取锁失败!", Thread.currentThread().getName());
            return false;
        }
        log.info("{}获取锁成功!", Thread.currentThread().getName());

        try {
            // 执行业务
//            Thread.currentThread().sleep(1000);
        } catch (Exception e) {
            log.error("[doBusiness]do something error");
        } finally {
            // 释放锁
//            boolean delLockResult = distributedLockService.delLock(lockName, ownerId);
//            if (delLockResult == false) {
//                log.error("{}释放锁失败!", Thread.currentThread().getName());
//            } else {
//                log.info("{}释放锁成功!", Thread.currentThread().getName());
//            }
            return true;
        }
    }


}
