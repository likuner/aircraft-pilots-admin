package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.CertApplyCreateDTO;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.CerCertificateApply;
import com.uav.admin.service.CertApplyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 合格证申请接口
 */
@Tag(name = "合格证申请")
@RestController
@RequestMapping("/api/cert/applications")
@RequiredArgsConstructor
public class CertApplyController {

    private final CertApplyService certApplyService;

    @Operation(summary = "分页查询申请")
    @GetMapping
    @PreAuthorize("hasAuthority('cert:apply:list')")
    public Result<PageResult<CerCertificateApply>> page(PageQuery query, Long studentUserId, String status) {
        return Result.ok(certApplyService.page(query.getPageNum(), query.getPageSize(), studentUserId, status));
    }

    @Operation(summary = "提交合格证申请")
    @PostMapping
    @PreAuthorize("hasAuthority('cert:apply:list')")
    @Log(module = "cert", operation = "提交合格证申请")
    public Result<Void> create(@RequestBody CertApplyCreateDTO dto) {
        certApplyService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "申请审核（通过后 MQ 异步签发）")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('cert:apply:audit')")
    @Log(module = "cert", operation = "合格证申请审核")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        certApplyService.audit(id, body.get("action"), body.get("comment"));
        return Result.ok();
    }
}
