package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.OrgSiteInspection;
import com.uav.admin.service.CertificationApplyService;
import com.uav.admin.service.InspectionService;
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
 * 实地核查接口
 */
@Tag(name = "实地核查")
@RestController
@RequestMapping("/api/institution/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final CertificationApplyService certificationApplyService;

    @Operation(summary = "分页查询核查任务")
    @GetMapping
    @PreAuthorize("hasAuthority('inst:inspection:list')")
    public Result<PageResult<OrgSiteInspection>> page(PageQuery query, Long applyId, Long inspectorId, String status) {
        return Result.ok(inspectionService.page(query.getPageNum(), query.getPageSize(), applyId, inspectorId, status));
    }

    @Operation(summary = "核查任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:inspection:list')")
    public Result<OrgSiteInspection> detail(@PathVariable Long id) {
        return Result.ok(inspectionService.detail(id));
    }

    @Operation(summary = "完成实地核查（通过/不通过）")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('inst:inspection:complete')")
    @Log(module = "institution", operation = "完成实地核查")
    public Result<Void> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        certificationApplyService.completeInspection(id, body.get("result"), body.get("summary"));
        return Result.ok();
    }
}
