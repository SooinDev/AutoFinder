package com.example.autofinder.repository;

import com.example.autofinder.model.CarComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CarComparisonRepository extends JpaRepository<CarComparison, Long> {

    /**
     * 사용자별 비교 기록 조회 (최신순)
     */
    List<CarComparison> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 기간 이후 비교 기록 수 조회
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 사용자별 비교 기록 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 특정 차량이 포함된 비교 기록 조회
     */
    @Query("SELECT c FROM CarComparison c WHERE c.carIds LIKE %:carId%")
    List<CarComparison> findByCarIdContaining(@Param("carId") String carId);

    /**
     * 최근 N일간의 비교 기록 조회
     */
    @Query("SELECT c FROM CarComparison c WHERE c.createdAt >= :startDate ORDER BY c.createdAt DESC")
    List<CarComparison> findRecentComparisons(@Param("startDate") LocalDateTime startDate);

    /**
     * 가장 많이 비교된 차량들 조회 (수정된 버전)
     */
    @Query(value = """
        SELECT car_id, COUNT(*) as comparison_count
        FROM (
            SELECT TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(car_ids, ',', numbers.n), ',', -1)) as car_id
            FROM car_comparisons
            CROSS JOIN (
                SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 
                UNION ALL SELECT 4 UNION ALL SELECT 5
            ) numbers
            WHERE CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) >= numbers.n - 1
            AND car_ids IS NOT NULL
            AND car_ids != ''
        ) car_list
        WHERE car_id REGEXP '^[0-9]+$'
        AND car_id IS NOT NULL
        AND car_id != ''
        GROUP BY car_id
        ORDER BY comparison_count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findMostComparedCars();

    /**
     * 비교 당 평균 차량 수 계산 (수정된 버전)
     */
    @Query(value = """
        SELECT COALESCE(AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1), 0)
        FROM car_comparisons
        WHERE car_ids IS NOT NULL AND car_ids != ''
        """, nativeQuery = true)
    Double getAverageCarCountPerComparison();

    /**
     * 사용자별 최근 비교 기록 조회 (제한된 개수)
     */
    @Query("SELECT c FROM CarComparison c WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    List<CarComparison> findTopNByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 기간의 비교 통계 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            DATE(created_at) as comparison_date,
            COUNT(*) as daily_comparisons,
            COUNT(DISTINCT user_id) as unique_users,
            COALESCE(AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1), 0) as avg_cars_per_comparison
        FROM car_comparisons 
        WHERE created_at >= :startDate 
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY DATE(created_at) 
        ORDER BY comparison_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyComparisonStats(@Param("startDate") LocalDateTime startDate);

    /**
     * 사용자의 비교 패턴 분석 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            (CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1) as car_count,
            COUNT(*) as frequency
        FROM car_comparisons 
        WHERE user_id = :userId
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY (CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1)
        ORDER BY car_count
        """, nativeQuery = true)
    List<Object[]> getUserComparisonPattern(@Param("userId") Long userId);

    /**
     * 인기 비교 조합 분석 (수정된 버전)
     */
    @Query(value = """
        SELECT car_ids, COUNT(*) as frequency 
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY car_ids 
        HAVING COUNT(*) >= :minFrequency
        ORDER BY frequency DESC 
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getPopularComparisonCombinations(
            @Param("startDate") LocalDateTime startDate,
            @Param("minFrequency") int minFrequency);

    /**
     * 브랜드별 비교 빈도 분석 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            SUBSTRING_INDEX(c.model, ' ', 1) as brand,
            COUNT(*) as comparison_frequency
        FROM car_comparisons cc
        JOIN cars c ON FIND_IN_SET(c.id, REPLACE(cc.car_ids, ',', ',')) > 0
        WHERE cc.created_at >= :startDate
        AND cc.car_ids IS NOT NULL 
        AND cc.car_ids != ''
        GROUP BY SUBSTRING_INDEX(c.model, ' ', 1)
        ORDER BY comparison_frequency DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getBrandComparisonFrequency(@Param("startDate") LocalDateTime startDate);

    /**
     * 월별 비교 트렌드 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            YEAR(created_at) as year,
            MONTH(created_at) as month,
            COUNT(*) as monthly_comparisons,
            COUNT(DISTINCT user_id) as unique_users
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY YEAR(created_at), MONTH(created_at)
        ORDER BY year DESC, month DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyComparisonTrend(@Param("startDate") LocalDateTime startDate);

    /**
     * 특정 차량과 함께 자주 비교되는 차량들 (단순화된 버전)
     */
    @Query(value = """
        SELECT 
            other_cars.car_id as related_car_id,
            COUNT(*) as co_comparison_count
        FROM (
            SELECT id, car_ids
            FROM car_comparisons 
            WHERE car_ids LIKE CONCAT('%', :targetCarId, '%')
            AND car_ids IS NOT NULL 
            AND car_ids != ''
        ) target_comparisons
        JOIN (
            SELECT 
                cc.id,
                TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(cc.car_ids, ',', n.n), ',', -1)) as car_id
            FROM car_comparisons cc
            CROSS JOIN (
                SELECT 1 as n UNION ALL SELECT 2 UNION ALL SELECT 3 
                UNION ALL SELECT 4 UNION ALL SELECT 5
            ) n
            WHERE CHAR_LENGTH(cc.car_ids) - CHAR_LENGTH(REPLACE(cc.car_ids, ',', '')) >= n.n - 1
            AND cc.car_ids IS NOT NULL 
            AND cc.car_ids != ''
        ) other_cars ON target_comparisons.id = other_cars.id
        WHERE other_cars.car_id != :targetCarId
        AND other_cars.car_id REGEXP '^[0-9]+$'
        GROUP BY other_cars.car_id
        ORDER BY co_comparison_count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findFrequentlyComparedWith(@Param("targetCarId") String targetCarId);

    /**
     * 비교 세션 분석 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            user_id,
            COUNT(*) as session_comparisons,
            MIN(created_at) as session_start,
            MAX(created_at) as session_end,
            TIMESTAMPDIFF(MINUTE, MIN(created_at), MAX(created_at)) as session_duration_minutes
        FROM car_comparisons 
        WHERE user_id IS NOT NULL 
        AND created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY user_id, DATE(created_at)
        HAVING COUNT(*) > 1
        ORDER BY session_comparisons DESC
        LIMIT 50
        """, nativeQuery = true)
    List<Object[]> getComparisonSessions(@Param("startDate") LocalDateTime startDate);

    /**
     * 데이터 정리 - 오래된 비교 기록 삭제 (수정된 버전)
     */
    @Query(value = """
        DELETE FROM car_comparisons 
        WHERE created_at < :cutoffDate 
        AND user_id IS NULL
        """, nativeQuery = true)
    void deleteOldAnonymousComparisons(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 사용자별 마지막 비교 시간 (수정된 버전)
     */
    @Query("""
        SELECT c.userId, MAX(c.createdAt) as lastComparison 
        FROM CarComparison c 
        WHERE c.userId IS NOT NULL 
        GROUP BY c.userId 
        ORDER BY lastComparison DESC
        """)
    List<Object[]> getLastComparisonTimeByUser();

    /**
     * 비교 활동이 활발한 사용자 TOP N (수정된 버전)
     */
    @Query("""
        SELECT c.userId, COUNT(*) as comparisonCount, MAX(c.createdAt) as lastActivity
        FROM CarComparison c 
        WHERE c.userId IS NOT NULL 
        AND c.createdAt >= :startDate
        GROUP BY c.userId 
        ORDER BY comparisonCount DESC
        """)
    List<Object[]> getMostActiveComparers(@Param("startDate") LocalDateTime startDate,
                                          org.springframework.data.domain.Pageable pageable);

    /**
     * 시간대별 비교 활동 패턴 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            HOUR(created_at) as hour_of_day,
            COUNT(*) as comparison_count,
            COUNT(DISTINCT user_id) as unique_users
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY HOUR(created_at)
        ORDER BY hour_of_day
        """, nativeQuery = true)
    List<Object[]> getHourlyComparisonPattern(@Param("startDate") LocalDateTime startDate);

    /**
     * 요일별 비교 활동 패턴 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            DAYOFWEEK(created_at) as day_of_week,
            DAYNAME(created_at) as day_name,
            COUNT(*) as comparison_count,
            COALESCE(AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1), 0) as avg_cars_compared
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        GROUP BY DAYOFWEEK(created_at), DAYNAME(created_at)
        ORDER BY day_of_week
        """, nativeQuery = true)
    List<Object[]> getDailyComparisonPattern(@Param("startDate") LocalDateTime startDate);

    /**
     * 비교 완료율 분석 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN comparison_name IS NOT NULL AND comparison_name != '' THEN 1 END) as saved_comparisons,
            COUNT(*) as total_comparisons,
            ROUND(COUNT(CASE WHEN comparison_name IS NOT NULL AND comparison_name != '' THEN 1 END) * 100.0 / COUNT(*), 2) as save_rate
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        """, nativeQuery = true)
    Object[] getComparisonSaveRate(@Param("startDate") LocalDateTime startDate);

    /**
     * 사용자 세그먼트별 비교 패턴 (수정된 버전)
     */
    @Query(value = """
        SELECT 
            CASE 
                WHEN comparison_count = 1 THEN 'One-time'
                WHEN comparison_count BETWEEN 2 AND 5 THEN 'Casual'
                WHEN comparison_count BETWEEN 6 AND 15 THEN 'Regular'
                ELSE 'Power User'
            END as user_segment,
            COUNT(*) as user_count,
            AVG(comparison_count) as avg_comparisons_per_user
        FROM (
            SELECT user_id, COUNT(*) as comparison_count
            FROM car_comparisons 
            WHERE user_id IS NOT NULL 
            AND created_at >= :startDate
            AND car_ids IS NOT NULL 
            AND car_ids != ''
            GROUP BY user_id
        ) user_stats
        GROUP BY 
            CASE 
                WHEN comparison_count = 1 THEN 'One-time'
                WHEN comparison_count BETWEEN 2 AND 5 THEN 'Casual'
                WHEN comparison_count BETWEEN 6 AND 15 THEN 'Regular'
                ELSE 'Power User'
            END
        ORDER BY avg_comparisons_per_user
        """, nativeQuery = true)
    List<Object[]> getUserSegmentAnalysis(@Param("startDate") LocalDateTime startDate);

    /**
     * 간단한 비교 통계 조회 (기본 기능용)
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_comparisons,
            COUNT(DISTINCT user_id) as unique_users,
            COALESCE(AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1), 0) as avg_cars_per_comparison
        FROM car_comparisons 
        WHERE created_at >= :startDate
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        """, nativeQuery = true)
    Object[] getBasicComparisonStats(@Param("startDate") LocalDateTime startDate);

    /**
     * 특정 사용자의 비교 요약 통계
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_comparisons,
            COUNT(DISTINCT car_ids) as unique_combinations,
            MIN(created_at) as first_comparison,
            MAX(created_at) as last_comparison,
            COALESCE(AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1), 0) as avg_cars_per_comparison
        FROM car_comparisons 
        WHERE user_id = :userId
        AND car_ids IS NOT NULL 
        AND car_ids != ''
        """, nativeQuery = true)
    Object[] getUserComparisonSummary(@Param("userId") Long userId);
}