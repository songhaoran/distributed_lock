package com.song.distributedlock.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;

/**
 * Created by Song on 2020/05/09.
 */
@Slf4j
public class RedisLockDaemonRunnable implements Runnable {

    private JedisPool jedisPool;

    /**
     * 锁持有人id
     */
    private String ownerId;

    /**
     * 锁的名称
     */
    private String lockName;

    /**
     * 锁的超时时间(毫秒)
     */
    private long lockExpireMills;

    /**
     * 主线程是否执行完毕
     */
    private boolean isMainOver = false;

    String set_expire_time_lua = "if redis.call('get',KEYS[1])==KEYS[2] then return redis.call('pexpire',KEYS[1],KEYS[3]) else return 0 end";

    public void stop() {
        this.isMainOver = true;
    }

    public RedisLockDaemonRunnable(JedisPool jedisPool, String ownerId, String lockName, long lockExpireMills) {
        this.jedisPool = jedisPool;
        this.ownerId = ownerId;
        this.lockName = lockName;
        this.lockExpireMills = lockExpireMills;
    }

    @Override
    public void run() {
        while (isMainOver == false) {
            // 当锁的过期时间超过2/3时,重新设置超时时间
            try {
                Thread.sleep(lockExpireMills * 2 / 3);
                if (isMainOver == false) {
                    Jedis resource = jedisPool.getResource();
                    try {
                        Object result = resource.eval(set_expire_time_lua, Lists.newArrayList(lockName, ownerId, lockExpireMills + ""), new ArrayList<>());
                        if ((long) result == 1) {
                            log.info("[]demon reset expire time success!");
                        } else {
                            log.info("[]demon reset expire time fail!");
                        }
                    } catch (Exception e) {
                        log.error("[]demon reset expire time error!", e);
                    } finally {
                        resource.close();
                    }
                }
            } catch (InterruptedException e) {
                log.error("[]demon reset expire time error!", e);
            }
        }
    }
}
