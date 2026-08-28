package com.uav.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uav.admin.entity.ExmExamSession;
import com.uav.admin.mapper.ExmExamSessionMapper;
import com.uav.admin.mapper.ExmScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 场次状态流转定时任务：
 * 1. 已发布且已过考试日期且未截止 → ENROLLMENT_CLOSED
 * 2. 报名已截止且已过考试日期 → IN_PROGRESS
 * 3. 全部成绩已审核（无 SUBMITTED/DRAFT）且已过考试日期 → COMPLETED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSessionStatusTask {

    private final ExmExamSessionMapper sessionMapper;
    private final ExmScoreMapper scoreMapper;

    /** 每分钟检查一次 */
    @Scheduled(cron = "0 * * * * ?")
    public void run() {
        LocalDate today = LocalDate.now();
        List<ExmExamSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ExmExamSession>()
                        .in(ExmExamSession::getStatus, "PUBLISHED", "ENROLLMENT_CLOSED", "IN_PROGRESS"));
        for (ExmExamSession session : sessions) {
            boolean past = session.getExamDate() != null && !session.getExamDate().isAfter(today);
            if (!past) {
                continue;
            }
            switch (session.getStatus()) {
                case "PUBLISHED" -> {
                    transition(session.getId(), "ENROLLMENT_CLOSED");
                    log.info("场次 {} 报名自动截止", session.getSessionCode());
                }
                case "ENROLLMENT_CLOSED" -> {
                    transition(session.getId(), "IN_PROGRESS");
                    log.info("场次 {} 开考（自动流转）", session.getSessionCode());
                }
                case "IN_PROGRESS" -> {
                    Long pending = scoreMapper.selectCount(new LambdaQueryWrapper<com.uav.admin.entity.ExmScore>()
                            .eq(com.uav.admin.entity.ExmScore::getSessionId, session.getId())
                            .in(com.uav.admin.entity.ExmScore::getStatus, "DRAFT", "SUBMITTED"));
                    if (pending == null || pending == 0) {
                        transition(session.getId(), "COMPLETED");
                        log.info("场次 {} 成绩已全部审核，自动结束", session.getSessionCode());
                    }
                }
                default -> {
                }
            }
        }
    }

    private void transition(Long id, String status) {
        ExmExamSession upd = new ExmExamSession();
        upd.setId(id);
        upd.setStatus(status);
        sessionMapper.updateById(upd);
    }
}
