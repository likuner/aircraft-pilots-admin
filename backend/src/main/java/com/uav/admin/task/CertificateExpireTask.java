package com.uav.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.entity.CerCertificate;
import com.uav.admin.entity.SysNotice;
import com.uav.admin.mapper.CerCertificateMapper;
import com.uav.admin.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合格证到期任务：
 * 1. 到期（valid_until <= today）→ VALID → EXPIRED
 * 2. 临期（valid_until - 30d <= today）→ 生成预警公告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateExpireTask {

    private final CerCertificateMapper certificateMapper;
    private final SysNoticeMapper noticeMapper;

    /** 每天凌晨 1 点执行 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void run() {
        LocalDate today = LocalDate.now();
        // 到期自动流转
        List<CerCertificate> expired = certificateMapper.selectList(
                new LambdaQueryWrapper<CerCertificate>()
                        .eq(CerCertificate::getStatus, "VALID")
                        .le(CerCertificate::getValidUntil, today));
        for (CerCertificate cert : expired) {
            CerCertificate upd = new CerCertificate();
            upd.setId(cert.getId());
            upd.setStatus("EXPIRED");
            certificateMapper.updateById(upd);
            log.info("合格证 {} 已到期", cert.getCertNo());
        }
        // 30 天临期预警
        LocalDate warnBefore = today.plusDays(30);
        List<CerCertificate> warning = certificateMapper.selectList(
                new LambdaQueryWrapper<CerCertificate>()
                        .eq(CerCertificate::getStatus, "VALID")
                        .le(CerCertificate::getValidUntil, warnBefore)
                        .gt(CerCertificate::getValidUntil, today));
        for (CerCertificate cert : warning) {
            Long exists = noticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                    .eq(SysNotice::getTitle, "合格证到期预警")
                    .eq(SysNotice::getTargetRole, "STUDENT:" + cert.getStudentUserId())
                    .apply("DATE(publish_time) = CURDATE()"));
            if (exists != null && exists > 0) {
                continue;
            }
            SysNotice notice = new SysNotice();
            notice.setTitle("合格证到期预警");
            notice.setContent("您的合格证 " + cert.getCertNo() + " 将于 " + cert.getValidUntil()
                    + " 到期，请及时办理换发/续期手续。");
            notice.setNoticeType("WARNING");
            notice.setStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
            notice.setTargetRole("STUDENT:" + cert.getStudentUserId());
            noticeMapper.insert(notice);
        }
    }
}
