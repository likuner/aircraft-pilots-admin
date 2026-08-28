package com.uav.admin.controller;

import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.SysOperationLog;
import com.uav.admin.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志接口
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService logService;

    @Operation(summary = "分页查询日志")
    @GetMapping
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<SysOperationLog>> page(PageQuery query, String keyword, String module, Integer status,
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin,
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return Result.ok(logService.page(query.getPageNum(), query.getPageSize(), keyword, module, status, begin, end));
    }
}
