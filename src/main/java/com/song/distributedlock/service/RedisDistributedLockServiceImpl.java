package com.song.distributedlock.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created by Song on 2020/04/29.
 */
@Component
@Slf4j
public class RedisDistributedLockServiceImpl implements DistributedLockService {

    private static final int expire_time = 2000;
    private static final String set_lock_lua = "if redis.call('setnx',KEYS[1],KEYS[2])==1 then return redis.call('pexpire',KEYS[1],KEYS[3]) else return 0 end";
    private static final String del_lock_lua = "if redis.call('get',KEYS[1])==KEYS[2] then return redis.call('del',KEYS[1]) else return 0 end";

    @Resource
    private JedisPool jedisPool;

    @Override
    public boolean getLock(String lockName, String ownerId) {
        Jedis resource = jedisPool.getResource();
        try {
            Object result = resource.eval(set_lock_lua, Lists.newArrayList(lockName, ownerId, expire_time + ""), new ArrayList<>());
            if ((long) result == 1) {
                //            log.info("[getLock]获取锁成功!lock_name->{},owner_id->{}", lockName, ownerId);
                return true;
            }
//        log.error("[getLock]获取锁失败!lock_name->{},owner_id->{}", lockName, ownerId);
            return false;
        } catch (Exception e) {
            log.error("[getLock]", e);
            return false;
        } finally {
            resource.close();
        }
    }

    @Override
    public boolean delLock(String lockName, String ownerId) {
        Jedis resource = jedisPool.getResource();
        try {
            Object result = resource.eval(del_lock_lua, Lists.newArrayList(lockName, ownerId), new ArrayList<>());
            if ((long) result == 1) {
                //            log.info("[delLock]获取锁成功!lock_name->{},owner_id->{}", lockName, ownerId);
                return true;
            }
//        log.error("[delLock]获取锁失败!lock_name->{},owner_id->{}", lockName, ownerId);
            return false;
        } catch (Exception e) {
            log.error("[delLock]", e);
            return false;
        } finally {
            resource.close();
        }
    }
}
