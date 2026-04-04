package com.ecibet.russianroulette.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccuseRequest {
    @NotBlank
    private String userId;  // quien acusa
}
