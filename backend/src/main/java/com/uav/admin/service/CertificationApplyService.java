package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.OrgApplyMaterial;
import com.uav.admin.entity.OrgCertificationApply;
import com.uav.admin.entity.OrgInstitution;
import com.uav.admin.entity.OrgMaterialReview;
import com.uav.admin.entity.OrgQualification;
import com.uav.admin.entity.OrgQualificationReview;
import com.uav.admin.entity.OrgSiteInspection;
import com.uav.admin.mapper.OrgApplyMaterialMapper;
import com.uav.admin.mapper.OrgCertificationApplyMapper;
import com.uav.admin.mapper.OrgInstitutionMapper;
import com.uav.admin.mapper.OrgMaterialReviewMapper;
import com.uav.admin.mapper.OrgQualificationMapper;
import com.uav.admin.mapper.OrgQualificationReviewMapper;
import com.uav.admin.mapper.OrgSiteInspectionMapper;
import com.uav.admin.mq.MqMessage;
import com.uav.admin.security.SecurityUtils;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 机构认证申请服务：提交 → 材料审查 → 实地核查 → 资质评定 → 发证（状态机唯一入口）
 */
@Service
@RequiredArgsConstructor
public class CertificationApplyService {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final OrgCertificationApplyMapper applyMapper;
    private final OrgApplyMaterialMapper materialMapper;
    private final OrgMaterialReviewMapper reviewMapper;
    private final OrgSiteInspectionMapper inspectionMapper;
    private final OrgQualificationReviewMapper qualificationReviewMapper;
    private final OrgQualificationMapper qualificationMapper;
    private final OrgInstitutionMapper institutionMapper;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;

    @Value("${uav.qualification.valid-years:3}")
    private int validYears;

