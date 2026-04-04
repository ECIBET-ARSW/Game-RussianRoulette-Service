package com.ecibet.russianroulette.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRoomRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String username;
    @NotBlank
    private String roomName;
    @NotNull @Positive
    private BigDecimal buyIn;
}
