package com.song.distributedlock.service;

import com.song.distributedlock.dao.DistributedLockMapper;
import com.song.distributedlock.model.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by Song on 2020/04/29.
 */
@Component
@Slf4j
public class SqlDistributedLockServiceImpl implements DistributedLockService{

    @Resource
    private DistributedLockMapper distributedLockMapper;

    @Override
    public boolean getLock(String lockName, String ownerId) {
        if (StringUtils.isBlank(lockName)) {
            throw new RuntimeException("锁名称不能为空");
        }
        if (StringUtils.isBlank(ownerId)) {
            throw new RuntimeException("锁拥有者id不能为空");
        }

        DistributedLock distributedLock = distributedLockMapper.getByLockName(lockName);
        if (distributedLock != null) {
            if (distributedLock.getOwnerId().equals(ownerId) == false) {
//                log.error("[getLock]已被其他客户端加锁");
                return false;
            }
            return true;
        }

        distributedLock = new DistributedLock();
        distributedLock.setLockName(lockName);
        distributedLock.setOwnerId(ownerId);
        try {
            int i = distributedLockMapper.insertSelective(distributedLock);
            if (i < 1) {
//                log.error("[getLock]获取锁失败");
                return false;
            }
            return true;
        } catch (DuplicateKeyException e) {
            log.error("[getLock]获取锁异常,或为并发获取异常");
            return false;
        }
    }


    @Override
    public boolean delLock(String lockName, String ownerId) {
        if (StringUtils.isBlank(lockName)) {
            throw new RuntimeException("锁名称不能为空");
        }
        if (StringUtils.isBlank(ownerId)) {
            throw new RuntimeException("锁拥有者id不能为空");
        }

        int i = distributedLockMapper.deleteByLockNameAndOwnerId(lockName, ownerId);
        if (i < 1) {
//            log.error("[delLock]删除锁失败");
            return false;
        }
        return true;
    }
}
