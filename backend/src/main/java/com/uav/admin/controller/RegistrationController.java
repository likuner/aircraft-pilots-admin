package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.dto.RegistrationCreateDTO;
import com.uav.admin.entity.ExmRegistration;
import com.uav.admin.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 报名管理接口
 */
@Tag(name = "报名管理")
@RestController
@RequestMapping("/api/exam/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "分页查询报名")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:registration:list')")
    public Result<PageResult<ExmRegistration>> page(PageQuery query, Long sessionId, String status, String keyword) {
        return Result.ok(registrationService.page(query.getPageNum(), query.getPageSize(), sessionId, status, keyword));
    }

    @Operation(summary = "考生报名（原子扣减名额）")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:registration:add')")
    @Log(module = "exam", operation = "考生报名")
    public Result<Void> create(@RequestBody RegistrationCreateDTO dto) {
        registrationService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "报名审核通过")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('exam:registration:approve')")
    @Log(module = "exam", operation = "报名审核通过")
    public Result<Void> approve(@PathVariable Long id) {
        registrationService.approve(id);
        return Result.ok();
    }

    @Operation(summary = "报名驳回")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('exam:registration:reject')")
    @Log(module = "exam", operation = "报名驳回")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        registrationService.reject(id, body == null ? null : body.get("reason"));
        return Result.ok();
    }

    @Operation(summary = "考生取消报名")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('exam:registration:add')")
    @Log(module = "exam", operation = "取消报名")
    public Result<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return Result.ok();
    }

    @Operation(summary = "编排批次")
    @PutMapping("/{id}/arrange")
    @PreAuthorize("hasAuthority('exam:registration:schedule')")
    @Log(module = "exam", operation = "报名编排批次")
    public Result<Void> arrange(@PathVariable Long id, @RequestParam Long batchId) {
        registrationService.arrange(id, batchId);
        return Result.ok();
    }
}
