# 分布式锁

#### 一. mysql实现
1. 表结构
```
CREATE TABLE `distributed_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `lock_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '锁名称',
  `owner_id` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '锁拥有者id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '锁创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_lock_name` (`lock_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='分布式锁';
```


#### 二. redis实现 (本项目只使用简单单机模式)