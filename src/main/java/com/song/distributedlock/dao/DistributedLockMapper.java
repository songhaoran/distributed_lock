package com.song.distributedlock.dao;

import com.song.distributedlock.model.DistributedLock;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;


/**
 * Created by Song on 2020/04/29.
 */
@Repository
public interface DistributedLockMapper extends Mapper<DistributedLock> {

    @Select("select * from distributed_lock where lock_name=#{lock_name}")
    DistributedLock getByLockName(@Param("lock_name") String lockName);

    @Delete("delete from distributed_lock where lock_name=#{lock_name} and owner_id=#{owner_id}")
    int deleteByLockNameAndOwnerId(@Param("lock_name") String lockName, @Param("owner_id") String ownerId);


}
