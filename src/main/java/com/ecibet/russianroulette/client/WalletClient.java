package com.ecibet.russianroulette.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class WalletClient {

    private final WebClient webClient;

    public WalletClient(@Value("${ecibet.wallets-service.url}") String walletsUrl) {
        this.webClient = WebClient.builder().baseUrl(walletsUrl).build();
    }

    public boolean debit(String userId, BigDecimal amount, String description) {
        try {
            webClient.post()
                    .uri("/api/v1/transactions/debit/{userId}", userId)
                    .bodyValue(Map.of("amount", amount, "description", description))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.error("Failed to debit wallet for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public boolean credit(String userId, BigDecimal amount, String description) {
        try {
            webClient.post()
                    .uri("/api/v1/transactions/deposit/{userId}", userId)
                    .bodyValue(Map.of("amount", amount, "description", description))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.error("Failed to credit wallet for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
