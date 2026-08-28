package com.uav.admin.mq;

import com.rabbitmq.client.Channel;
import com.uav.admin.common.Constants;
import com.uav.admin.entity.SysNotice;
import com.uav.admin.mapper.SysNoticeMapper;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 考试结果通知消费者：成绩审核通过 → 站内通知考生
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamResultConsumer {

    private static final String IDEMPOTENT_PREFIX = "uav:mq:idempotent:exam:";

    private final SysNoticeMapper noticeMapper;
    private final RedisUtil redisUtil;
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @RabbitListener(queues = Constants.EXAM_RESULT_QUEUE)
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            MqMessage mqMsg = (MqMessage) jackson2JsonMessageConverter.fromMessage(message);
            if (mqMsg == null || mqMsg.getPayload() == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // traceId 幂等去重
            String traceKey = IDEMPOTENT_PREFIX + mqMsg.getTraceId();
            if (!redisUtil.setIfAbsent(traceKey, "1", Duration.ofHours(24))) {
                log.info("重复消息已忽略: {}", mqMsg.getTraceId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            Map<String, Object> p = mqMsg.getPayload();
            Object studentUserId = p.get("studentUserId");
            Object passStatus = p.get("passStatus");
            Object score = p.get("score");
            Object examType = p.get("examType");

            SysNotice notice = new SysNotice();
            notice.setTitle("考试成绩通知");
            notice.setContent("您的" + examType + "考试成绩为 " + score + " 分，判定结果："
                    + ("PASS".equals(passStatus) ? "合格" : "不合格")
                    + "。成绩已由管理机构审核确认。");
            notice.setNoticeType("NOTICE");
            notice.setStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
            notice.setTargetRole(studentUserId == null ? "STUDENT" : "STUDENT:" + studentUserId);
            noticeMapper.insert(notice);

            log.info("考试结果通知已发送: traceId={}, pass={}", mqMsg.getTraceId(), passStatus);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("考试结果通知消费失败: {}", body, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
