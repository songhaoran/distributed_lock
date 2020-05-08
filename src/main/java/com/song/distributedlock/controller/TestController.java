package com.song.distributedlock.controller;

import com.song.distributedlock.service.BusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
                    boolean result = businessService.testLock(lockMediaType);
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
                    boolean result = businessService.testLock(lockMediaType);
                    if (result) {
                        log.info("****************thread-2 测试成功");
                        break;
                    }
                }
            }
        }, "thread-2").start();
    }
}
