package com.song.distributedlock.service;

import com.song.distributedlock.service.watcher.ChildWatcher;
import com.song.distributedlock.service.watcher.RootWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Song on 2020/05/24.
 */
@Component
@Slf4j
public class ZookeeperDistributedLockServiceImpl implements DistributedLockService {
    private static final String host_name = "127.0.0.1:2181";
    public static final String rootPath = "/lock";
    public static final String initNodeName = "lock";

    ConcurrentHashMap<String, String> ownerId2NodeNameMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, ZooKeeper> ownerId2ZooKeeperMap = new ConcurrentHashMap<>();

    @Override
    public boolean getLock(String lockName, String ownerId) {
        ZooKeeper zooKeeper;
        try {
            zooKeeper = this.getZooKeeper();
            ownerId2ZooKeeperMap.put(ownerId, zooKeeper);
        } catch (IOException e) {
            log.error("[getLock]", e);
            return false;
        }

        try {
            // 创建根节点
            Stat rootPathStat = zooKeeper.exists(rootPath, true);
            if (rootPathStat == null) {
                zooKeeper.create(rootPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            // 创建当前节点
            String currentNodePath = zooKeeper.create(getInitNodePath(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            String currentNodeName = currentNodePath.substring(currentNodePath.indexOf(rootPath) + 6);
            ownerId2NodeNameMap.put(ownerId, currentNodeName);

            // 获取所有锁节点
            List<String> childrenList = zooKeeper.getChildren(rootPath, true);
            Collections.sort(childrenList);

            // 当前节点是否第一个节点
            if (childrenList.get(0).equals(currentNodeName)) {
                return true;
            }

            // 监听前一个节点
            String preNodePath = getPreNodePath(currentNodeName, childrenList);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ChildWatcher watcher = new ChildWatcher(zooKeeper, preNodePath, rootPath, currentNodeName, countDownLatch);
            zooKeeper.exists(preNodePath, watcher);

            // 已经成为第一个节点
            try {
                countDownLatch.await(5, TimeUnit.SECONDS);
                return true;
            } catch (InterruptedException e) {
                log.error("[]等待变为前一个节点失败");
                throw e;
            }
        } catch (KeeperException e) {
            e.printStackTrace();
            ownerId2NodeNameMap.remove(ownerId);
            ownerId2ZooKeeperMap.remove(ownerId);
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            ownerId2NodeNameMap.remove(ownerId);
            ownerId2ZooKeeperMap.remove(ownerId);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            ownerId2NodeNameMap.remove(ownerId);
            ownerId2ZooKeeperMap.remove(ownerId);
            return false;
        } finally {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getPreNodePath(String currentNodeName, List<String> childrenList) {
        String preNodeName = childrenList.get(childrenList.indexOf(currentNodeName) - 1);
        return rootPath + "/" + preNodeName;
    }

    private ZooKeeper getZooKeeper() throws IOException {
        return new ZooKeeper(host_name, 6000, new RootWatcher());
    }

    public static String getInitNodePath() {
        return rootPath + "/" + initNodeName;
    }

    @Override
    public boolean delLock(String lockName, String ownerId) {
        try {
            ZooKeeper zooKeeper = ownerId2ZooKeeperMap.get(ownerId);
            zooKeeper.close();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            ownerId2NodeNameMap.remove(ownerId);
        }
    }
}
