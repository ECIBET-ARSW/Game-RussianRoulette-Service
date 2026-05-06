package com.ecibet.russianroulette.messaging;

import com.ecibet.russianroulette.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDebit(String userId, BigDecimal amount, String referenceId, String description) {
        GameWalletEvent event = GameWalletEvent.builder()
                .userId(userId)
                .amount(amount)
                .referenceId(referenceId)
                .description(description)
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.GAME_EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_DEBIT, event);
        log.info("Published debit event for user {} amount {}", userId, amount);
    }

    public void publishCredit(String userId, BigDecimal amount, String referenceId, String description) {
        GameWalletEvent event = GameWalletEvent.builder()
                .userId(userId)
                .amount(amount)
                .referenceId(referenceId)
                .description(description)
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.GAME_EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_CREDIT, event);
        log.info("Published credit event for user {} amount {}", userId, amount);
    }
}
