package com.uav.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前用户信息 VO（含角色、权限、菜单树）
 */
@Data
public class UserInfoVO {

    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String phone;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuTreeVO> menus;
}
