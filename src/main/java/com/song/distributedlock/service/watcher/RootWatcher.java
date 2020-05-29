package com.song.distributedlock.service.watcher;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

/**
 * Created by Song on 2020/05/24.
 */
@Slf4j
public class RootWatcher implements Watcher {

    private ZooKeeper zooKeeper;

    @Override
    public void process(WatchedEvent event) {
        Event.EventType type = event.getType();
        log.info("[root.process]event->{}", JSON.toJSONString(event));
    }
}
