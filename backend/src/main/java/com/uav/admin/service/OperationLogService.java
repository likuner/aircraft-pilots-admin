package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.SysOperationLog;
import com.uav.admin.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 操作日志查询服务
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper logMapper;

    /**
     * 分页查询日志
     */
    public PageResult<SysOperationLog> page(long page, long size, String keyword, String module,
                                            Integer status, LocalDateTime begin, LocalDateTime end) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysOperationLog::getUsername, keyword)
                    .or().like(SysOperationLog::getOperation, keyword)
                    .or().like(SysOperationLog::getRequestUrl, keyword));
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperationLog::getModule, module);
        }
        if (status != null) {
            wrapper.eq(SysOperationLog::getStatus, status);
        }
        if (begin != null) {
            wrapper.ge(SysOperationLog::getCreateTime, begin);
        }
        if (end != null) {
            wrapper.le(SysOperationLog::getCreateTime, end);
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        Page<SysOperationLog> p = logMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }
}
