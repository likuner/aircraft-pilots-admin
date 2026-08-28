package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.SysRole;
import com.uav.admin.mapper.SysRoleMapper;
import com.uav.admin.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色管理服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * 分页查询角色
     */
    public PageResult<SysRole> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleCode, keyword));
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByAsc(SysRole::getId);
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 全部角色（分配用下拉）
     */
    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SysRole role) {
        if (!StringUtils.hasText(role.getRoleCode()) || !StringUtils.hasText(role.getRoleName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "角色编码和名称不能为空");
        }
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BaseException(ErrorCode.DATA_EXISTS, "角色编码已存在");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (!StringUtils.hasText(role.getDataScope())) {
            role.setDataScope("ALL");
        }
        roleMapper.insert(role);
        if (role.getMenuIds() != null && !role.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(role.getId(), role.getMenuIds());
        }
    }

    /**
     * 更新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role) {
        SysRole db = roleMapper.selectById(role.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        roleMapper.updateById(role);
        if (role.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(role.getId());
            if (!role.getMenuIds().isEmpty()) {
                roleMenuMapper.batchInsert(role.getId(), role.getMenuIds());
            }
        }
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    /**
     * 分配菜单权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleId, menuIds);
        }
    }

    /**
     * 查询角色菜单 ID 集合
     */
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }
}
