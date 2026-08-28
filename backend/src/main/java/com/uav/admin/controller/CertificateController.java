package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.CerCertificate;
import com.uav.admin.entity.CerCertificateChangeRecord;
import com.uav.admin.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 合格证接口
 */
@Tag(name = "合格证")
@RestController
@RequestMapping("/api/cert/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "分页查询证书")
    @GetMapping
    @PreAuthorize("hasAuthority('cert:certificate:list')")
    public Result<PageResult<CerCertificate>> page(PageQuery query, String certNo, String status,
                                                   Long studentUserId, Long applyId) {
        return Result.ok(certificateService.page(query.getPageNum(), query.getPageSize(),
                certNo, status, studentUserId, applyId));
    }

    @Operation(summary = "证书详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('cert:certificate:list')")
    public Result<CerCertificate> detail(@PathVariable Long id) {
        return Result.ok(certificateService.detail(id));
    }

    @Operation(summary = "换发证书（生成新证号）")
    @PutMapping("/{id}/reissue")
    @PreAuthorize("hasAuthority('cert:certificate:reissue')")
    @Log(module = "cert", operation = "换发合格证")
    public Result<CerCertificate> reissue(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return Result.ok(certificateService.reissue(id, body == null ? null : body.get("reason")));
    }

    @Operation(summary = "吊销/作废证书")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('cert:certificate:revoke')")
    @Log(module = "cert", operation = "吊销合格证")
    public Result<Void> revoke(@PathVariable Long id, @RequestBody Map<String, String> body) {
        certificateService.revoke(id, body.get("reason"), body.get("changeType"));
        return Result.ok();
    }

    @Operation(summary = "证书变更记录")
    @GetMapping("/{id}/changes")
    @PreAuthorize("hasAuthority('cert:certificate:list')")
    public Result<List<CerCertificateChangeRecord>> changes(@PathVariable Long id) {
        return Result.ok(certificateService.changes(id));
    }
}
