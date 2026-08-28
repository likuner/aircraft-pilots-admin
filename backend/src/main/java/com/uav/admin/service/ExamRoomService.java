package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.ExmExamRoom;
import com.uav.admin.mapper.ExmExamRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 考场服务
 */
@Service
@RequiredArgsConstructor
public class ExamRoomService {

    private final ExmExamRoomMapper roomMapper;

    public PageResult<ExmExamRoom> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<ExmExamRoom> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ExmExamRoom::getRoomName, keyword)
                    .or().like(ExmExamRoom::getRoomCode, keyword));
        }
        if (status != null) {
            wrapper.eq(ExmExamRoom::getStatus, status);
        }
        wrapper.orderByDesc(ExmExamRoom::getCreateTime);
        Page<ExmExamRoom> p = roomMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public void create(ExmExamRoom room) {
        if (!StringUtils.hasText(room.getRoomName())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "考场名称不能为空");
        }
        if (!StringUtils.hasText(room.getRoomCode())) {
            room.setRoomCode("RM" + ExamPlanService.genCode(""));
        }
        if (room.getStatus() == null) {
            room.setStatus(1);
        }
        roomMapper.insert(room);
    }

    public void update(ExmExamRoom room) {
        ExmExamRoom db = roomMapper.selectById(room.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "考场不存在");
        }
        roomMapper.updateById(room);
    }

    public void delete(Long id) {
        roomMapper.deleteById(id);
    }
}
