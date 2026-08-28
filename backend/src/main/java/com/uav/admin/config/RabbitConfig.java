package com.uav.admin.config;

import com.uav.admin.common.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：交换机 / 队列 / 绑定 / 死信
 */
@Configuration
public class RabbitConfig {

    /**
     * JSON 消息转换器：生产者/消费者统一使用（RabbitTemplate 自动装配）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ---------- 交换机 ----------
    @Bean
    public DirectExchange uavDirectExchange() {
        return new DirectExchange(Constants.DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange uavFanoutExchange() {
        return new FanoutExchange(Constants.FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange uavDlxExchange() {
        return new DirectExchange(Constants.DLX_EXCHANGE, true, false);
    }

    // ---------- 队列 ----------
    @Bean
    public Queue examResultQueue() {
        return QueueBuilder.durable(Constants.EXAM_RESULT_QUEUE)
                .deadLetterExchange(Constants.DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .build();
    }

    @Bean
    public Queue certIssueQueue() {
        return QueueBuilder.durable(Constants.CERT_ISSUE_QUEUE)
                .deadLetterExchange(Constants.DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .build();
    }

    @Bean
    public Queue noticeBroadcastQueue() {
        return QueueBuilder.durable(Constants.NOTICE_BROADCAST_QUEUE).build();
    }

    @Bean
    public Queue instNotifyQueue() {
        return QueueBuilder.durable(Constants.INST_NOTIFY_QUEUE)
                .deadLetterExchange(Constants.DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx")
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(Constants.DLX_QUEUE).build();
    }

    // ---------- 绑定 ----------
    @Bean
    public Binding examResultBinding() {
        return BindingBuilder.bind(examResultQueue()).to(uavDirectExchange())
                .with(Constants.EXAM_RESULT_ROUTING);
    }

    @Bean
    public Binding certIssueBinding() {
        return BindingBuilder.bind(certIssueQueue()).to(uavDirectExchange())
                .with(Constants.CERT_ISSUE_ROUTING);
    }

    @Bean
    public Binding noticeBroadcastBinding() {
        return BindingBuilder.bind(noticeBroadcastQueue()).to(uavFanoutExchange());
    }

    @Bean
    public Binding instNotifyBinding() {
        return BindingBuilder.bind(instNotifyQueue()).to(uavDirectExchange())
                .with(Constants.INST_NOTIFY_ROUTING);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(uavDlxExchange()).with("dlx");
    }
}
