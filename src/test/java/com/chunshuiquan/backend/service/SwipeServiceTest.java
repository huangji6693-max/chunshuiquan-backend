package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.SwipeResult;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.Swipe;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.SwipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private SwipeService swipeService;

    private UUID aliceId;
    private UUID bobId;
    private Profile alice;
    private Profile bob;

    @BeforeEach
    void setUp() {
        // alice < bob in UUID comparison for consistent user1/user2 ordering
        aliceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        bobId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        alice = new Profile();
        alice.setId(aliceId);
        alice.setName("Alice");
        alice.setAvatarUrls(new String[]{"https://img.example.com/alice.jpg"});

        bob = new Profile();
        bob.setId(bobId);
        bob.setName("Bob");
        bob.setAvatarUrls(new String[]{"https://img.example.com/bob.jpg"});
    }

    @Test
    @DisplayName("swipe nope - 不检查匹配，直接返回noMatch")
    void swipe_nope_doesNotCheckMatch() {
        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));

        SwipeResult result = swipeService.swipe(aliceId, bobId, "nope");

        assertFalse(result.isMatched());
        assertNull(result.getMatchId());
        // nope方向不应查询对方是否like
        verify(swipeRepository, never()).existsBySwipedIdAndSwiperIdAndDirectionIn(
                any(), any(), any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("swipe like - 对方也like了我，创建Match")
    void swipe_like_mutualLike_createsMatch() {
        UUID matchId = UUID.randomUUID();
        Match match = new Match();
        match.setId(matchId);
        match.setUser1Id(aliceId);
        match.setUser2Id(bobId);

        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));
        // Bob已经like过Alice
        when(swipeRepository.existsBySwipedIdAndSwiperIdAndDirectionIn(
                eq(aliceId), eq(bobId), eq(List.of("like", "superlike")))).thenReturn(true);
        // 尚未有Match记录
        when(matchRepository.findByUser1IdAndUser2Id(aliceId, bobId)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(profileRepository.findById(bobId)).thenReturn(Optional.of(bob));

        SwipeResult result = swipeService.swipe(aliceId, bobId, "like");

        assertTrue(result.isMatched());
        assertEquals(matchId, result.getMatchId());
        assertEquals("Bob", result.getPartnerName());
        assertEquals("https://img.example.com/bob.jpg", result.getPartnerAvatarUrl());
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("swipe like - 重复滑动幂等，返回noMatch")
    void swipe_duplicateSwipe_returnsNoMatch() {
        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(true);

        SwipeResult result = swipeService.swipe(aliceId, bobId, "like");

        assertFalse(result.isMatched());
        // 不应保存新Swipe
        verify(swipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("swipe like - 对方还没like，返回noMatch")
    void swipe_like_noMutualLike_returnsNoMatch() {
        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));
        // Bob没有like过Alice
        when(swipeRepository.existsBySwipedIdAndSwiperIdAndDirectionIn(
                eq(aliceId), eq(bobId), eq(List.of("like", "superlike")))).thenReturn(false);

        SwipeResult result = swipeService.swipe(aliceId, bobId, "like");

        assertFalse(result.isMatched());
        assertNull(result.getMatchId());
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("swipe superlike - 对方也like/superlike了，也能创建Match")
    void swipe_superlike_mutualLike_createsMatch() {
        UUID matchId = UUID.randomUUID();
        Match match = new Match();
        match.setId(matchId);
        match.setUser1Id(aliceId);
        match.setUser2Id(bobId);

        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(swipeRepository.existsBySwipedIdAndSwiperIdAndDirectionIn(
                eq(aliceId), eq(bobId), eq(List.of("like", "superlike")))).thenReturn(true);
        when(matchRepository.findByUser1IdAndUser2Id(aliceId, bobId)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(profileRepository.findById(bobId)).thenReturn(Optional.of(bob));

        SwipeResult result = swipeService.swipe(aliceId, bobId, "superlike");

        assertTrue(result.isMatched());
        assertEquals(matchId, result.getMatchId());
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("swipe like - 匹配时发送推送通知给对方")
    void swipe_match_sendsPushNotification() {
        UUID matchId = UUID.randomUUID();
        Match match = new Match();
        match.setId(matchId);
        match.setUser1Id(aliceId);
        match.setUser2Id(bobId);

        bob.setFcmToken("bob_fcm_token");

        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(swipeRepository.existsBySwipedIdAndSwiperIdAndDirectionIn(
                eq(aliceId), eq(bobId), eq(List.of("like", "superlike")))).thenReturn(true);
        when(matchRepository.findByUser1IdAndUser2Id(aliceId, bobId)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(profileRepository.findById(bobId)).thenReturn(Optional.of(bob));

        swipeService.swipe(aliceId, bobId, "like");

        verify(pushNotificationService).sendNewMatchNotification(
                eq("bob_fcm_token"), eq("Alice"), eq(matchId.toString()));
    }

    @Test
    @DisplayName("swipe - 保存的Swipe记录包含正确字段")
    void swipe_savesSwipeRecord_withCorrectFields() {
        when(swipeRepository.existsBySwiperIdAndSwipedId(aliceId, bobId)).thenReturn(false);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> inv.getArgument(0));

        swipeService.swipe(aliceId, bobId, "nope");

        var captor = org.mockito.ArgumentCaptor.forClass(Swipe.class);
        verify(swipeRepository).save(captor.capture());
        Swipe saved = captor.getValue();
        assertEquals(aliceId, saved.getSwiperId());
        assertEquals(bobId, saved.getSwipedId());
        assertEquals("nope", saved.getDirection());
    }
}
