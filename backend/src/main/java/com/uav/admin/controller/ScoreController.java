package com.uav.admin.controller;

import com.uav.admin.aspect.Log;
import com.uav.admin.common.PageResult;
import com.uav.admin.common.Result;
import com.uav.admin.dto.PageQuery;
import com.uav.admin.dto.ScoreCreateDTO;
import com.uav.admin.entity.ExmScore;
import com.uav.admin.service.ScoreService;
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

import java.util.Map;

/**
 * 成绩管理接口
 */
@Tag(name = "成绩管理")
@RestController
@RequestMapping("/api/exam/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @Operation(summary = "分页查询成绩")
    @GetMapping
    @PreAuthorize("hasAuthority('exam:score:list')")
    public Result<PageResult<ExmScore>> page(PageQuery query, Long sessionId, String status, String passStatus, String keyword) {
        return Result.ok(scoreService.page(query.getPageNum(), query.getPageSize(), sessionId, status, passStatus, keyword));
    }

    @Operation(summary = "考官录入成绩")
    @PostMapping
    @PreAuthorize("hasAuthority('exam:score:add')")
    @Log(module = "exam", operation = "录入成绩")
    public Result<Void> create(@RequestBody ScoreCreateDTO dto) {
        scoreService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑成绩")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam:score:add')")
    @Log(module = "exam", operation = "编辑成绩")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExmScore score) {
        score.setId(id);
        scoreService.update(score);
        return Result.ok();
    }

    @Operation(summary = "提交成绩（进入待审）")
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('exam:score:submit')")
    @Log(module = "exam", operation = "提交成绩")
    public Result<Void> submit(@PathVariable Long id) {
        scoreService.submit(id);
        return Result.ok();
    }

    @Operation(summary = "成绩审核（自动判定+通知）")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('exam:scoreAudit:audit')")
    @Log(module = "exam", operation = "成绩审核")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        scoreService.audit(id, body.get("action"), body.get("comment"));
        return Result.ok();
    }
}
