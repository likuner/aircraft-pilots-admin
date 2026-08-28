package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.ExmExamPlan;
import com.uav.admin.mapper.ExmExamPlanMapper;
import com.uav.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 考试计划服务
 */
@Service
@RequiredArgsConstructor
public class ExamPlanService {

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExmExamPlanMapper planMapper;

    public PageResult<ExmExamPlan> page(long page, long size, String keyword, String status, String examType) {
        LambdaQueryWrapper<ExmExamPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ExmExamPlan::getPlanName, keyword)
                    .or().like(ExmExamPlan::getPlanCode, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExmExamPlan::getStatus, status);
        }
        if (StringUtils.hasText(examType)) {
            wrapper.eq(ExmExamPlan::getExamType, examType);
        }
        wrapper.orderByDesc(ExmExamPlan::getCreateTime);
        Page<ExmExamPlan> p = planMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public void create(ExmExamPlan plan) {
        if (!StringUtils.hasText(plan.getPlanName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "计划名称不能为空");
        }
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "计划起止日期不能为空");
        }
        plan.setPlanCode(genCode("PLAN"));
        plan.setStatus("DRAFT");
        plan.setCreatorId(SecurityUtils.getUserId());
        planMapper.insert(plan);
    }

    public void update(ExmExamPlan plan) {
        ExmExamPlan db = planMapper.selectById(plan.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试计划不存在");
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可编辑");
        }
        planMapper.updateById(plan);
    }

    public void delete(Long id) {
        ExmExamPlan db = planMapper.selectById(id);
        if (db == null) {
            return;
        }
        if ("PUBLISHED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "已发布计划不可删除，请先取消");
        }
        planMapper.deleteById(id);
    }

    /**
     * 发布计划：DRAFT → PUBLISHED
     */
    public void publish(Long id) {
        ExmExamPlan db = planMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试计划不存在");
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可发布");
        }
        ExmExamPlan update = new ExmExamPlan();
        update.setId(id);
        update.setStatus("PUBLISHED");
        planMapper.updateById(update);
    }

    /** 取消计划：PUBLISHED/DRAFT → CANCELLED */
    public void cancel(Long id) {
        ExmExamPlan db = planMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试计划不存在");
        }
        if ("CLOSED".equals(db.getStatus()) || "CANCELLED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可取消");
        }
        ExmExamPlan update = new ExmExamPlan();
        update.setId(id);
        update.setStatus("CANCELLED");
        planMapper.updateById(update);
    }

    public static String genCode(String prefix) {
        return prefix + LocalDateTime.now().format(CODE_FMT)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
