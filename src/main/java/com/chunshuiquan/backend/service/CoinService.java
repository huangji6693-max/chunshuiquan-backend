package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.entity.CoinTransaction;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.CoinTransactionRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CoinService {

    /** 充值包定义: packageId -> coins */
    private static final Map<String, Integer> PACKAGES = Map.of(
            "small",  60,     // ¥6  → 60金币
            "medium", 300,    // ¥25 → 300金币
            "large",  980,    // ¥68 → 980金币
            "mega",   2000    // ¥128 → 2000金币
    );

    private final ProfileRepository profileRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    public CoinService(ProfileRepository profileRepository,
                       CoinTransactionRepository coinTransactionRepository) {
        this.profileRepository = profileRepository;
        this.coinTransactionRepository = coinTransactionRepository;
    }

    /** 获取充值包列表 */
    public Map<String, Integer> getPackages() {
        return PACKAGES;
    }

    /** 充值金币（简化版，实际应校验支付收据） */
    @Transactional
    public CoinTransaction recharge(UUID userId, String packageId, String receipt, String platform) {
        Integer coins = PACKAGES.get(packageId);
        if (coins == null) {
            throw new IllegalArgumentException("无效的充值包: " + packageId);
        }

        // TODO: 根据 platform 校验 receipt（Apple/Google/Stripe）
        // 此处简化为直接充值

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        profile.setCoins(profile.getCoins() + coins);
        profileRepository.save(profile);

        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(userId);
        tx.setAmount(coins);
        tx.setBalanceAfter(profile.getCoins());
        tx.setType("recharge");
        tx.setNote(packageId + " 充值包 +" + coins + "金币");
        tx.setOrderId(receipt);
        return coinTransactionRepository.save(tx);
    }

    /** 管理员赠送金币 */
    @Transactional
    public CoinTransaction adminGrant(UUID userId, int amount, String note) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        profile.setCoins(profile.getCoins() + amount);
        profileRepository.save(profile);

        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setBalanceAfter(profile.getCoins());
        tx.setType("admin_grant");
        tx.setNote(note != null ? note : "管理员赠送");
        return coinTransactionRepository.save(tx);
    }

    /** 记录消费流水（送礼时调用） */
    @Transactional
    public void recordSpend(UUID userId, int amount, int balanceAfter, String type, String note) {
        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(userId);
        tx.setAmount(-amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setType(type);
        tx.setNote(note);
        coinTransactionRepository.save(tx);
    }

    /** 查询流水 */
    public List<CoinTransaction> getTransactions(UUID userId) {
        return coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
