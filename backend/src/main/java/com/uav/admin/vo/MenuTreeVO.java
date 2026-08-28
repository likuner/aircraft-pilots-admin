package com.uav.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树 VO
 */
@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer orderNum;
    private List<MenuTreeVO> children = new ArrayList<>();
}
