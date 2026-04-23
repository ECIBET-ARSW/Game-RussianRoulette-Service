package com.ecibet.russianroulette.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameWalletEvent {
    private String userId;
    private BigDecimal amount;
    private String referenceId;
    private String description;
}
