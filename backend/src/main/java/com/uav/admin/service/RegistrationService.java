package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.dto.RegistrationCreateDTO;
import com.uav.admin.entity.ExmBatch;
import com.uav.admin.entity.ExmExamSession;
import com.uav.admin.entity.ExmRegistration;
import com.uav.admin.entity.StuPilotProfile;
import com.uav.admin.mapper.ExmBatchMapper;
import com.uav.admin.mapper.ExmExamSessionMapper;
import com.uav.admin.mapper.ExmRegistrationMapper;
import com.uav.admin.mapper.StuPilotProfileMapper;
import com.uav.admin.security.SecurityUtils;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 报名服务：Redis Lua 原子名额扣减 + 数据库唯一键双保险
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final ExmRegistrationMapper registrationMapper;
    private final ExmExamSessionMapper sessionMapper;
    private final ExmBatchMapper batchMapper;
    private final StuPilotProfileMapper profileMapper;
    private final RedisUtil redisUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ExamSessionService examSessionService;

    private DefaultRedisScript<Long> capacityLua;

    @PostConstruct
    public void init() {
        capacityLua = new DefaultRedisScript<>();
        capacityLua.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/capacity-decr.lua")));
        capacityLua.setResultType(Long.class);
    }

    public PageResult<ExmRegistration> page(long page, long size, Long sessionId, String status, String keyword) {
        LambdaQueryWrapper<ExmRegistration> wrapper = new LambdaQueryWrapper<>();
        if (sessionId != null) {
            wrapper.eq(ExmRegistration::getSessionId, sessionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExmRegistration::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.eq(ExmRegistration::getRegistrationNo, keyword));
        }
        wrapper.orderByDesc(ExmRegistration::getApplyTime);
        Page<ExmRegistration> p = registrationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 考生报名：Lua 原子扣减名额，成功后才落库
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(RegistrationCreateDTO dto) {
        if (dto.getSessionId() == null || dto.getStudentProfileId() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "场次与考生档案不能为空");
        }
        ExmExamSession session = sessionMapper.selectById(dto.getSessionId());
        if (session == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考试场次不存在");
        }
        if (!"PUBLISHED".equals(session.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "该场次未开放报名");
        }
        StuPilotProfile profile = profileMapper.selectById(dto.getStudentProfileId());
        if (profile == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考生档案不存在");
        }

        // 数据库唯一键双保险：一人一场次一单
        Long exists = registrationMapper.selectCount(new LambdaQueryWrapper<ExmRegistration>()
                .eq(ExmRegistration::getSessionId, dto.getSessionId())
                .eq(ExmRegistration::getStudentProfileId, dto.getStudentProfileId())
                .in(ExmRegistration::getStatus, List.of("PENDING", "APPROVED", "SCHEDULED", "COMPLETED")));
        if (exists != null && exists > 0) {
            throw new BaseException(ErrorCode.ALREADY_SIGNED);
        }

        // Redis Lua 原子扣减名额
        String key = examSessionService.capacityKey(session.getId());
        Long remain = stringRedisTemplate.execute(capacityLua, Collections.singletonList(key));
        if (remain == null || remain < 0) {
            throw new BaseException(ErrorCode.CAPACITY_FULL, "名额已满，报名失败");
        }

        // 落库
        ExmRegistration reg = new ExmRegistration();
        reg.setRegistrationNo(ExamPlanService.genCode("REG"));
        reg.setSessionId(dto.getSessionId());
        reg.setStudentUserId(profile.getUserId());
        reg.setStudentProfileId(profile.getId());
        reg.setInstitutionId(dto.getInstitutionId() != null ? dto.getInstitutionId() : profile.getInstitutionId());
        reg.setApplyTime(LocalDateTime.now());
        reg.setStatus("PENDING");
        try {
            registrationMapper.insert(reg);
        } catch (Exception e) {
            // 唯一键冲突或落库失败：回补名额
            redisUtil.increment(key);
            throw new BaseException(ErrorCode.ALREADY_SIGNED, "报名失败：" + e.getMessage());
        }

        // 场次已报名数 +1
        ExmExamSession upd = new ExmExamSession();
        upd.setId(session.getId());
        upd.setEnrolledCount((session.getEnrolledCount() == null ? 0 : session.getEnrolledCount()) + 1);
        sessionMapper.updateById(upd);
    }

    /**
     * 报名审核通过：PENDING → APPROVED
     */
    public void approve(Long id) {
        ExmRegistration reg = getAndCheck(id);
        if (!"PENDING".equals(reg.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅待审核状态可审核通过");
        }
        ExmRegistration upd = new ExmRegistration();
        upd.setId(id);
        upd.setStatus("APPROVED");
        upd.setApproveTime(LocalDateTime.now());
        upd.setApproverId(SecurityUtils.getUserId());
        registrationMapper.updateById(upd);
    }

    /**
     * 报名驳回：PENDING → REJECTED（回补名额）
     */
    public void reject(Long id, String reason) {
        ExmRegistration reg = getAndCheck(id);
        if (!"PENDING".equals(reg.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅待审核状态可驳回");
        }
        ExmRegistration upd = new ExmRegistration();
        upd.setId(id);
        upd.setStatus("REJECTED");
        upd.setRejectReason(reason);
        upd.setApproverId(SecurityUtils.getUserId());
        registrationMapper.updateById(upd);
        // 回补名额
        redisUtil.increment(examSessionService.capacityKey(reg.getSessionId()));
    }

    /**
     * 考生取消：PENDING → CANCELLED（回补名额）
     */
    public void cancel(Long id) {
        ExmRegistration reg = getAndCheck(id);
        if (!"PENDING".equals(reg.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅待审核状态可取消");
        }
        ExmRegistration upd = new ExmRegistration();
        upd.setId(id);
        upd.setStatus("CANCELLED");
        registrationMapper.updateById(upd);
        redisUtil.increment(examSessionService.capacityKey(reg.getSessionId()));
    }

    /**
     * 编排批次：APPROVED → SCHEDULED
     */
    public void arrange(Long id, Long batchId) {
        ExmRegistration reg = getAndCheck(id);
        if (!"APPROVED".equals(reg.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "仅审核通过的报名可编排");
        }
        ExmBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "批次不存在");
        }
        if (!reg.getSessionId().equals(batch.getSessionId())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "批次不属于该报名场次");
        }
        if (batch.getEnrolledCount() != null && batch.getEnrolledCount() >= batch.getCapacity()) {
            throw new BaseException(ErrorCode.CAPACITY_FULL, "批次已满");
        }
        ExmRegistration upd = new ExmRegistration();
        upd.setId(id);
        upd.setBatchId(batchId);
        upd.setStatus("SCHEDULED");
        registrationMapper.updateById(upd);

        ExmBatch bUpd = new ExmBatch();
        bUpd.setId(batchId);
        bUpd.setEnrolledCount((batch.getEnrolledCount() == null ? 0 : batch.getEnrolledCount()) + 1);
        batchMapper.updateById(bUpd);
    }

    private ExmRegistration getAndCheck(Long id) {
        ExmRegistration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "报名记录不存在");
        }
        return reg;
    }
}
