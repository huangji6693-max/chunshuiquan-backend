package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.SwipeRequest;
import com.chunshuiquan.backend.dto.SwipeResult;
import com.chunshuiquan.backend.service.SwipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/swipe")
public class SwipeController {

    private final SwipeService swipeService;

    public SwipeController(SwipeService swipeService) {
        this.swipeService = swipeService;
    }

    @PostMapping
    public ResponseEntity<SwipeResult> swipe(@AuthenticationPrincipal String userId,
                                             @Valid @RequestBody SwipeRequest req) {
        SwipeResult result = swipeService.swipe(
                UUID.fromString(userId), req.getSwipedId(), req.getDirection());
        return ResponseEntity.ok(result);
    }
}
