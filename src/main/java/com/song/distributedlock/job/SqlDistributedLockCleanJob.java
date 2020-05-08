package com.song.distributedlock.job;

import com.song.distributedlock.dao.DistributedLockMapper;
import com.song.distributedlock.model.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Created by Song on 2020/04/29.
 * 清理mysql分布式锁的过期锁
 */
@Component
@Slf4j
public class SqlDistributedLockCleanJob {

    @Resource
    private DistributedLockMapper distributedLockMapper;

    long expire_mills = 10000;

    @Scheduled(cron = "*/2 * * * * ?")
    public void run() {
        List<DistributedLock> list = distributedLockMapper.selectAll();
        list.forEach(lock -> {
            try {
                Date createdAt = lock.getCreatedAt();
                if (new Date().getTime() - createdAt.getTime() > expire_mills) {
                    distributedLockMapper.delete(lock);
                }
            } catch (Exception e) {
                log.error("[run]delete error!");
            }
        });
    }
}
