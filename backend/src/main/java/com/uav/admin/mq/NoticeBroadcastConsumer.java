package com.uav.admin.mq;

import com.rabbitmq.client.Channel;
import com.uav.admin.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 公告广播消费者：fanout 广播接收（可扩展 WebSocket 实时推送前端）
 */
@Slf4j
@Component
public class NoticeBroadcastConsumer {

    @RabbitListener(queues = Constants.NOTICE_BROADCAST_QUEUE)
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("[公告广播] 收到公告消息: {}", body);
            // 预留：WebSocket 推送、站内信落库等扩展点
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("公告广播消费失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
