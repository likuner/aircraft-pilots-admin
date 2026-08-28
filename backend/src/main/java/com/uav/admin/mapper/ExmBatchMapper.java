package com.uav.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uav.admin.entity.ExmBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExmBatchMapper extends BaseMapper<ExmBatch> {

    @Update("UPDATE exm_batch SET enrolled_count = enrolled_count + #{delta} WHERE id = #{id}")
    int incrEnrolledCount(@Param("id") Long id, @Param("delta") int delta);
}
