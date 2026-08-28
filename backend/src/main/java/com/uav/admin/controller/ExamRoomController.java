package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.ExmExamRoom;
import com.uav.admin.service.ExamRoomService;
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
 * 考场接口
 */
@Tag(name = "考场管理")
@RestController
@RequestMapping("/api/exam/rooms")
@RequiredArgsConstructor
public class ExamRoomController {

    private final ExamRoomService examRoomService;

    @Operation(summary = "分页查询考场")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:room:list')")
    public Result<PageResult<ExmExamRoom>> page(PageQuery query, String keyword, Integer status) {
        return Result.ok(examRoomService.page(query.getPageNum(), query.getPageSize(), keyword, status));
    }

    @Operation(summary = "新增考场")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:room:add')")
    @Log(module = "exam", operation = "新增考场")
    public Result<Void> create(@RequestBody ExmExamRoom room) {
        examRoomService.create(room);
        return Result.ok();
    }

    @Operation(summary = "编辑考场")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:room:edit')")
    @Log(module = "exam", operation = "编辑考场")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExmExamRoom room) {
        room.setId(id);
        examRoomService.update(room);
        return Result.ok();
    }

    @Operation(summary = "删除考场")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:room:delete')")
    @Log(module = "exam", operation = "删除考场")
    public Result<Void> delete(@PathVariable Long id) {
        examRoomService.delete(id);
        return Result.ok();
    }
}
