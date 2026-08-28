package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.ExmExamSession;
import com.uav.admin.service.ExamSessionService;
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
 * 考试场次接口
 */
@Tag(name = "考试场次")
@RestController
@RequestMapping("/api/exam/sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    @Operation(summary = "分页查询考试场次")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:session:list')")
    public Result<PageResult<ExmExamSession>> page(PageQuery query, Long planId, String examType,
                                                   String status, String keyword) {
        return Result.ok(examSessionService.page(query.getPageNum(), query.getPageSize(),
                planId, examType, status, keyword));
    }

    @Operation(summary = "新增考试场次")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:session:add')")
    @Log(module = "exam", operation = "新增考试场次")
    public Result<Void> create(@RequestBody ExmExamSession session) {
        examSessionService.create(session);
        return Result.ok();
    }

    @Operation(summary = "编辑考试场次")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:session:edit')")
    @Log(module = "exam", operation = "编辑考试场次")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExmExamSession session) {
        session.setId(id);
        examSessionService.update(session);
        return Result.ok();
    }

    @Operation(summary = "删除考试场次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:session:delete')")
    @Log(module = "exam", operation = "删除考试场次")
    public Result<Void> delete(@PathVariable Long id) {
        examSessionService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "发布场次（开放报名）")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('exam:session:publish')")
    @Log(module = "exam", operation = "发布考试场次")
    public Result<Void> publish(@PathVariable Long id) {
        examSessionService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "截止报名")
    @PutMapping("/{id}/close-enrollment")
    @PreAuthorize("hasAuthority('exam:session:close')")
    @Log(module = "exam", operation = "截止场次报名")
    public Result<Void> closeEnrollment(@PathVariable Long id) {
        examSessionService.closeEnrollment(id);
        return Result.ok();
    }
}
