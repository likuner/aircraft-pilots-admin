package com.uav.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uav.admin.entity.ExmExamSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExmExamSessionMapper extends BaseMapper<ExmExamSession> {

    @Update("UPDATE exm_exam_session SET enrolled_count = enrolled_count + #{delta} WHERE id = #{id}")
    int incrEnrolledCount(@Param("id") Long id, @Param("delta") int delta);
}
