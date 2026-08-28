package com.uav.admin.dto;

import lombok.Data;

/**
 * 分页查询基类
 */
@Data
public class PageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    public long getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
