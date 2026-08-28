package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.ExmBatch;
import com.uav.admin.service.ExamBatchService;
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

import java.util.List;

/**
 * 批次接口
 */
@Tag(name = "考试批次")
@RestController
@RequestMapping("/api/exam/batches")
@RequiredArgsConstructor
public class ExamBatchController {

    private final ExamBatchService examBatchService;

    @Operation(summary = "分页查询批次")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:batch:list')")
    public Result<PageResult<ExmBatch>> page(PageQuery query, Long sessionId, String status) {
        return Result.ok(examBatchService.page(query.getPageNum(), query.getPageSize(), sessionId, status));
    }

    @Operation(summary = "场次下批次列表")
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAuthority('exam:batch:list')")
    public Result<List<ExmBatch>> listBySession(@PathVariable Long sessionId) {
        return Result.ok(examBatchService.listBySession(sessionId));
    }

    @Operation(summary = "新增批次")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:batch:add')")
    @Log(module = "exam", operation = "新增批次")
    public Result<Void> create(@RequestBody ExmBatch batch) {
        examBatchService.create(batch);
        return Result.ok();
    }

    @Operation(summary = "编辑批次")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:batch:edit')")
    @Log(module = "exam", operation = "编辑批次")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExmBatch batch) {
        batch.setId(id);
        examBatchService.update(batch);
        return Result.ok();
    }

    @Operation(summary = "删除批次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:batch:delete')")
    @Log(module = "exam", operation = "删除批次")
    public Result<Void> delete(@PathVariable Long id) {
        examBatchService.delete(id);
        return Result.ok();
    }
}
