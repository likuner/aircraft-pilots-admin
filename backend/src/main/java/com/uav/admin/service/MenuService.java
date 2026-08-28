package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.entity.SysMenu;
import com.uav.admin.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理服务（树形结构）
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    /**
     * 菜单树（含按钮）
     */
    public List<SysMenu> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getOrderNum));
        return buildTree(all, 0L);
    }

    /**
     * 构建树
     */
    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> children = new ArrayList<>();
        for (SysMenu menu : all) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(all, menu.getId()));
                children.add(menu);
            }
        }
        return children;
    }

    /**
     * 菜单详情
     */
    public SysMenu getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "菜单不存在");
        }
        return menu;
    }

    /**
     * 新增菜单
     */
    public void create(SysMenu menu) {
        if (!StringUtils.hasText(menu.getMenuName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "菜单名称不能为空");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType(2);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getOrderNum() == null) {
            menu.setOrderNum(0);
        }
        menuMapper.insert(menu);
    }

    /**
     * 更新菜单
     */
    public void update(SysMenu menu) {
        SysMenu db = menuMapper.selectById(menu.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "菜单不存在");
        }
        // 防止把父级设为自己的子级
        if (menu.getParentId() != null && menu.getParentId().equals(menu.getId())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "父级不能是自己");
        }
        menuMapper.updateById(menu);
    }

    /**
     * 删除菜单（存在子级时禁止删除）
     */
    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw new BaseException(ErrorCode.STATE_ERROR, "存在子菜单，请先删除子菜单");
        }
        menuMapper.deleteById(id);
    }

    /**
     * 当前用户菜单树（已含权限，登录后调用）
     */
    public List<SysMenu> menusOfUser(Long userId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        return buildTree(menus, 0L);
    }

    /**
     * 角色已分配菜单 ID 集合
     */
    public List<Long> menuIdsOfRole(Long roleId) {
        return menuMapper.selectMenusByRoleId(roleId).stream()
                .map(SysMenu::getId)
                .collect(Collectors.toList());
    }
}
