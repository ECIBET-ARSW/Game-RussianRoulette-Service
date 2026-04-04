package com.ecibet.russianroulette.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinRoomRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String username;
}
