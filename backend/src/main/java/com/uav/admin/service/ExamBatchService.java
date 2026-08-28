package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.ExmBatch;
import com.uav.admin.mapper.ExmBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 批次（考场编排单元）服务
 */
@Service
@RequiredArgsConstructor
public class ExamBatchService {

    private final ExmBatchMapper batchMapper;

    public PageResult<ExmBatch> page(long page, long size, Long sessionId, String status) {
        LambdaQueryWrapper<ExmBatch> wrapper = new LambdaQueryWrapper<>();
        if (sessionId != null) {
            wrapper.eq(ExmBatch::getSessionId, sessionId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExmBatch::getStatus, status);
        }
        wrapper.orderByAsc(ExmBatch::getBatchTime);
        Page<ExmBatch> p = batchMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<ExmBatch> listBySession(Long sessionId) {
        return batchMapper.selectList(new LambdaQueryWrapper<ExmBatch>()
                .eq(ExmBatch::getSessionId, sessionId)
                .orderByAsc(ExmBatch::getBatchTime));
    }

    public void create(ExmBatch batch) {
        if (batch.getSessionId() == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "必须指定所属场次");
        }
        if (batch.getCapacity() == null || batch.getCapacity() <= 0) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "批次容量必须大于 0");
        }
        if (!StringUtils.hasText(batch.getBatchCode())) {
            batch.setBatchCode("B" + ExamPlanService.genCode(""));
        }
        batch.setStatus("PLANNED");
        batch.setEnrolledCount(0);
        batchMapper.insert(batch);
    }

    public void update(ExmBatch batch) {
        ExmBatch db = batchMapper.selectById(batch.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "批次不存在");
        }
        batchMapper.updateById(batch);
    }

    public void delete(Long id) {
        ExmBatch db = batchMapper.selectById(id);
        if (db == null) {
            return;
        }
        if (db.getEnrolledCount() != null && db.getEnrolledCount() > 0) {
            throw new BaseException(ErrorCode.STATE_ERROR, "批次已有编排考生，不可删除");
        }
        batchMapper.deleteById(id);
    }
}
