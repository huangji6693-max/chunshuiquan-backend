package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.CheckInResultDto;
import com.chunshuiquan.backend.entity.DailyCheckIn;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.DailyCheckInRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private DailyCheckInRepository checkInRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CoinService coinService;

    @InjectMocks
    private CheckInService checkInService;

    private UUID userId;
    private Profile profile;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = new Profile();
        profile.setId(userId);
        profile.setCoins(100);
        today = LocalDate.now();
    }

    // ========== getStatus 测试 ==========

    @Test
    @DisplayName("getStatus - 未签到返回checkedInToday=false")
    void getStatus_notCheckedIn_returnsFalse() {
        when(checkInRepository.findByUserIdAndCheckDate(userId, today)).thenReturn(Optional.empty());
        // calculateStreak需要查询昨天
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.empty());
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        // getWeekStatus查询
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(Collections.emptyList());

        CheckInResultDto status = checkInService.getStatus(userId);

        assertFalse(status.getCheckedInToday());
        assertEquals(0, status.getStreakDays());
        assertEquals(100, status.getTotalCoins());
    }

    @Test
    @DisplayName("getStatus - 已签到返回checkedInToday=true")
    void getStatus_alreadyCheckedIn_returnsTrue() {
        DailyCheckIn todayCheckIn = new DailyCheckIn();
        todayCheckIn.setUserId(userId);
        todayCheckIn.setCheckDate(today);
        todayCheckIn.setReward(10);
        todayCheckIn.setStreakDay(1);

        when(checkInRepository.findByUserIdAndCheckDate(userId, today))
                .thenReturn(Optional.of(todayCheckIn));
        // calculateStreak(includesToday=true)需要查昨天
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.empty());
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(List.of(todayCheckIn));

        CheckInResultDto status = checkInService.getStatus(userId);

        assertTrue(status.getCheckedInToday());
        assertEquals(1, status.getStreakDays());
    }

    // ========== checkIn 测试 ==========

    @Test
    @DisplayName("checkIn - 首次签到成功，获得10金币")
    void checkIn_firstTime_earns10Coins() {
        // 今天没签过
        when(checkInRepository.findByUserIdAndCheckDate(userId, today)).thenReturn(Optional.empty());
        // 昨天也没签过（streak=0 -> 签到后streak=1, reward=STREAK_REWARDS[0]=10）
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.empty());
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(checkInRepository.save(any(DailyCheckIn.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(Collections.emptyList());

        CheckInResultDto result = checkInService.checkIn(userId);

        assertTrue(result.getCheckedInToday());
        assertEquals(1, result.getStreakDays());
        assertEquals(10, result.getTodayReward());
        // 金币从100变成110
        assertEquals(110, result.getTotalCoins());

        // 验证保存了签到记录
        ArgumentCaptor<DailyCheckIn> captor = ArgumentCaptor.forClass(DailyCheckIn.class);
        verify(checkInRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getReward());
        assertEquals(1, captor.getValue().getStreakDay());
    }

    @Test
    @DisplayName("checkIn - 连续签到第2天获得15金币")
    void checkIn_secondConsecutiveDay_earns15Coins() {
        // 昨天签过（连续1天）
        DailyCheckIn yesterdayCheckIn = new DailyCheckIn();
        yesterdayCheckIn.setUserId(userId);
        yesterdayCheckIn.setCheckDate(today.minusDays(1));
        yesterdayCheckIn.setStreakDay(1);

        when(checkInRepository.findByUserIdAndCheckDate(userId, today)).thenReturn(Optional.empty());
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.of(yesterdayCheckIn));
        // 前天没签过
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(2))))
                .thenReturn(Optional.empty());
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(checkInRepository.save(any(DailyCheckIn.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(Collections.emptyList());

        CheckInResultDto result = checkInService.checkIn(userId);

        assertEquals(2, result.getStreakDays());
        assertEquals(15, result.getTodayReward());
        assertEquals(115, result.getTotalCoins());
    }

    @Test
    @DisplayName("checkIn - 重复签到抛IllegalStateException")
    void checkIn_duplicate_throwsException() {
        DailyCheckIn existing = new DailyCheckIn();
        existing.setUserId(userId);
        existing.setCheckDate(today);
        when(checkInRepository.findByUserIdAndCheckDate(userId, today))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> checkInService.checkIn(userId));

        // 不应保存任何记录
        verify(checkInRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkIn - 签到后记录金币流水")
    void checkIn_recordsCoinTransaction() {
        when(checkInRepository.findByUserIdAndCheckDate(userId, today)).thenReturn(Optional.empty());
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.empty());
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(checkInRepository.save(any(DailyCheckIn.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(Collections.emptyList());

        checkInService.checkIn(userId);

        verify(coinService).recordSpend(eq(userId), eq(-10), eq(110),
                eq("daily_bonus"), contains("连续签到第1天"));
    }

    @Test
    @DisplayName("getStatus - 周状态正确返回7个布尔值")
    void getStatus_weekStatus_returns7Booleans() {
        when(checkInRepository.findByUserIdAndCheckDate(userId, today)).thenReturn(Optional.empty());
        when(checkInRepository.findByUserIdAndCheckDate(eq(userId), eq(today.minusDays(1))))
                .thenReturn(Optional.empty());
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        // 模拟周一和周三签过
        DailyCheckIn mondayCheckIn = new DailyCheckIn();
        mondayCheckIn.setCheckDate(monday);
        DailyCheckIn wednesdayCheckIn = new DailyCheckIn();
        wednesdayCheckIn.setCheckDate(monday.plusDays(2));

        when(checkInRepository.findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
                userId, monday, sunday)).thenReturn(List.of(mondayCheckIn, wednesdayCheckIn));

        CheckInResultDto status = checkInService.getStatus(userId);

        List<Boolean> weekStatus = status.getWeekStatus();
        assertNotNull(weekStatus);
        assertEquals(7, weekStatus.size());
        assertTrue(weekStatus.get(0));   // 周一 true
        assertFalse(weekStatus.get(1));  // 周二 false
        assertTrue(weekStatus.get(2));   // 周三 true
        assertFalse(weekStatus.get(3));  // 周四 false
        assertFalse(weekStatus.get(4));  // 周五 false
        assertFalse(weekStatus.get(5));  // 周六 false
        assertFalse(weekStatus.get(6));  // 周日 false
    }
}
