package com.uav.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 首页统计 Mapper
 */
@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM exm_registration WHERE deleted = 0")
    long countRegistrations();

    @Select("SELECT COUNT(*) FROM exm_score WHERE status = 'SUBMITTED' AND deleted = 0")
    long countPendingScores();

    @Select("SELECT COUNT(*) FROM cer_certificate_apply WHERE status IN ('SUBMITTED','PENDING_AUDIT') AND deleted = 0")
    long countPendingCertApplies();

    @Select("SELECT COUNT(*) FROM org_site_inspection WHERE status IN ('PENDING','ASSIGNED')")
    long countPendingInspections();

    @Select("SELECT COUNT(*) FROM exm_score WHERE pass_status = 'PASS' AND deleted = 0")
    long countPassScores();

    @Select("SELECT COUNT(*) FROM exm_score WHERE status = 'APPROVED' AND deleted = 0")
    long countApprovedScores();

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = 1")
    long countUsers();

    @Select("SELECT COUNT(*) FROM org_institution WHERE deleted = 0")
    long countInstitutions();

    @Select("SELECT COUNT(*) FROM cer_certificate WHERE status = 'VALID' AND deleted = 0")
    long countValidCerts();

    @Select("SELECT status, COUNT(*) AS cnt FROM exm_registration WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> registrationStatusStats();

    @Select("SELECT pass_status, COUNT(*) AS cnt FROM exm_score WHERE status='APPROVED' AND deleted = 0 GROUP BY pass_status")
    List<Map<String, Object>> passStatusStats();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS cnt FROM exm_registration " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH) AND deleted = 0 GROUP BY month ORDER BY month")
    List<Map<String, Object>> registrationTrend();
}
