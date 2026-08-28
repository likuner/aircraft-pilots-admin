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
 * 机构消息通知消费者：认证流程关键节点 → 站内通知机构管理员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstNotifyConsumer {

    private static final String IDEMPOTENT_PREFIX = "uav:mq:idempotent:inst:";

    private final SysNoticeMapper noticeMapper;
    private final RedisUtil redisUtil;
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @RabbitListener(queues = Constants.INST_NOTIFY_QUEUE)
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            MqMessage mqMsg = (MqMessage) jackson2JsonMessageConverter.fromMessage(message);
            if (mqMsg == null || mqMsg.getPayload() == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            String traceKey = IDEMPOTENT_PREFIX + mqMsg.getTraceId();
            if (!redisUtil.setIfAbsent(traceKey, "1", Duration.ofHours(24))) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            Map<String, Object> p = mqMsg.getPayload();
            SysNotice notice = new SysNotice();
            notice.setTitle("机构认证进度通知");
            notice.setContent(String.valueOf(p.getOrDefault("content", "您的机构认证流程有新的进展，请及时关注。")));
            notice.setNoticeType("NOTICE");
            notice.setStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
            notice.setTargetRole("INSTITUTION_ADMIN");
            noticeMapper.insert(notice);
            log.info("机构通知已发送: applyId={}", p.get("applyId"));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("机构通知消费失败: {}", body, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
