package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 推荐列表：支持年龄、性别、距离筛选
     * - 排除自己、已滑过的、已封号的、双向屏蔽、未完成onboarding的用户
     * - minAge/maxAge: 年龄范围筛选（基于 birth_date）
     * - gender: 性别筛选（null 则不筛选）
     * - myLat/myLon: 当前用户经纬度（用于距离计算）
     * - maxDistanceKm: 最大距离（km），null 则不筛选距离
     */
    @Query(value = """
        SELECT p.* FROM profiles p
        WHERE p.id != :myId
          AND p.is_active = true
          AND (p.onboarding_completed = true OR p.onboarding_completed IS NULL)
          AND p.id NOT IN (
              SELECT s.swiped_id FROM swipes s WHERE s.swiper_id = :myId
          )
          AND p.id NOT IN (
              SELECT bu.blocked_id FROM blocked_users bu WHERE bu.blocker_id = :myId
          )
          AND p.id NOT IN (
              SELECT bu.blocker_id FROM blocked_users bu WHERE bu.blocked_id = :myId
          )
          AND (:minAge IS NULL OR EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) >= :minAge)
          AND (:maxAge IS NULL OR EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) <= :maxAge)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (
              :maxDistanceKm IS NULL
              OR :myLat IS NULL
              OR :myLon IS NULL
              OR (
                  p.latitude IS NOT NULL AND p.longitude IS NOT NULL
                  AND p.latitude BETWEEN (:myLat - :maxDistanceKm / 111.0) AND (:myLat + :maxDistanceKm / 111.0)
                  AND p.longitude BETWEEN (:myLon - :maxDistanceKm / (111.0 * COS(RADIANS(:myLat)))) AND (:myLon + :maxDistanceKm / (111.0 * COS(RADIANS(:myLat))))
                  AND (
                      6371.0 * ACOS(
                          LEAST(1.0, COS(RADIANS(:myLat)) * COS(RADIANS(p.latitude))
                              * COS(RADIANS(p.longitude) - RADIANS(:myLon))
                              + SIN(RADIANS(:myLat)) * SIN(RADIANS(p.latitude)))
                      )
                  ) <= :maxDistanceKm
              )
          )
        ORDER BY
            CASE WHEN p.boost_until > NOW() THEN 0 ELSE 1 END,
            CASE WHEN p.vip_tier != 'none' THEN 0 ELSE 1 END,
            p.last_active DESC NULLS LAST
        """, nativeQuery = true)
    List<Profile> findFeed(
            @Param("myId") UUID myId,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("gender") String gender,
            @Param("myLat") Double myLat,
            @Param("myLon") Double myLon,
            @Param("maxDistanceKm") Double maxDistanceKm,
            Pageable pageable);

    /**
     * 搜索附近的人（独立端点使用）
     * - 先用经纬度范围框粗筛，再用 Haversine 精确过滤
     * - 排除自己和未完成onboarding的用户
     */
    @Query(value = """
        SELECT p.* FROM profiles p
        WHERE p.id != :myId
          AND p.is_active = true
          AND (p.onboarding_completed = true OR p.onboarding_completed IS NULL)
          AND p.latitude IS NOT NULL
          AND p.longitude IS NOT NULL
          AND p.latitude BETWEEN (:lat - :radiusKm / 111.0) AND (:lat + :radiusKm / 111.0)
          AND p.longitude BETWEEN (:lon - :radiusKm / (111.0 * COS(RADIANS(:lat)))) AND (:lon + :radiusKm / (111.0 * COS(RADIANS(:lat))))
          AND (
              6371.0 * ACOS(
                  LEAST(1.0, COS(RADIANS(:lat)) * COS(RADIANS(p.latitude))
                      * COS(RADIANS(p.longitude) - RADIANS(:lon))
                      + SIN(RADIANS(:lat)) * SIN(RADIANS(p.latitude)))
              )
          ) <= :radiusKm
        ORDER BY (
            6371.0 * ACOS(
                LEAST(1.0, COS(RADIANS(:lat)) * COS(RADIANS(p.latitude))
                    * COS(RADIANS(p.longitude) - RADIANS(:lon))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(p.latitude)))
            )
        ) ASC
        """, nativeQuery = true)
    List<Profile> findNearby(
            @Param("myId") UUID myId,
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm,
            Pageable pageable);
}
