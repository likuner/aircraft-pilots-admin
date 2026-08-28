package com.uav.admin.service;

import com.uav.admin.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页统计服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    /**
     * 首页统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("registrationCount", dashboardMapper.countRegistrations());
        data.put("pendingScoreCount", dashboardMapper.countPendingScores());
        data.put("pendingCertApplyCount", dashboardMapper.countPendingCertApplies());
        data.put("pendingInspectionCount", dashboardMapper.countPendingInspections());
        data.put("passScoreCount", dashboardMapper.countPassScores());
        data.put("approvedScoreCount", dashboardMapper.countApprovedScores());
        data.put("userCount", dashboardMapper.countUsers());
        data.put("institutionCount", dashboardMapper.countInstitutions());
        data.put("validCertCount", dashboardMapper.countValidCerts());
        data.put("registrationStatusStats", dashboardMapper.registrationStatusStats());
        data.put("passStatusStats", dashboardMapper.passStatusStats());
        data.put("registrationTrend", dashboardMapper.registrationTrend());
        return data;
    }
}
