package com.chunshuiquan.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class SwipeRequest {

    @NotNull
    private UUID swipedId;

    @NotBlank
    @Pattern(regexp = "like|nope|superlike")
    private String direction;
}
