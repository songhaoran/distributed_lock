package com.song.distributedlock.service.watcher;

import com.alibaba.fastjson.JSON;
import com.song.distributedlock.service.ZookeeperDistributedLockServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Song on 2020/05/24.
 */
@Slf4j
public class ChildWatcher implements Watcher {

    private ZooKeeper zooKeeper;
    private String preNodePath;
    private String rootPath;
    private String currentNodeName;
    private CountDownLatch countDownLatch;

    public ChildWatcher(ZooKeeper zooKeeper, String preNodePath, String rootPath, String currentNodeName, CountDownLatch countDownLatch) {
        this.zooKeeper = zooKeeper;
        this.preNodePath = preNodePath;
        this.currentNodeName = currentNodeName;
        this.rootPath = rootPath;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void process(WatchedEvent event) {
        Event.EventType type = event.getType();
        log.info("[process]event->{}", JSON.toJSONString(event));
        if (type.equals(Event.EventType.NodeDeleted) == false) {
            this.startNewWatch();
            return;
        }

        try {
            List<String> childrenList = zooKeeper.getChildren(rootPath, true);
            log.info("[process]child_list->{}", JSON.toJSONString(childrenList));
            Collections.sort(childrenList);

            // 当前节点不是第一个节点
            if (childrenList.get(0).equals(currentNodeName) == false) {
                preNodePath = ZookeeperDistributedLockServiceImpl.getPreNodePath(currentNodeName, childrenList);
                this.startNewWatch();
                return;
            }

            countDownLatch.countDown();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void startNewWatch() {
        try {
            log.info("[process]设置新监听");
            zooKeeper.exists(preNodePath, new ChildWatcher(zooKeeper, preNodePath, rootPath, currentNodeName, countDownLatch));
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
