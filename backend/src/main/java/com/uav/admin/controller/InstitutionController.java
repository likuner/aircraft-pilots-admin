package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.OrgInstitution;
import com.uav.admin.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 训练机构接口
 */
@Tag(name = "训练机构")
@RestController
@RequestMapping("/api/institution/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @Operation(summary = "分页查询机构")
    @GetMapping
    @PreAuthorize("hasAuthority('inst:institution:list')")
    public Result<PageResult<OrgInstitution>> page(PageQuery query, String keyword, String qualificationStatus) {
        return Result.ok(institutionService.page(query.getPageNum(), query.getPageSize(), keyword, qualificationStatus));
    }

    @Operation(summary = "机构详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:institution:list')")
    public Result<OrgInstitution> detail(@PathVariable Long id) {
        return Result.ok(institutionService.detail(id));
    }

    @Operation(summary = "新增机构")
    @PostMapping
    @PreAuthorize("hasAuthority('inst:institution:add')")
    @Log(module = "institution", operation = "新增训练机构")
    public Result<Void> create(@RequestBody OrgInstitution institution) {
        institutionService.create(institution);
        return Result.ok();
    }

    @Operation(summary = "编辑机构")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:institution:edit')")
    @Log(module = "institution", operation = "编辑训练机构")
    public Result<Void> update(@PathVariable Long id, @RequestBody OrgInstitution institution) {
        institution.setId(id);
        institutionService.update(institution);
        return Result.ok();
    }

    @Operation(summary = "删除机构")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inst:institution:delete')")
    @Log(module = "institution", operation = "删除训练机构")
    public Result<Void> delete(@PathVariable Long id) {
        institutionService.delete(id);
        return Result.ok();
    }
}
