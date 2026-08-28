package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.CerCertificate;
import com.uav.admin.entity.ExmRegistration;
import com.uav.admin.entity.ExmScore;
import com.uav.admin.entity.StuPilotProfile;
import com.uav.admin.mapper.CerCertificateMapper;
import com.uav.admin.mapper.ExmRegistrationMapper;
import com.uav.admin.mapper.ExmScoreMapper;
import com.uav.admin.mapper.StuPilotProfileMapper;
import com.uav.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考生档案服务
 */
@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StuPilotProfileMapper profileMapper;
    private final ExmRegistrationMapper registrationMapper;
    private final ExmScoreMapper scoreMapper;
    private final CerCertificateMapper certificateMapper;

    /**
     * 分页查询档案（多条件）
     */
    public PageResult<StuPilotProfile> page(long page, long size, String keyword, String pilotType,
                                            Long institutionId, String status) {
        LambdaQueryWrapper<StuPilotProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(StuPilotProfile::getName, keyword)
                    .or().like(StuPilotProfile::getIdCard, keyword)
                    .or().like(StuPilotProfile::getPhone, keyword));
        }
        if (StringUtils.hasText(pilotType)) {
            wrapper.eq(StuPilotProfile::getPilotType, pilotType);
        }
        if (institutionId != null) {
            wrapper.eq(StuPilotProfile::getInstitutionId, institutionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(StuPilotProfile::getStatus, status);
        }
        wrapper.orderByDesc(StuPilotProfile::getCreateTime);
        Page<StuPilotProfile> p = profileMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 档案详情（含考试与证书历史）
     */
    public Map<String, Object> detail(Long id) {
        StuPilotProfile profile = profileMapper.selectById(id);
        if (profile == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "档案不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);

        List<ExmRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<ExmRegistration>()
                        .eq(ExmRegistration::getStudentProfileId, id)
                        .orderByDesc(ExmRegistration::getApplyTime));
        data.put("registrations", registrations);

        if (profile.getUserId() != null) {
            List<ExmScore> scores = scoreMapper.selectList(
                    new LambdaQueryWrapper<ExmScore>()
                            .eq(ExmScore::getStudentUserId, profile.getUserId())
                            .orderByDesc(ExmScore::getEntryTime));
            data.put("scores", scores);

            List<CerCertificate> certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<CerCertificate>()
                            .eq(CerCertificate::getStudentUserId, profile.getUserId())
                            .orderByDesc(CerCertificate::getIssueDate));
            data.put("certificates", certs);
        }
        return data;
    }

    /**
     * 考试/证书记录时间线
     */
    public List<Map<String, Object>> records(Long id) {
        StuPilotProfile profile = profileMapper.selectById(id);
        if (profile == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "档案不存在");
        }
        List<Map<String, Object>> timeline = new ArrayList<>();
        List<ExmRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<ExmRegistration>()
                        .eq(ExmRegistration::getStudentProfileId, id));
        for (ExmRegistration reg : registrations) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "REGISTRATION");
            item.put("title", "报名记录 " + reg.getRegistrationNo());
            item.put("time", reg.getApplyTime() == null ? null : reg.getApplyTime().toString());
            item.put("status", reg.getStatus());
            timeline.add(item);
        }
        if (profile.getUserId() != null) {
            List<ExmScore> scores = scoreMapper.selectList(
                    new LambdaQueryWrapper<ExmScore>()
                            .eq(ExmScore::getStudentUserId, profile.getUserId()));
            for (ExmScore score : scores) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "SCORE");
                item.put("title", "考试成绩（" + score.getExamType() + "）" + score.getScore());
                item.put("time", score.getEntryTime() == null ? null : score.getEntryTime().toString());
                item.put("status", score.getPassStatus());
                timeline.add(item);
            }
            List<CerCertificate> certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<CerCertificate>()
                            .eq(CerCertificate::getStudentUserId, profile.getUserId()));
            for (CerCertificate cert : certs) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "CERTIFICATE");
                item.put("title", "合格证 " + cert.getCertNo());
                item.put("time", cert.getIssueDate() == null ? null : cert.getIssueDate().toString());
                item.put("status", cert.getStatus());
                timeline.add(item);
            }
        }
        timeline.sort((a, b) -> String.valueOf(b.get("time")).compareTo(String.valueOf(a.get("time"))));
        return timeline;
    }

    /**
     * 新建档案
     */
    public void create(StuPilotProfile profile) {
        if (!StringUtils.hasText(profile.getName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "姓名不能为空");
        }
        if (StringUtils.hasText(profile.getIdCard())) {
            Long exists = profileMapper.selectCount(new LambdaQueryWrapper<StuPilotProfile>()
                    .eq(StuPilotProfile::getIdCard, profile.getIdCard()));
            if (exists != null && exists > 0) {
                throw new BaseException(ErrorCode.DATA_EXISTS, "该身份证号已存在档案");
            }
        }
        if (profile.getStatus() == null) {
            profile.setStatus("ACTIVE");
        }
        // 未指定所属用户时，默认绑定当前登录用户（user_id NOT NULL）
        if (profile.getUserId() == null) {
            profile.setUserId(SecurityUtils.getUserId());
        }
        profileMapper.insert(profile);
    }

    /**
     * 编辑档案
     */
    public void update(StuPilotProfile profile) {
        StuPilotProfile db = profileMapper.selectById(profile.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "档案不存在");
        }
        profileMapper.updateById(profile);
    }

    /**
     * 删除档案
     */
    public void delete(Long id) {
        profileMapper.deleteById(id);
    }
}
