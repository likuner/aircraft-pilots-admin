package com.uav.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.entity.OrgInstitution;
import com.uav.admin.entity.OrgQualification;
import com.uav.admin.entity.SysNotice;
import com.uav.admin.mapper.OrgInstitutionMapper;
import com.uav.admin.mapper.OrgQualificationMapper;
import com.uav.admin.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 机构资质到期任务：到期 VALID → EXPIRED + 30 天预警，联动机构状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualificationExpireTask {

    private final OrgQualificationMapper qualificationMapper;
    private final OrgInstitutionMapper institutionMapper;
    private final SysNoticeMapper noticeMapper;

    /** 每天凌晨 2 点执行 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        LocalDate today = LocalDate.now();
        List<OrgQualification> expired = qualificationMapper.selectList(
                new LambdaQueryWrapper<OrgQualification>()
                        .eq(OrgQualification::getStatus, "VALID")
                        .le(OrgQualification::getValidUntil, today));
        for (OrgQualification qual : expired) {
            OrgQualification upd = new OrgQualification();
            upd.setId(qual.getId());
            upd.setStatus("EXPIRED");
            qualificationMapper.updateById(upd);

            OrgInstitution instUpd = new OrgInstitution();
            instUpd.setId(qual.getInstitutionId());
            instUpd.setQualificationStatus("EXPIRED");
            institutionMapper.updateById(instUpd);
            log.info("机构资质 {} 已到期", qual.getQualificationNo());
        }

        LocalDate warnBefore = today.plusDays(30);
        List<OrgQualification> warning = qualificationMapper.selectList(
                new LambdaQueryWrapper<OrgQualification>()
                        .eq(OrgQualification::getStatus, "VALID")
                        .le(OrgQualification::getValidUntil, warnBefore)
                        .gt(OrgQualification::getValidUntil, today));
        for (OrgQualification qual : warning) {
            Long exists = noticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                    .eq(SysNotice::getTitle, "机构资质到期预警")
                    .eq(SysNotice::getTargetRole, "INSTITUTION_ADMIN")
                    .apply("DATE(publish_time) = CURDATE()"));
            if (exists != null && exists > 0) {
                continue;
            }
            SysNotice notice = new SysNotice();
            notice.setTitle("机构资质到期预警");
            notice.setContent("机构资质证 " + qual.getQualificationNo() + " 将于 " + qual.getValidUntil()
                    + " 到期，请及时办理续期。");
            notice.setNoticeType("WARNING");
            notice.setStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
            notice.setTargetRole("INSTITUTION_ADMIN");
            noticeMapper.insert(notice);
        }
    }
}
