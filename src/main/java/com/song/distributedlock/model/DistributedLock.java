package com.song.distributedlock.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by Song on 2020/04/29.
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Table(name = "distributed_lock")
public class DistributedLock {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 锁拥有者id
     */
    private String ownerId;

    /**
     * 锁创建时间
     */
    private Date createdAt;

}
