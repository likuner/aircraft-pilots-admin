package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.common.Constants;
import com.uav.admin.entity.CerCertificate;
import com.uav.admin.entity.CerCertificateApply;
import com.uav.admin.mapper.CerCertificateApplyMapper;
import com.uav.admin.mapper.CerCertificateMapper;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 合格证签发服务：生成证号、有效期，写 cer_certificate，回写申请状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertIssueService {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final CerCertificateMapper certificateMapper;
    private final CerCertificateApplyMapper applyMapper;
    private final RedisUtil redisUtil;

    @Value("${uav.cert.valid-years:6}")
    private int validYears;

    @Value("${uav.cert.issue-org:无人机驾驶员管理机构}")
    private String issueOrg;

    /**
     * 签发证书（消费端调用，含幂等保护）
     *
     * @return true 本次签发成功；false 重复/跳过
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean issue(Long applyId, Long registrationId, Long scoreId, Long studentUserId, String certificateType) {
        CerCertificateApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            log.warn("签发失败：申请不存在 applyId={}", applyId);
            return false;
        }
        // 幂等：已签发过则跳过
        if ("ISSUED".equals(apply.getStatus())) {
            log.info("申请已签发过，跳过 applyId={}", applyId);
            return false;
        }
        if (!"AUDIT_PASSED".equals(apply.getStatus())) {
            log.warn("申请状态不是 AUDIT_PASSED，跳过签发 applyId={}, status={}", applyId, apply.getStatus());
            return false;
        }

        // 生成证号：UVA-2026-000001（Redis 自增流水；撞号时以 DB 最大号兜底，防 Redis 计数器重置）
        String year = LocalDate.now().format(YEAR_FMT);
        String certNo = redisUtil.nextBizNo(Constants.CERT_NO_SEQ, "UVA-" + year + "-",
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
        CerCertificate cert = new CerCertificate();
        cert.setCertNo(certNo);
        cert.setApplyId(applyId);
        cert.setStudentUserId(studentUserId);
        cert.setCertificateType(certificateType);
        cert.setIssueDate(today);
        cert.setValidFrom(today);
        cert.setValidUntil(today.plusYears(validYears));
        cert.setStatus("VALID");
        cert.setIssuerId(apply.getAuditBy());
        cert.setIssueOrg(issueOrg);
        certificateMapper.insert(cert);

        // 回写申请状态 ISSUED
        CerCertificateApply upd = new CerCertificateApply();
        upd.setId(applyId);
        upd.setStatus("ISSUED");
        applyMapper.updateById(upd);

        log.info("合格证签发成功: certNo={}, applyId={}", certNo, applyId);
        return true;
    }
}
