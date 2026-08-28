package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.CerCertificate;
import com.uav.admin.entity.CerCertificateChangeRecord;
import com.uav.admin.mapper.CerCertificateChangeRecordMapper;
import com.uav.admin.mapper.CerCertificateMapper;
import com.uav.admin.security.SecurityUtils;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 合格证服务：列表/详情/换发/吊销/变更记录
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final CerCertificateMapper certificateMapper;
    private final CerCertificateChangeRecordMapper changeRecordMapper;
    private final RedisUtil redisUtil;

    public PageResult<CerCertificate> page(long page, long size, String certNo, String status,
                                           Long studentUserId, Long applyId) {
        LambdaQueryWrapper<CerCertificate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(certNo)) {
            wrapper.like(CerCertificate::getCertNo, certNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CerCertificate::getStatus, status);
        }
        if (studentUserId != null) {
            wrapper.eq(CerCertificate::getStudentUserId, studentUserId);
        }
        if (applyId != null) {
            wrapper.eq(CerCertificate::getApplyId, applyId);
        }
        wrapper.orderByDesc(CerCertificate::getIssueDate);
        Page<CerCertificate> p = certificateMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public CerCertificate detail(Long id) {
        CerCertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "证书不存在");
        }
        return cert;
    }

    /**
     * 换发：原证 REISSUED，生成新证书（新证号新有效期），写变更记录
     */
    @Transactional(rollbackFor = Exception.class)
    public CerCertificate reissue(Long id, String reason) {
        CerCertificate db = certificateMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "证书不存在");
        }
        if (!"VALID".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅有效证书可换发");
        }
        String year = LocalDate.now().format(YEAR_FMT);
        String newCertNo = redisUtil.nextBizNo(Constants.CERT_NO_SEQ, "UVA-" + year + "-",
                no -> certificateMapper.selectCount(new LambdaQueryWrapper<CerCertificate>()
                        .eq(CerCertificate::getCertNo, no)) > 0,
                () -> {
                    CerCertificate last = certificateMapper.selectOne(new LambdaQueryWrapper<CerCertificate>()
                            .likeRight(CerCertificate::getCertNo, "UVA-" + year + "-")
                            .orderByDesc(CerCertificate::getCertNo)
                            .last("LIMIT 1"));
                    return last == null ? null : last.getCertNo();
                });

        LocalDate today = LocalDate.now();
        CerCertificate newCert = new CerCertificate();
        newCert.setCertNo(newCertNo);
        newCert.setApplyId(db.getApplyId());
        newCert.setStudentUserId(db.getStudentUserId());
        newCert.setCertificateType(db.getCertificateType());
        newCert.setIssueDate(today);
        newCert.setValidFrom(today);
        newCert.setValidUntil(today.plusYears(6));
        newCert.setStatus("VALID");
        newCert.setIssuerId(SecurityUtils.getUserId());
        newCert.setIssueOrg(db.getIssueOrg());
        newCert.setRemark("由证书 " + db.getCertNo() + " 换发");
        certificateMapper.insert(newCert);

        // 原证 REISSUED
        CerCertificate upd = new CerCertificate();
        upd.setId(id);
        upd.setStatus("REISSUED");
        certificateMapper.updateById(upd);

        writeChangeRecord(id, newCert.getId(), "REISSUE", reason, db.getStatus(), "REISSUED");
        return newCert;
    }

    /**
     * 吊销/作废：VALID → REVOKED / VOID
     */
    public void revoke(Long id, String reason, String changeType) {
        CerCertificate db = certificateMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "证书不存在");
        }
        if (!"VALID".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅有效证书可吊销/作废");
        }
        String type = "VOID".equals(changeType) ? "VOID" : "REVOKE";
        String targetStatus = "VOID".equals(type) ? "VOID" : "REVOKED";
        CerCertificate upd = new CerCertificate();
        upd.setId(id);
        upd.setStatus(targetStatus);
        certificateMapper.updateById(upd);
        writeChangeRecord(id, null, type, reason, "VALID", targetStatus);
    }

    public List<CerCertificateChangeRecord> changes(Long certId) {
        return changeRecordMapper.selectList(new LambdaQueryWrapper<CerCertificateChangeRecord>()
                .eq(CerCertificateChangeRecord::getCertId, certId)
                .orderByDesc(CerCertificateChangeRecord::getOperateTime));
    }

    private void writeChangeRecord(Long certId, Long newCertId, String changeType, String reason,
                                   String before, String after) {
        CerCertificateChangeRecord record = new CerCertificateChangeRecord();
        record.setCertId(certId);
        record.setNewCertId(newCertId);
        record.setChangeType(changeType);
        record.setReason(reason);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setBeforeStatus(before);
        record.setAfterStatus(after);
        record.setOperateTime(LocalDateTime.now());
        changeRecordMapper.insert(record);
    }
}
