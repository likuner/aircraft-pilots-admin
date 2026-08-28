package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.dto.ScoreCreateDTO;
import com.uav.admin.entity.ExmExamSession;
import com.uav.admin.entity.ExmRegistration;
import com.uav.admin.entity.ExmScore;
import com.uav.admin.entity.ExmScoreAudit;
import com.uav.admin.mapper.ExmExamSessionMapper;
import com.uav.admin.mapper.ExmRegistrationMapper;
import com.uav.admin.mapper.ExmScoreAuditMapper;
import com.uav.admin.mapper.ExmScoreMapper;
import com.uav.admin.mq.MqMessage;
import com.uav.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 成绩服务：录入 → 提交 → 审核（自动判定）→ MQ 结果通知
 */
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ExmScoreMapper scoreMapper;
    private final ExmScoreAuditMapper scoreAuditMapper;
    private final ExmRegistrationMapper registrationMapper;
    private final ExmExamSessionMapper sessionMapper;
    private final RabbitTemplate rabbitTemplate;

    public PageResult<ExmScore> page(long page, long size, Long sessionId, String status, String passStatus,
                                     String keyword) {
        LambdaQueryWrapper<ExmScore> wrapper = new LambdaQueryWrapper<>();
        if (sessionId != null) {
            wrapper.eq(ExmScore::getSessionId, sessionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExmScore::getStatus, status);
        }
        if (StringUtils.hasText(passStatus)) {
            wrapper.eq(ExmScore::getPassStatus, passStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.eq(ExmScore::getStudentUserId, keyword);
        }
        wrapper.orderByDesc(ExmScore::getEntryTime);
        Page<ExmScore> p = scoreMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 考官录入成绩（DRAFT）
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(ScoreCreateDTO dto) {
        if (dto.getRegistrationId() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "报名单不能为空");
        }
        ExmRegistration reg = registrationMapper.selectById(dto.getRegistrationId());
        if (reg == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "报名记录不存在");
        }
        // 一单唯一成绩
        Long exists = scoreMapper.selectCount(new LambdaQueryWrapper<ExmScore>()
                .eq(ExmScore::getRegistrationId, dto.getRegistrationId()));
        if (exists != null && exists > 0) {
            throw new BaseException(ErrorCode.DATA_EXISTS, "该报名单已存在成绩记录");
        }
        ExmExamSession session = sessionMapper.selectById(reg.getSessionId());
        if (session == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "场次不存在");
        }
        if (dto.getScore() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "分数不能为空");
        }
        ExmScore score = new ExmScore();
        score.setRegistrationId(dto.getRegistrationId());
        score.setSessionId(reg.getSessionId());
        score.setStudentUserId(reg.getStudentUserId());
        score.setExamType(StringUtils.hasText(dto.getExamType()) ? dto.getExamType() : session.getExamType());
        score.setScore(dto.getScore());
        score.setPassStatus("NOT_EVALUATED");
        score.setStatus("DRAFT");
        score.setExaminerId(SecurityUtils.getUserId());
        score.setEntryTime(LocalDateTime.now());
        score.setRemark(dto.getRemark());
        scoreMapper.insert(score);
    }

    /**
     * 编辑成绩（仅 DRAFT）
     */
    public void update(ExmScore score) {
        ExmScore db = scoreMapper.selectById(score.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "成绩记录不存在");
        }
        if (!"DRAFT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅草稿状态可编辑");
        }
        if (score.getScore() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "分数不能为空");
        }
        ExmScore upd = new ExmScore();
        upd.setId(score.getId());
        upd.setScore(score.getScore());
        upd.setRemark(score.getRemark());
        scoreMapper.updateById(upd);
    }

    /**
     * 提交成绩：DRAFT/REJECTED → SUBMITTED
     */
    public void submit(Long id) {
        ExmScore db = scoreMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "成绩记录不存在");
        }
        if (!"DRAFT".equals(db.getStatus()) && !"REJECTED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可提交");
        }
        ExmScore upd = new ExmScore();
        upd.setId(id);
        upd.setStatus("SUBMITTED");
        scoreMapper.updateById(upd);
    }

    /**
     * 成绩审核：SUBMITTED → APPROVED（自动判定 PASS/FAIL + MQ 通知）/ REJECTED
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String action, String comment) {
        ExmScore db = scoreMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "成绩记录不存在");
        }
        if (!"SUBMITTED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅待审核成绩可审核");
        }
        if (!"PASS".equals(action) && !"REJECT".equals(action)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "审核动作必须为 PASS/REJECT");
        }
        Long auditorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        // 写审核流水
        ExmScoreAudit audit = new ExmScoreAudit();
        audit.setScoreId(id);
        audit.setAuditorId(auditorId);
        audit.setAction(action);
        audit.setComment(comment);
        audit.setAuditTime(now);
        scoreAuditMapper.insert(audit);

        ExmScore upd = new ExmScore();
        upd.setId(id);
        upd.setAuditBy(auditorId);
        upd.setAuditTime(now);
        upd.setAuditRemark(comment);
        if ("REJECT".equals(action)) {
            upd.setStatus("REJECTED");
            scoreMapper.updateById(upd);
            return;
        }

        // 通过：按场次及格线自动判定
        ExmExamSession session = sessionMapper.selectById(db.getSessionId());
        BigDecimal passScore = session != null && session.getPassScore() != null
                ? session.getPassScore() : BigDecimal.valueOf(60);
        boolean pass = db.getScore() != null && db.getScore().compareTo(passScore) >= 0;
        upd.setStatus("APPROVED");
        upd.setPassStatus(pass ? "PASS" : "FAIL");
        scoreMapper.updateById(upd);

        // MQ 通知考生（成绩结果）
        Map<String, Object> payload = new HashMap<>();
        payload.put("scoreId", db.getId());
        payload.put("registrationId", db.getRegistrationId());
        payload.put("studentUserId", db.getStudentUserId());
        payload.put("sessionId", db.getSessionId());
        payload.put("examType", db.getExamType());
        payload.put("score", db.getScore());
        payload.put("passStatus", pass ? "PASS" : "FAIL");
        payload.put("auditTime", now.toString());
        MqMessage message = new MqMessage("EXAM_RESULT", payload, UUID.randomUUID().toString(), now.toString());
        rabbitTemplate.convertAndSend(Constants.DIRECT_EXCHANGE, Constants.EXAM_RESULT_ROUTING, message);
    }
}
