package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.OrgApplyMaterial;
import com.uav.admin.entity.OrgCertificationApply;
import com.uav.admin.service.CertificationApplyService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 机构认证申请接口
 */
@Tag(name = "机构认证申请")
@RestController
@RequestMapping("/api/institution/applications")
@RequiredArgsConstructor
public class CertificationApplyController {

    private final CertificationApplyService certificationApplyService;

    @Operation(summary = "分页查询认证申请")
    @GetMapping
    @PreAuthorize("hasAuthority('inst:application:list')")
    public Result<PageResult<OrgCertificationApply>> page(PageQuery query, Long institutionId, String status) {
        return Result.ok(certificationApplyService.page(query.getPageNum(), query.getPageSize(), institutionId, status));
    }

    @Operation(summary = "申请详情（材料/审查/核查/评定）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:application:list')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(certificationApplyService.detail(id));
    }

    @Operation(summary = "提交认证申请")
    @PostMapping
    @PreAuthorize("hasAuthority('inst:application:submit')")
    @Log(module = "institution", operation = "提交认证申请")
    public Result<Void> create(@RequestBody OrgCertificationApply apply) {
        certificationApplyService.create(apply);
        return Result.ok();
    }

    @Operation(summary = "提交申请材料")
    @PutMapping("/{id}/materials")
    @PreAuthorize("hasAuthority('inst:application:submit')")
    @Log(module = "institution", operation = "提交认证材料")
    public Result<Void> submitMaterial(@PathVariable Long id, @RequestBody List<OrgApplyMaterial> materials) {
        certificationApplyService.submitMaterial(id, materials);
        return Result.ok();
    }

    @Operation(summary = "材料审查（通过/退回）")
    @PutMapping("/{id}/review-material")
    @PreAuthorize("hasAuthority('inst:material:review')")
    @Log(module = "institution", operation = "机构材料审查")
    public Result<Void> reviewMaterial(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        certificationApplyService.reviewMaterial(id,
                body.get("result") == null ? null : String.valueOf(body.get("result")),
                body.get("comment") == null ? null : String.valueOf(body.get("comment")),
                body.get("reviewStep") == null ? null : ((Number) body.get("reviewStep")).intValue());
        return Result.ok();
    }

    @Operation(summary = "派发核查任务")
    @PutMapping("/{id}/assign-inspection")
    @PreAuthorize("hasAuthority('inst:inspection:assign')")
    @Log(module = "institution", operation = "派发核查任务")
    public Result<Void> assignInspection(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long inspectorId = body.get("inspectorId") == null ? null : ((Number) body.get("inspectorId")).longValue();
        LocalDate date = body.get("inspectionDate") == null ? null : LocalDate.parse(String.valueOf(body.get("inspectionDate")));
        certificationApplyService.assignInspection(id, inspectorId, date,
                body.get("address") == null ? null : String.valueOf(body.get("address")),
                body.get("checklist") == null ? null : String.valueOf(body.get("checklist")));
        return Result.ok();
    }

    @Operation(summary = "资质评定（通过后发证）")
    @PutMapping("/{id}/qualify")
    @PreAuthorize("hasAuthority('inst:qualification:list')")
    @Log(module = "institution", operation = "机构资质评定")
    public Result<Void> qualify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal score = body.get("evaluationScore") == null ? null : new BigDecimal(String.valueOf(body.get("evaluationScore")));
        certificationApplyService.qualify(id, score,
                body.get("suggestion") == null ? null : String.valueOf(body.get("suggestion")),
                body.get("result") == null ? null : String.valueOf(body.get("result")));
        return Result.ok();
    }
}
