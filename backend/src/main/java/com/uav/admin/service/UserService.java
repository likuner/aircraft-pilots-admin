package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.SysUser;
import com.uav.admin.mapper.SysUserMapper;
import com.uav.admin.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户
     */
    public PageResult<SysUser> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 排除敏感字段（密码）
        wrapper.select(SysUser.class, i -> !"password".equals(i.getColumn()));
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 用户详情
     */
    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        // 默认密码 123456
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456"));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        // 分配角色
        if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            userRoleMapper.batchInsert(user.getId(), user.getRoleIds());
        }
    }

    /**
     * 更新用户（不含密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        SysUser db = userMapper.selectById(user.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        user.setCreateTime(null);
        userMapper.updateById(user);
        // 重设角色
        if (user.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(user.getId());
            if (!user.getRoleIds().isEmpty()) {
                userRoleMapper.batchInsert(user.getId(), user.getRoleIds());
            }
        }
    }

    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String password) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(StringUtils.hasText(password) ? password : "123456"));
        userMapper.updateById(update);
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.batchInsert(userId, roleIds);
        }
    }

    /**
     * 查询用户角色 ID 集合
     */
    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }
}
