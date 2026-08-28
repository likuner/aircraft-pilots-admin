package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.OrgInstitution;
import com.uav.admin.mapper.OrgInstitutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 训练机构服务
 */
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final OrgInstitutionMapper institutionMapper;

    public PageResult<OrgInstitution> page(long page, long size, String keyword, String qualificationStatus) {
        LambdaQueryWrapper<OrgInstitution> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OrgInstitution::getInstName, keyword)
                    .or().like(OrgInstitution::getInstCode, keyword)
                    .or().like(OrgInstitution::getCreditCode, keyword));
        }
        if (StringUtils.hasText(qualificationStatus)) {
            wrapper.eq(OrgInstitution::getQualificationStatus, qualificationStatus);
        }
        wrapper.orderByDesc(OrgInstitution::getCreateTime);
        Page<OrgInstitution> p = institutionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public OrgInstitution detail(Long id) {
        OrgInstitution inst = institutionMapper.selectById(id);
        if (inst == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "机构不存在");
        }
        return inst;
    }

    public void create(OrgInstitution institution) {
        if (!StringUtils.hasText(institution.getInstName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "机构名称不能为空");
        }
        if (!StringUtils.hasText(institution.getInstCode())) {
            institution.setInstCode("INST" + ExamPlanService.genCode(""));
        }
        if (institution.getQualificationStatus() == null) {
            institution.setQualificationStatus("NONE");
        }
        if (institution.getStatus() == null) {
            institution.setStatus(1);
        }
        institutionMapper.insert(institution);
    }

    public void update(OrgInstitution institution) {
        OrgInstitution db = institutionMapper.selectById(institution.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "机构不存在");
        }
        institutionMapper.updateById(institution);
    }

    public void delete(Long id) {
        institutionMapper.deleteById(id);
    }
}
