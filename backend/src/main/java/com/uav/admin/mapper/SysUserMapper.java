package com.uav.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uav.admin.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Update("UPDATE sys_user SET last_login_time = NOW(), last_login_ip = #{ip} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("ip") String ip);
}
