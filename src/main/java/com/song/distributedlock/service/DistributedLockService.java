package com.song.distributedlock.service;

/**
 * Created by Song on 2020/04/29.
 */
public interface DistributedLockService {
    /**
     * 获取锁
     *
     * @param lockName
     * @param ownerId
     * @return
     */
    boolean getLock(String lockName, String ownerId);

    /**
     * 删除锁
     *
     * @param lockName
     * @param ownerId
     * @return
     */
    boolean delLock(String lockName, String ownerId);
}
