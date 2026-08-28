package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.OrgQualification;
import com.uav.admin.service.QualificationService;
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

import java.util.Map;

/**
 * 机构资质证接口
 */
@Tag(name = "机构资质证")
@RestController
@RequestMapping("/api/institution/qualifications")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService qualificationService;

    @Operation(summary = "分页查询资质证")
    @GetMapping
    @PreAuthorize("hasAuthority('inst:qualification:list')")
    public Result<PageResult<OrgQualification>> page(PageQuery query, Long institutionId, String status, String keyword) {
        return Result.ok(qualificationService.page(query.getPageNum(), query.getPageSize(), institutionId, status, keyword));
    }

    @Operation(summary = "资质证详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:qualification:list')")
    public Result<OrgQualification> detail(@PathVariable Long id) {
        return Result.ok(qualificationService.detail(id));
    }

    @Operation(summary = "资质证续期")
    @PutMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('inst:qualification:list')")
    @Log(module = "institution", operation = "机构资质续期")
    public Result<Void> renew(@PathVariable Long id) {
        qualificationService.renew(id);
        return Result.ok();
    }

    @Operation(summary = "吊销/暂停资质证")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('inst:qualification:revoke')")
    @Log(module = "institution", operation = "吊销机构资质")
    public Result<Void> revoke(@PathVariable Long id, @RequestBody Map<String, String> body) {
        qualificationService.revoke(id, body.get("reason"), body.get("changeType"));
        return Result.ok();
    }
}
