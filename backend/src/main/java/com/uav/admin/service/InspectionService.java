package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.OrgSiteInspection;
import com.uav.admin.mapper.OrgSiteInspectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 实地核查任务服务
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final OrgSiteInspectionMapper inspectionMapper;

    public PageResult<OrgSiteInspection> page(long page, long size, Long applyId, Long inspectorId, String status) {
        LambdaQueryWrapper<OrgSiteInspection> wrapper = new LambdaQueryWrapper<>();
        if (applyId != null) {
            wrapper.eq(OrgSiteInspection::getApplyId, applyId);
        }
        if (inspectorId != null) {
            wrapper.eq(OrgSiteInspection::getInspectorId, inspectorId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrgSiteInspection::getStatus, status);
        }
        wrapper.orderByDesc(OrgSiteInspection::getCreateTime);
        Page<OrgSiteInspection> p = inspectionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public OrgSiteInspection detail(Long id) {
        OrgSiteInspection inspection = inspectionMapper.selectById(id);
        if (inspection == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "核查任务不存在");
        }
        return inspection;
    }
}
