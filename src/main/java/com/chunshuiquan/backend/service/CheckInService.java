package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.CheckInResultDto;
import com.chunshuiquan.backend.entity.DailyCheckIn;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.DailyCheckInRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class CheckInService {

    /** 连续签到奖励表：第1天10，第2天15，...第7天50 */
    private static final int[] STREAK_REWARDS = {10, 15, 20, 25, 30, 40, 50};

    private final DailyCheckInRepository checkInRepository;
    private final ProfileRepository profileRepository;
    private final CoinService coinService;

    public CheckInService(DailyCheckInRepository checkInRepository,
                          ProfileRepository profileRepository,
                          CoinService coinService) {
        this.checkInRepository = checkInRepository;
        this.profileRepository = profileRepository;
        this.coinService = coinService;
    }

    /** 查询签到状态 */
    public CheckInResultDto getStatus(UUID userId) {
        LocalDate today = LocalDate.now();
        boolean checkedInToday = checkInRepository.findByUserIdAndCheckDate(userId, today).isPresent();

        int streak = calculateStreak(userId, today, checkedInToday);
        int todayReward = STREAK_REWARDS[Math.min(streak, STREAK_REWARDS.length - 1)];

        Profile profile = profileRepository.findById(userId).orElse(null);
        int totalCoins = profile != null ? profile.getCoins() : 0;

        List<Boolean> weekStatus = getWeekStatus(userId, today);

        return new CheckInResultDto(checkedInToday, streak, todayReward, totalCoins, weekStatus);
    }

    /** 执行签到 */
    @Transactional
    public CheckInResultDto checkIn(UUID userId) {
        LocalDate today = LocalDate.now();

        // 防重复签到
        if (checkInRepository.findByUserIdAndCheckDate(userId, today).isPresent()) {
            throw new IllegalStateException("今天已签到");
        }

        int streak = calculateStreak(userId, today, false) + 1;
        int reward = STREAK_REWARDS[Math.min(streak - 1, STREAK_REWARDS.length - 1)];

        // 保存签到记录
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckDate(today);
        checkIn.setReward(reward);
        checkIn.setStreakDay(streak);
        checkInRepository.save(checkIn);

        // 发放金币（悲观锁防并发）
        Profile profile = profileRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        profile.setCoins(profile.getCoins() + reward);
        profileRepository.save(profile);

        // 记录流水
        coinService.recordSpend(userId, -reward, profile.getCoins(), "daily_bonus",
                "连续签到第" + streak + "天 +" + reward + "金币");

        List<Boolean> weekStatus = getWeekStatus(userId, today);
        return new CheckInResultDto(true, streak, reward, profile.getCoins(), weekStatus);
    }

    private int calculateStreak(UUID userId, LocalDate today, boolean includesToday) {
        LocalDate checkDate = includesToday ? today.minusDays(1) : today.minusDays(1);
        int streak = includesToday ? 1 : 0;

        // 从昨天往前数连续签到天数
        for (int i = 0; i < 30; i++) {
            if (checkInRepository.findByUserIdAndCheckDate(userId, checkDate).isPresent()) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private List<Boolean> getWeekStatus(UUID userId, LocalDate today) {
        // 获取本周一到周日的签到状态
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<DailyCheckIn> records = checkInRepository
                .findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(userId, monday, sunday);
        Set<LocalDate> checkedDates = new HashSet<>();
        records.forEach(r -> checkedDates.add(r.getCheckDate()));

        List<Boolean> status = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            status.add(checkedDates.contains(monday.plusDays(i)));
        }
        return status;
    }
}
