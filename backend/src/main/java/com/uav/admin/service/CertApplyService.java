package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.dto.CertApplyCreateDTO;
import com.uav.admin.entity.CerCertificateApply;
import com.uav.admin.entity.CerCertificateAudit;
import com.uav.admin.entity.ExmRegistration;
import com.uav.admin.entity.ExmScore;
import com.uav.admin.mapper.CerCertificateApplyMapper;
import com.uav.admin.mapper.CerCertificateAuditMapper;
import com.uav.admin.mapper.ExmRegistrationMapper;
import com.uav.admin.mapper.ExmScoreMapper;
import com.uav.admin.mq.MqMessage;
import com.uav.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 合格证申请服务：成绩合格 → 提交申请 → 审核 → MQ 异步签发
 */
@Service
@RequiredArgsConstructor
public class CertApplyService {

    private final CerCertificateApplyMapper applyMapper;
    private final CerCertificateAuditMapper auditMapper;
    private final ExmRegistrationMapper registrationMapper;
    private final ExmScoreMapper scoreMapper;
    private final RabbitTemplate rabbitTemplate;

    public PageResult<CerCertificateApply> page(long page, long size, Long studentUserId, String status) {
        LambdaQueryWrapper<CerCertificateApply> wrapper = new LambdaQueryWrapper<>();
        if (studentUserId != null) {
            wrapper.eq(CerCertificateApply::getStudentUserId, studentUserId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CerCertificateApply::getStatus, status);
        }
        wrapper.orderByDesc(CerCertificateApply::getApplyTime);
        Page<CerCertificateApply> p = applyMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 提交申请：校验成绩合格 → SUBMITTED → PENDING_AUDIT
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(CertApplyCreateDTO dto) {
        if (dto.getRegistrationId() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "报名单不能为空");
        }
        ExmRegistration reg = registrationMapper.selectById(dto.getRegistrationId());
        if (reg == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "报名记录不存在");
        }
        // 一单一生效申请
        Long exists = applyMapper.selectCount(new LambdaQueryWrapper<CerCertificateApply>()
                .eq(CerCertificateApply::getRegistrationId, dto.getRegistrationId()));
        if (exists != null && exists > 0) {
            throw new BaseException(ErrorCode.CERT_EXISTS);
        }
        ExmScore score = null;
        if (dto.getScoreId() != null) {
            score = scoreMapper.selectById(dto.getScoreId());
        } else {
            score = scoreMapper.selectOne(new LambdaQueryWrapper<ExmScore>()
                    .eq(ExmScore::getRegistrationId, dto.getRegistrationId())
                    .eq(ExmScore::getPassStatus, "PASS")
                    .last("LIMIT 1"));
        }
        if (score == null || !"PASS".equals(score.getPassStatus()) || !"APPROVED".equals(score.getStatus())) {
            throw new BaseException(ErrorCode.NOT_PASS);
        }
        CerCertificateApply apply = new CerCertificateApply();
        apply.setApplyNo(ExamPlanService.genCode("CA"));
        apply.setRegistrationId(dto.getRegistrationId());
        apply.setScoreId(score.getId());
        apply.setStudentUserId(reg.getStudentUserId());
        apply.setCertificateType(dto.getCertificateType());
        apply.setApplyTime(LocalDateTime.now());
        apply.setStatus("PENDING_AUDIT");
        applyMapper.insert(apply);
    }

    /**
     * 申请审核：PENDING_AUDIT → AUDIT_PASSED（发 MQ 异步签发）/ AUDIT_REJECTED
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String action, String comment) {
        CerCertificateApply db = applyMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "申请不存在");
        }
        if (!"PENDING_AUDIT".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅审核中的申请可审核");
        }
        if (!"PASS".equals(action) && !"REJECT".equals(action)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "审核动作必须为 PASS/REJECT");
        }
        Long auditorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        CerCertificateAudit audit = new CerCertificateAudit();
        audit.setApplyId(id);
        audit.setAuditType("APPLY_AUDIT");
        audit.setAuditorId(auditorId);
        audit.setAction(action);
        audit.setComment(comment);
        audit.setAuditTime(now);
        auditMapper.insert(audit);

        CerCertificateApply upd = new CerCertificateApply();
        upd.setId(id);
        upd.setAuditBy(auditorId);
        upd.setAuditTime(now);
        upd.setAuditRemark(comment);
        if ("REJECT".equals(action)) {
            upd.setStatus("AUDIT_REJECTED");
            applyMapper.updateById(upd);
            return;
        }
        upd.setStatus("AUDIT_PASSED");
        applyMapper.updateById(upd);

        // 发 MQ 异步签发
        Map<String, Object> payload = new HashMap<>();
        payload.put("applyId", db.getId());
        payload.put("registrationId", db.getRegistrationId());
        payload.put("scoreId", db.getScoreId());
        payload.put("studentUserId", db.getStudentUserId());
        payload.put("certificateType", db.getCertificateType());
        MqMessage message = new MqMessage("CERT_ISSUE", payload, UUID.randomUUID().toString(), now.toString());
        rabbitTemplate.convertAndSend(Constants.DIRECT_EXCHANGE, Constants.CERT_ISSUE_ROUTING, message);
    }
}
