package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Message;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.MessageRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private UUID matchId;
    private UUID aliceId;
    private UUID bobId;
    private Match match;
    private Profile alice;
    private Profile bob;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        aliceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        bobId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        match = new Match();
        match.setId(matchId);
        match.setUser1Id(aliceId);
        match.setUser2Id(bobId);
        match.setIsActive(true);

        alice = new Profile();
        alice.setId(aliceId);
        alice.setName("Alice");
        alice.setFcmToken("alice_token");

        bob = new Profile();
        bob.setId(bobId);
        bob.setName("Bob");
        bob.setFcmToken("bob_token");
    }

    // ========== sendMessage 测试 ==========

    @Test
    @DisplayName("sendMessage - 正常发送消息")
    void sendMessage_success() {
        Message savedMsg = new Message();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setMatchId(matchId);
        savedMsg.setSenderId(aliceId);
        savedMsg.setContent("Hello Bob!");
        savedMsg.setCreatedAt(OffsetDateTime.now());
        savedMsg.setIsRead(false);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(profileRepository.findById(bobId)).thenReturn(Optional.of(bob));

        Message result = messageService.sendMessage(matchId, aliceId.toString(), "Hello Bob!");

        assertNotNull(result);
        assertEquals("Hello Bob!", result.getContent());
        assertEquals(aliceId, result.getSenderId());
        assertEquals(matchId, result.getMatchId());

        // 验证保存了消息
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertEquals("Hello Bob!", captor.getValue().getContent());

        // 验证通过WebSocket推送
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + matchId), any(Object.class));

        // 验证发送FCM推送给Bob
        verify(pushNotificationService).sendNewMessageNotification(
                eq("bob_token"), eq("Alice"), eq("Hello Bob!"), eq(matchId.toString()));
    }

    @Test
    @DisplayName("sendMessage - match不存在抛RuntimeException")
    void sendMessage_matchNotFound_throwsException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(matchId, aliceId.toString(), "Hello"));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage - 发送者不是match参与者抛异常")
    void sendMessage_notParticipant_throwsException() {
        UUID strangerId = UUID.randomUUID();
        Profile stranger = new Profile();
        stranger.setId(strangerId);
        stranger.setName("Stranger");

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(strangerId)).thenReturn(Optional.of(stranger));

        assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(matchId, strangerId.toString(), "Hello"));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage - 长消息预览截断为50字符")
    void sendMessage_longMessage_previewTruncated() {
        String longContent = "A".repeat(100);
        Message savedMsg = new Message();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setMatchId(matchId);
        savedMsg.setSenderId(aliceId);
        savedMsg.setContent(longContent);
        savedMsg.setCreatedAt(OffsetDateTime.now());
        savedMsg.setIsRead(false);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);
        when(profileRepository.findById(bobId)).thenReturn(Optional.of(bob));

        messageService.sendMessage(matchId, aliceId.toString(), longContent);

        // 验证FCM预览被截断
        ArgumentCaptor<String> previewCaptor = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService).sendNewMessageNotification(
                any(), any(), previewCaptor.capture(), any());
        // 预览应为50字符 + "..."
        assertTrue(previewCaptor.getValue().length() <= 52);
    }

    // ========== getMessages 测试 ==========

    @Test
    @DisplayName("getMessages - 分页返回消息列表")
    void getMessages_returnsPagedMessages() {
        Pageable pageable = PageRequest.of(0, 20);

        Message msg1 = new Message();
        msg1.setId(UUID.randomUUID());
        msg1.setMatchId(matchId);
        msg1.setSenderId(aliceId);
        msg1.setContent("Hi!");
        msg1.setCreatedAt(OffsetDateTime.now().minusMinutes(5));

        Message msg2 = new Message();
        msg2.setId(UUID.randomUUID());
        msg2.setMatchId(matchId);
        msg2.setSenderId(bobId);
        msg2.setContent("Hello!");
        msg2.setCreatedAt(OffsetDateTime.now());

        Page<Message> page = new PageImpl<>(List.of(msg2, msg1), pageable, 2);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(messageRepository.findByMatchIdOrderByCreatedAtDesc(matchId, pageable)).thenReturn(page);

        Page<Message> result = messageService.getMessages(matchId, aliceId.toString(), pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Hello!", result.getContent().get(0).getContent());
        assertEquals("Hi!", result.getContent().get(1).getContent());
    }

    @Test
    @DisplayName("getMessages - match不存在抛RuntimeException")
    void getMessages_matchNotFound_throwsException() {
        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> messageService.getMessages(matchId, aliceId.toString(), pageable));
    }

    @Test
    @DisplayName("getMessages - 非参与者无法查看消息")
    void getMessages_notParticipant_throwsException() {
        UUID strangerId = UUID.randomUUID();
        Profile stranger = new Profile();
        stranger.setId(strangerId);
        stranger.setName("Stranger");

        Pageable pageable = PageRequest.of(0, 20);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(strangerId)).thenReturn(Optional.of(stranger));

        assertThrows(RuntimeException.class,
                () -> messageService.getMessages(matchId, strangerId.toString(), pageable));
    }
}
