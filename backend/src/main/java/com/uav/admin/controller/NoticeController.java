package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.SysNotice;
import com.uav.admin.service.NoticeService;
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
 * 公告通知接口
 */
@Tag(name = "公告通知")
@RestController
@RequestMapping("/api/system/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<PageResult<SysNotice>> page(PageQuery query, String keyword, String status, String noticeType) {
        return Result.ok(noticeService.page(query.getPageNum(), query.getPageSize(), keyword, status, noticeType));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    @PreAuthorize("hasAuthority('system:notice:add')")
    @Log(module = "system", operation = "新增公告")
    public Result<Void> create(@RequestBody SysNotice notice) {
        noticeService.create(notice);
        return Result.ok();
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:edit')")
    @Log(module = "system", operation = "更新公告")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysNotice notice) {
        notice.setId(id);
        noticeService.update(notice);
        return Result.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:delete')")
    @Log(module = "system", operation = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "发布公告（MQ 广播）")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:notice:publish')")
    @Log(module = "system", operation = "发布公告")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "撤下公告")
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('system:notice:publish')")
    @Log(module = "system", operation = "撤下公告")
    public Result<Void> unpublish(@PathVariable Long id) {
        noticeService.unpublish(id);
        return Result.ok();
    }

    @Operation(summary = "最新公告（首页）")
    @GetMapping("/latest")
    public Result<PageResult<SysNotice>> latest(Integer limit) {
        return Result.ok(noticeService.latest(limit == null ? 5 : limit));
    }
}
