package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.OrgInstitution;
import com.uav.admin.entity.OrgQualification;
import com.uav.admin.mapper.OrgInstitutionMapper;
import com.uav.admin.mapper.OrgQualificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 机构资质证服务
 */
@Service
@RequiredArgsConstructor
public class QualificationService {

    private final OrgQualificationMapper qualificationMapper;
    private final OrgInstitutionMapper institutionMapper;

    public PageResult<OrgQualification> page(long page, long size, Long institutionId, String status, String keyword) {
        LambdaQueryWrapper<OrgQualification> wrapper = new LambdaQueryWrapper<>();
        if (institutionId != null) {
            wrapper.eq(OrgQualification::getInstitutionId, institutionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrgQualification::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(OrgQualification::getQualificationNo, keyword);
        }
        wrapper.orderByDesc(OrgQualification::getIssueDate);
        Page<OrgQualification> p = qualificationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public OrgQualification detail(Long id) {
        OrgQualification qual = qualificationMapper.selectById(id);
        if (qual == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "资质证不存在");
        }
        return qual;
    }

    /**
     * 续期：有效资质证有效期顺延 3 年
     */
    @Transactional(rollbackFor = Exception.class)
    public void renew(Long id) {
        OrgQualification db = qualificationMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "资质证不存在");
        }
        if (!"VALID".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅有效资质证可续期");
        }
        OrgQualification upd = new OrgQualification();
        upd.setId(id);
        LocalDate base = db.getValidUntil() == null ? LocalDate.now() : db.getValidUntil();
        upd.setValidUntil(base.isAfter(LocalDate.now()) ? base.plusYears(3) : LocalDate.now().plusYears(3));
        qualificationMapper.updateById(upd);
    }

    /**
     * 吊销/暂停：联动机构资质状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, String reason, String changeType) {
        OrgQualification db = qualificationMapper.selectById(id);
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "资质证不存在");
        }
        if (!"VALID".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅有效资质证可吊销/暂停");
        }
        boolean suspend = "SUSPEND".equals(changeType);
        String status = suspend ? "SUSPENDED" : "REVOKED";
        OrgQualification upd = new OrgQualification();
        upd.setId(id);
        upd.setStatus(status);
        upd.setRevokeReason(reason);
        qualificationMapper.updateById(upd);

        OrgInstitution instUpd = new OrgInstitution();
        instUpd.setId(db.getInstitutionId());
        instUpd.setQualificationStatus(suspend ? "SUSPENDED" : "REVOKED");
        institutionMapper.updateById(instUpd);
    }
}
