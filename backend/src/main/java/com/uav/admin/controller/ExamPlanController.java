package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.ExmExamPlan;
import com.uav.admin.service.ExamPlanService;
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
 * 考试计划接口
 */
@Tag(name = "考试计划")
@RestController
@RequestMapping("/api/exam/plans")
@RequiredArgsConstructor
public class ExamPlanController {

    private final ExamPlanService examPlanService;

    @Operation(summary = "分页查询考试计划")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:plan:list')")
    public Result<PageResult<ExmExamPlan>> page(PageQuery query, String keyword, String status, String examType) {
        return Result.ok(examPlanService.page(query.getPageNum(), query.getPageSize(), keyword, status, examType));
    }

    @Operation(summary = "新增考试计划")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:plan:add')")
    @Log(module = "exam", operation = "新增考试计划")
    public Result<Void> create(@RequestBody ExmExamPlan plan) {
        examPlanService.create(plan);
        return Result.ok();
    }

    @Operation(summary = "编辑考试计划")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:plan:edit')")
    @Log(module = "exam", operation = "编辑考试计划")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExmExamPlan plan) {
        plan.setId(id);
        examPlanService.update(plan);
        return Result.ok();
    }

    @Operation(summary = "删除考试计划")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:plan:delete')")
    @Log(module = "exam", operation = "删除考试计划")
    public Result<Void> delete(@PathVariable Long id) {
        examPlanService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "发布计划")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('exam:plan:publish')")
    @Log(module = "exam", operation = "发布考试计划")
    public Result<Void> publish(@PathVariable Long id) {
        examPlanService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "取消计划")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('exam:plan:publish')")
    @Log(module = "exam", operation = "取消考试计划")
    public Result<Void> cancel(@PathVariable Long id) {
        examPlanService.cancel(id);
        return Result.ok();
    }
}
