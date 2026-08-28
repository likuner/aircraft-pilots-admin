package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.entity.StuPilotProfile;
import com.uav.admin.service.StudentProfileService;
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
import java.util.Map;

/**
 * 考生档案接口
 */
@Tag(name = "考生档案")
@RestController
@RequestMapping("/api/student/profiles")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @Operation(summary = "分页查询考生档案")
    @GetMapping
    @PreAuthorize("hasAuthority('student:profile:list')")
    public Result<PageResult<StuPilotProfile>> page(PageQuery query, String keyword, String pilotType,
                                                    Long institutionId, String status) {
        return Result.ok(studentProfileService.page(query.getPageNum(), query.getPageSize(),
                keyword, pilotType, institutionId, status));
    }

    @Operation(summary = "档案详情（含考试/证书历史）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('student:profile:list')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(studentProfileService.detail(id));
    }

    @Operation(summary = "考试/证书记录时间线")
    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('student:profile:list')")
    public Result<List<Map<String, Object>>> records(@PathVariable Long id) {
        return Result.ok(studentProfileService.records(id));
    }

    @Operation(summary = "新建档案")
    @PostMapping
    @PreAuthorize("hasAuthority('student:profile:add')")
    @Log(module = "student", operation = "新建考生档案")
    public Result<Void> create(@RequestBody StuPilotProfile profile) {
        studentProfileService.create(profile);
        return Result.ok();
    }

    @Operation(summary = "编辑档案")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('student:profile:edit')")
    @Log(module = "student", operation = "编辑考生档案")
    public Result<Void> update(@PathVariable Long id, @RequestBody StuPilotProfile profile) {
        profile.setId(id);
        studentProfileService.update(profile);
        return Result.ok();
    }

    @Operation(summary = "删除档案")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('student:profile:delete')")
    @Log(module = "student", operation = "删除考生档案")
    public Result<Void> delete(@PathVariable Long id) {
        studentProfileService.delete(id);
        return Result.ok();
    }
}