    public PageResult<OrgCertificationApply> page(long page, long size, Long institutionId, String status) {
        LambdaQueryWrapper<OrgCertificationApply> wrapper = new LambdaQueryWrapper<>();
        if (institutionId != null) {
            wrapper.eq(OrgCertificationApply::getInstitutionId, institutionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrgCertificationApply::getStatus, status);
        }
        wrapper.orderByDesc(OrgCertificationApply::getApplyTime);
        Page<OrgCertificationApply> p = applyMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 申请详情：申请 + 材料 + 审查记录 + 核查任务 + 评定记录
     */
    public Map<String, Object> detail(Long id) {
        OrgCertificationApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "认证申请不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("apply", apply);
        data.put("materials", materialMapper.selectList(new LambdaQueryWrapper<OrgApplyMaterial>()
                .eq(OrgApplyMaterial::getApplyId, id)));
        data.put("reviews", reviewMapper.selectList(new LambdaQueryWrapper<OrgMaterialReview>()
                .eq(OrgMaterialReview::getApplyId, id).orderByDesc(OrgMaterialReview::getReviewTime)));
        data.put("inspections", inspectionMapper.selectList(new LambdaQueryWrapper<OrgSiteInspection>()
                .eq(OrgSiteInspection::getApplyId, id).orderByDesc(OrgSiteInspection::getCreateTime)));
        data.put("qualificationReviews", qualificationReviewMapper.selectList(
                new LambdaQueryWrapper<OrgQualificationReview>()
                        .eq(OrgQualificationReview::getApplyId, id).orderByDesc(OrgQualificationReview::getReviewTime)));
        return data;
    }

    /**
     * 提交认证申请：→ SUBMITTED
     */
    public void create(OrgCertificationApply apply) {
        if (apply.getInstitutionId() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "机构不能为空");
        }
        OrgInstitution inst = institutionMapper.selectById(apply.getInstitutionId());
        if (inst == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "机构不存在");
        }
        apply.setApplyNo(ExamPlanService.genCode("IA"));
        if (apply.getApplyType() == null) {
            apply.setApplyType("NEW");
        }
        apply.setCurrentStep(1);
        apply.setApplyTime(LocalDateTime.now());
        apply.setStatus("SUBMITTED");
        apply.setSubmittedBy(SecurityUtils.getUserId());
        applyMapper.insert(apply);
    }

    /**
     * 提交材料：SUBMITTED/MATERIAL_REJECTED → MATERIAL_REVIEWING
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitMaterial(Long applyId, List<OrgApplyMaterial> materials) {
        OrgCertificationApply apply = getAndCheck(applyId);
        if (!"SUBMITTED".equals(apply.getStatus()) && !"MATERIAL_REJECTED".equals(apply.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可提交材料");
        }
        if (materials == null || materials.isEmpty()) {
            throw new BaseException(ErrorCode.MATERIAL_REQUIRED);
        }
        Long userId = SecurityUtils.getUserId();
        for (OrgApplyMaterial m : materials) {
            m.setId(null);
            m.setApplyId(applyId);
            m.setUploadBy(userId);
            m.setUploadTime(LocalDateTime.now());
            materialMapper.insert(m);
        }
        transition(applyId, "MATERIAL_REVIEWING", 1);
        notifyInst("机构认证材料已提交，进入材料审查", apply);
    }

    /**
     * 材料审查：MATERIAL_REVIEWING → MATERIAL_PASSED / MATERIAL_REJECTED
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewMaterial(Long applyId, String result, String comment, Integer reviewStep) {
        OrgCertificationApply apply = getAndCheck(applyId);
        if (!"MATERIAL_REVIEWING".equals(apply.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可审查材料");
        }
        if (!"PASS".equals(result) && !"REJECT".equals(result)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "审查结果必须为 PASS/REJECT");
        }
        OrgMaterialReview review = new OrgMaterialReview();
        review.setApplyId(applyId);
        review.setReviewerId(SecurityUtils.getUserId());
        review.setResult(result);
        review.setComment(comment);
        review.setReviewStep(reviewStep == null ? 1 : reviewStep);
        review.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(review);

        if ("PASS".equals(result)) {
            transition(applyId, "MATERIAL_PASSED", 2);
            notifyInst("机构认证材料审查通过，等待实地核查派发", apply);
        } else {
            transition(applyId, "MATERIAL_REJECTED", 1);
            notifyInst("机构认证材料被退回，请补充后重新提交", apply);
        }
    }

    /**
     * 派发核查任务：MATERIAL_PASSED → INSPECTION_SCHEDULED
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignInspection(Long applyId, Long inspectorId, LocalDate inspectionDate,
                                 String address, String checklist) {
        OrgCertificationApply apply = getAndCheck(applyId);
        if (!"MATERIAL_PASSED".equals(apply.getStatus()) && !"INSPECTION_SCHEDULED".equals(apply.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可派发核查");
        }
        if (inspectorId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "请指定核查员");
        }
        OrgSiteInspection existing = inspectionMapper.selectOne(new LambdaQueryWrapper<OrgSiteInspection>()
                .eq(OrgSiteInspection::getApplyId, applyId).last("LIMIT 1"));
        OrgSiteInspection inspection;
        if (existing == null) {
            inspection = new OrgSiteInspection();
            inspection.setApplyId(applyId);
            inspection.setInstitutionId(apply.getInstitutionId());
            inspection.setInspectorId(inspectorId);
            inspection.setInspectionDate(inspectionDate);
            inspection.setAddress(StringUtils.hasText(address) ? address : null);
            inspection.setChecklist(checklist);
            inspection.setStatus("ASSIGNED");
            inspection.setResult("PENDING");
            inspection.setAssignTime(LocalDateTime.now());
            inspectionMapper.insert(inspection);
        } else {
            inspection = existing;
            OrgSiteInspection upd = new OrgSiteInspection();
            upd.setId(existing.getId());
            upd.setInspectorId(inspectorId);
            upd.setInspectionDate(inspectionDate);
            upd.setAddress(address);
            upd.setChecklist(checklist);
            upd.setStatus("ASSIGNED");
            upd.setResult("PENDING");
            upd.setAssignTime(LocalDateTime.now());
            inspectionMapper.updateById(upd);
        }
        transition(applyId, "INSPECTION_SCHEDULED", 3);
        notifyInst("机构认证核查任务已派发，请配合实地核查", apply);
    }

    /**
     * 完成实地核查：ASSIGNED → COMPLETED；PASS → QUALIFICATION_REVIEWING，FAIL → MATERIAL_REJECTED
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeInspection(Long inspectionId, String result, String summary) {
        OrgSiteInspection inspection = inspectionMapper.selectById(inspectionId);
        if (inspection == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "核查任务不存在");
        }
        if (!"ASSIGNED".equals(inspection.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅已派发的核查任务可提交结果");
        }
        if (!"PASS".equals(result) && !"FAIL".equals(result)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "核查结果必须为 PASS/FAIL");
        }
        OrgSiteInspection upd = new OrgSiteInspection();
        upd.setId(inspectionId);
        upd.setStatus("COMPLETED");
        upd.setResult(result);
        upd.setSummary(summary);
        upd.setCompleteTime(LocalDateTime.now());
        inspectionMapper.updateById(upd);

        OrgCertificationApply apply = applyMapper.selectById(inspection.getApplyId());
        if (apply == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "认证申请不存在");
        }
        if ("PASS".equals(result)) {
            transition(apply.getId(), "QUALIFICATION_REVIEWING", 4);
            notifyInst("机构实地核查通过，进入资质评定", apply);
        } else {
            transition(apply.getId(), "MATERIAL_REJECTED", 1);
            notifyInst("机构实地核查未通过，认证流程终止", apply);
        }
    }

    /**
     * 资质评定：QUALIFICATION_REVIEWING → APPROVED（发证）/ REJECTED
     */
    @Transactional(rollbackFor = Exception.class)
    public void qualify(Long applyId, BigDecimal evaluationScore, String suggestion, String result) {
        OrgCertificationApply apply = getAndCheck(applyId);
        if (!"QUALIFICATION_REVIEWING".equals(apply.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "当前状态不可资质评定");
        }
        if (!"PASS".equals(result) && !"REJECT".equals(result)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "评定结果必须为 PASS/REJECT");
        }
        OrgQualificationReview review = new OrgQualificationReview();
        review.setApplyId(applyId);
        review.setReviewerId(SecurityUtils.getUserId());
        review.setEvaluationScore(evaluationScore);
        review.setSuggestion(suggestion);
        review.setResult(result);
        review.setReviewTime(LocalDateTime.now());
        qualificationReviewMapper.insert(review);

        if ("REJECT".equals(result)) {
            transition(applyId, "REJECTED", 5);
            notifyInst("机构资质评定未通过", apply);
            return;
        }
        transition(applyId, "APPROVED", 5);
        issueQualification(apply);
        notifyInst("机构资质评定通过，资质证书已签发", apply);
    }

    /**
     * 发证：生成资质证号，写 org_qualification，机构 qualification_status → CERTIFIED
     */
    private void issueQualification(OrgCertificationApply apply) {
        String year = LocalDate.now().format(YEAR_FMT);
        String qualNo = redisUtil.nextBizNo(Constants.QUALIFICATION_NO_SEQ, "QUAL-" + year + "-",
                no -> qualificationMapper.selectCount(new LambdaQueryWrapper<OrgQualification>()
                        .eq(OrgQualification::getQualificationNo, no)) > 0,
                () -> {
                    OrgQualification last = qualificationMapper.selectOne(new LambdaQueryWrapper<OrgQualification>()
                            .likeRight(OrgQualification::getQualificationNo, "QUAL-" + year + "-")
                            .orderByDesc(OrgQualification::getQualificationNo)
                            .last("LIMIT 1"));
                    return last == null ? null : last.getQualificationNo();
                });

        LocalDate today = LocalDate.now();
        OrgQualification qual = new OrgQualification();
        qual.setQualificationNo(qualNo);
        qual.setApplyId(apply.getId());
        qual.setInstitutionId(apply.getInstitutionId());
        qual.setQualificationLevel("A");
        qual.setCategory(apply.getCategory());
        qual.setIssueDate(today);
        qual.setValidUntil(today.plusYears(validYears));
        qual.setStatus("VALID");
        qual.setIssuerId(SecurityUtils.getUserId());
        qualificationMapper.insert(qual);

        // 联动机构资质状态
        OrgInstitution instUpd = new OrgInstitution();
        instUpd.setId(apply.getInstitutionId());
        instUpd.setQualificationStatus("CERTIFIED");
        institutionMapper.updateById(instUpd);
    }

    private void transition(Long applyId, String targetStatus, int step) {
        OrgCertificationApply upd = new OrgCertificationApply();
        upd.setId(applyId);
        upd.setStatus(targetStatus);
        upd.setCurrentStep(step);
        applyMapper.updateById(upd);
    }

    private OrgCertificationApply getAndCheck(Long id) {
        OrgCertificationApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "认证申请不存在");
        }
        return apply;
    }

    /** 机构消息通知（MQ） */
    private void notifyInst(String content, OrgCertificationApply apply) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applyId", apply.getId());
        payload.put("institutionId", apply.getInstitutionId());
        payload.put("content", content);
        MqMessage message = new MqMessage("INST_NOTIFY", payload, UUID.randomUUID().toString(),
                LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(Constants.DIRECT_EXCHANGE, Constants.INST_NOTIFY_ROUTING, message);
    }
}
