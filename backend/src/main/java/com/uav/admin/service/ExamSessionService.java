package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.ExmExamSession;
import com.uav.admin.mapper.ExmExamSessionMapper;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 考试场次服务
 */
@Service
@RequiredArgsConstructor
public class ExamSessionService {

    private final ExmExamSessionMapper sessionMapper;
    private final RedisUtil redisUtil;

    public PageResult<ExmExamSession> page(long page, long size, Long planId, String examType,
                                           String status, String keyword) {
        LambdaQueryWrapper<ExmExamSession> wrapper = new LambdaQueryWrapper<>();
        if (planId != null) {
            wrapper.eq(ExmExamSession::getPlanId, planId);
        }
        if (StringUtils.hasText(examType)) {
            wrapper.eq(ExmExamSession::getExamType, examType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExmExamSession::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ExmExamSession::getSessionName, keyword)
                    .or().like(ExmExamSession::getSessionCode, keyword));
        }
        wrapper.orderByDesc(ExmExamSession::getExamDate);
        Page<ExmExamSession> p = sessionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public void create(ExmExamSession session) {
        if (!StringUtils.hasText(session.getSessionName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "场次名称不能为空");
        }
        if (session.getCapacity() == null || session.getCapacity() <= 0) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "名额必须大于 0");
        }
        if (session.getPassScore() == null) {
            session.setPassScore(java.math.BigDecimal.valueOf(60));
        }
        if (session.getFullScore() == null) {
            session.setFullScore(java.math.BigDecimal.valueOf(100));
        }
        session.setSessionCode(ExamPlanService.genCode("S"));
        session.setStatus("DRAFT");
        session.setEnrolledCount(0);
        sessionMapper.insert(session);
    }

    public void update(ExmExamSession session) {
        ExmExamSession db = sessionMapper.selectById(session.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试场次不存在");
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可编辑");
        }
        sessionMapper.updateById(session);
    }

    public void delete(Long id) {
        ExmExamSession db = sessionMapper.selectById(id);
        if (db == null) {
            return;
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可删除");
        }
        sessionMapper.deleteById(id);
    }

    /**
     * 发布场次：DRAFT → PUBLISHED，并初始化 Redis 名额（原子扣减用）
     */
    public void publish(Long id) {
        ExmExamSession db = sessionMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试场次不存在");
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可发布");
        }
        ExmExamSession update = new ExmExamSession();
        update.setId(id);
        update.setStatus("PUBLISHED");
        sessionMapper.updateById(update);

        // 初始化名额 key：剩余名额 = capacity - enrolledCount
        int remain = db.getCapacity() - (db.getEnrolledCount() == null ? 0 : db.getEnrolledCount());
        String key = capacityKey(id);
        String val = redisUtil.get(key);
        if (val == null) {
            redisUtil.set(key, String.valueOf(Math.max(remain, 0)), Duration.ofDays(90));
        }
    }

    /**
     * 截止报名：PUBLISHED → ENROLLMENT_CLOSED
     */
    public void closeEnrollment(Long id) {
        ExmExamSession db = sessionMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试场次不存在");
        }
        if ("ENROLLMENT_CLOSED".equals(db.getStatus())) {
            return;
        }
        if (!"PUBLISHED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅已发布场次可截止报名");
        }
        ExmExamSession update = new ExmExamSession();
        update.setId(id);
        update.setStatus("ENROLLMENT_CLOSED");
        sessionMapper.updateById(update);
    }

    /** 场次名额 Redis key */
    public String capacityKey(Long sessionId) {
        return Constants.SESSION_CAPACITY_PREFIX + sessionId + Constants.CAPACITY_SUFFIX;
    }
}
