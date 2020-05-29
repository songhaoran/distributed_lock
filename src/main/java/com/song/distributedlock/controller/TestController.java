package com.song.distributedlock.controller;

import com.alibaba.fastjson.JSON;
import com.song.distributedlock.service.BusinessService;
import com.song.distributedlock.service.ZookeeperDistributedLockServiceImpl;
import com.song.distributedlock.service.watcher.ChildWatcher;
import com.song.distributedlock.service.watcher.RootWatcher;
import com.sun.org.apache.regexp.internal.RE;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Song on 2020/04/29.
 */
@RestController
@Slf4j
public class TestController {
    @Resource
    private BusinessService businessService;

    @GetMapping("/doSomething")
    public void doSomething(@RequestParam("lock_media_type") Integer lockMediaType) {
        boolean result = businessService.doBusiness(lockMediaType);
        if (result) {
            log.info("[doSomething]执行成功");
        } else {
            log.error("[doSomething]执行失败");
        }
    }

    @GetMapping("/testLock")
    public void testLock(@RequestParam("lock_media_type") Integer lockMediaType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    boolean result = businessService.doBusiness(lockMediaType);
                    if (result) {
                        log.info("****************thread-1 测试成功");
                        break;
                    }
                }
            }
        }, "thread-1").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    boolean result = businessService.doBusiness(lockMediaType);
                    if (result) {
                        log.info("****************thread-2 测试成功");
                        break;
                    }
                }
            }
        }, "thread-2").start();
    }

    @GetMapping("/zk/add_lock")
    public void addLock() throws Exception {
        ZooKeeper zooKeeper = this.getZooKeeper();
        Stat rootPathExists = zooKeeper.exists(ZookeeperDistributedLockServiceImpl.rootPath, true);
        if (rootPathExists == null) {
            zooKeeper.create(ZookeeperDistributedLockServiceImpl.rootPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        String childPath = zooKeeper.create(ZookeeperDistributedLockServiceImpl.getInitNodePath(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        log.info("[addLock]child_path->{}", childPath);
        this.getChild(ZookeeperDistributedLockServiceImpl.rootPath, zooKeeper);
    }

    @GetMapping("/zk/delete_lock")
    public void delLock(@RequestParam("node_name") String nodeName) throws Exception {
        String nodePath = ZookeeperDistributedLockServiceImpl.rootPath + "/" + nodeName;
        ZooKeeper zooKeeper = this.getZooKeeper();
        zooKeeper.delete(nodePath, 0);
        this.getChild(ZookeeperDistributedLockServiceImpl.rootPath, zooKeeper);
    }

    private ZooKeeper getZooKeeper() throws IOException {
        return new ZooKeeper("127.0.0.1:2181", 6000, new RootWatcher());
    }

    @GetMapping("/zk/test")
    public void testZk() throws Exception {
        String rootPath = "/lock";
        String childNodeName0 = "ownerId0";
        String childNodeName1 = "ownerId1";

        ZooKeeper zooKeeper = this.getZooKeeper();
        // 创建根节点
        Stat rootPathExists = zooKeeper.exists(rootPath, true);
        if (rootPathExists == null) {
            String createRootNode = zooKeeper.create(rootPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            log.info("[testZk]create_root_node->{}", createRootNode);
        }

        // 创建两个子节点
        String childPath0 = zooKeeper.create(rootPath + "/" + childNodeName0, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        log.info("[testZk]child_path_0->{}", childPath0);
        String childPath1 = zooKeeper.create(rootPath + "/" + childNodeName1, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        log.info("[testZk]child_path_1->{}", childPath1);
        this.getChild(rootPath, zooKeeper);

        // 监听第一个节点
        CountDownLatch countDownLatch = new CountDownLatch(1);
        zooKeeper.exists(childPath0, new ChildWatcher(zooKeeper, childPath0, rootPath, childNodeName1, countDownLatch));

        // 删除第一个节点
        Stat node0ChangeStat = zooKeeper.setData(childPath0, new byte[0], 0);
        Thread.sleep(1000);
        zooKeeper.delete(childPath0, node0ChangeStat.getVersion());
        countDownLatch.await();
        log.info("[testZk]第一个节点删除后,第二个节点count down");
    }

    private void getChild(String rootPath, ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        List<String> childrenList = zooKeeper.getChildren(rootPath, true);
        log.info("[getChild]child->{}", JSON.toJSONString(childrenList));
    }
}
