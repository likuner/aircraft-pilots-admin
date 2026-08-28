package com.uav.admin.mq;

import com.rabbitmq.client.Channel;
import com.uav.admin.common.Constants;
import com.uav.admin.service.CertIssueService;
import com.uav.admin.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 证书异步签发消费者：Redis 分布式锁 + traceId 幂等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertIssueConsumer {

    private static final String IDEMPOTENT_PREFIX = "uav:mq:idempotent:cert:";

    private final CertIssueService certIssueService;
    private final RedisUtil redisUtil;
    private final Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @RabbitListener(queues = Constants.CERT_ISSUE_QUEUE)
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            MqMessage mqMsg = (MqMessage) jackson2JsonMessageConverter.fromMessage(message);
            if (mqMsg == null || mqMsg.getPayload() == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // traceId 幂等键（仅在业务成功后标记，避免失败重投被误判为重复而丢失消息）
            String traceKey = IDEMPOTENT_PREFIX + mqMsg.getTraceId();
            Map<String, Object> p = mqMsg.getPayload();
            Long applyId = asLong(p.get("applyId"));
            Long registrationId = asLong(p.get("registrationId"));
            Long scoreId = asLong(p.get("scoreId"));
            Long studentUserId = asLong(p.get("studentUserId"));
            String certificateType = p.get("certificateType") == null ? null : String.valueOf(p.get("certificateType"));

            // 分布式锁防并发重复签发
            String lockKey = "cert:issue:" + applyId;
            if (!redisUtil.tryLock(lockKey, Duration.ofSeconds(30))) {
                log.warn("签发锁占用中，稍后重试 applyId={}", applyId);
                channel.basicNack(deliveryTag, false, true);
                return;
            }
            try {
                boolean issued = certIssueService.issue(applyId, registrationId, scoreId, studentUserId, certificateType);
                if (!issued) {
                    // 幂等跳过视为成功
                    log.info("签发被幂等跳过: applyId={}", applyId);
                }
                // 业务成功后再标记幂等并 ack
                redisUtil.set(traceKey, "1", Duration.ofHours(24));
                channel.basicAck(deliveryTag, false);
            } finally {
                redisUtil.unlock(lockKey);
            }
        } catch (Exception e) {
            log.error("证书签发消费失败: {}", body, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private Long asLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(obj));
    }
}
