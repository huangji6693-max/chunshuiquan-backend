package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.BlockedUser;
import com.chunshuiquan.backend.repository.BlockedUserRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockedUserRepository blockedUserRepository;
    private final ProfileRepository profileRepository;

    public BlockController(BlockedUserRepository blockedUserRepository,
                           ProfileRepository profileRepository) {
        this.blockedUserRepository = blockedUserRepository;
        this.profileRepository = profileRepository;
    }

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> block(@AuthenticationPrincipal String userId,
                                      @PathVariable UUID targetId) {
        UUID myId = UUID.fromString(userId);
        if (!profileRepository.existsById(targetId)) {
            return ResponseEntity.notFound().build();
        }
        if (!blockedUserRepository.existsByBlockerIdAndBlockedId(myId, targetId)) {
            BlockedUser block = new BlockedUser();
            block.setBlockerId(myId);
            block.setBlockedId(targetId);
            blockedUserRepository.save(block);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetId}")
    @Transactional
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal String userId,
                                        @PathVariable UUID targetId) {
        UUID myId = UUID.fromString(userId);
        blockedUserRepository.deleteByBlockerIdAndBlockedId(myId, targetId);
        return ResponseEntity.noContent().build();
    }
}
