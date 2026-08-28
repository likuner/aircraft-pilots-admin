package com.uav.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uav.admin.entity.ExmRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExmRegistrationMapper extends BaseMapper<ExmRegistration> {

    @Select("SELECT COUNT(*) FROM exm_registration WHERE session_id = #{sessionId} AND status != 'CANCELLED' AND deleted = 0")
    long countBySession(@Param("sessionId") Long sessionId);
}
