package com.uav.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uav.admin.common.BaseException;
import com.uav.admin.common.Constants;
import com.uav.admin.common.ErrorCode;
import com.uav.admin.common.PageResult;
import com.uav.admin.entity.SysNotice;
import com.uav.admin.mapper.SysNoticeMapper;
import com.uav.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 公告通知服务
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final SysNoticeMapper noticeMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 分页查询公告
     */
    public PageResult<SysNotice> page(long page, long size, String keyword, String status, String noticeType) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysNotice::getTitle, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        if (StringUtils.hasText(noticeType)) {
            wrapper.eq(SysNotice::getNoticeType, noticeType);
        }
        wrapper.orderByDesc(SysNotice::getCreateTime);
        Page<SysNotice> p = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /**
     * 新增公告
     */
    public void create(SysNotice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "公告标题不能为空");
        }
        if (notice.getStatus() == null) {
            notice.setStatus("DRAFT");
        }
        notice.setPublisherId(SecurityUtils.getUserId());
        noticeMapper.insert(notice);
    }

    /**
     * 更新公告
     */
    public void update(SysNotice notice) {
        SysNotice db = noticeMapper.selectById(notice.getId());
        if (db == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "公告不存在");
        }
        // 已发布的公告不允许直接修改（先撤下）
        if ("PUBLISHED".equals(db.getStatus())) {
            throw new BaseException(ErrorCode.STATE_ERROR, "公告已发布，请先撤下再编辑");
        }
        noticeMapper.updateById(notice);
    }

    /**
     * 删除公告
     */
    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }

    /**
     * 发布公告：状态流转 + RabbitMQ fanout 广播
     */
    public void publish(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "公告不存在");
        }
        if ("PUBLISHED".equals(notice.getStatus())) {
            return;
        }
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setStatus("PUBLISHED");
        update.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(update);

        // 异步广播：可扩展为 WebSocket 推送前端
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notice.getId());
        payload.put("title", notice.getTitle());
        payload.put("noticeType", notice.getNoticeType());
        payload.put("targetRole", notice.getTargetRole());
        payload.put("publishTime", LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(Constants.FANOUT_EXCHANGE, "", payload);
    }

    /**
     * 撤下公告
     */
    public void unpublish(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BaseException(ErrorCode.DATA_NOT_FOUND, "公告不存在");
        }
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setStatus("DRAFT");
        noticeMapper.updateById(update);
    }

    /**
     * 首页最新公告
     */
    public PageResult<SysNotice> latest(int limit) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getStatus, "PUBLISHED");
        wrapper.orderByDesc(SysNotice::getPublishTime);
        Page<SysNotice> p = noticeMapper.selectPage(new Page<>(1, limit), wrapper);
        return PageResult.of(p.getTotal(), p.getRecords());
    }
}
