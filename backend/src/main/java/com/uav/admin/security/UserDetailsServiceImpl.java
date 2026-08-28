package com.uav.admin.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.entity.SysMenu;
import com.uav.admin.entity.SysRole;
import com.uav.admin.entity.SysUser;
import com.uav.admin.mapper.SysMenuMapper;
import com.uav.admin.mapper.SysRoleMapper;
import com.uav.admin.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户详情加载：查询用户 + 角色 + 权限
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return buildLoginUser(user);
    }

    public LoginUser loadUserByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        // 角色
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
        // 权限
        Set<String> perms = new HashSet<>(menuMapper.selectPermsByUserId(user.getId()));
        // 数据范围（取第一个角色的）
        String dataScope = roles.isEmpty() ? "SELF" : roles.get(0).getDataScope();

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoles(roleCodes);
        loginUser.setPermissions(perms);
        loginUser.setDataScope(dataScope);
        loginUser.setStatus(user.getStatus());
        return loginUser;
    }
}
