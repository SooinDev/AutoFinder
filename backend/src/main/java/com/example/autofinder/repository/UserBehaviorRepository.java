package com.example.autofinder.repository;

import com.example.autofinder.model.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    /**
     * 특정 사용자의 행동 기록 조회 (시간순 정렬)
     */
    List<UserBehavior> findByUserIdOrderByTimestampDesc(Long userId);

    /**
     * 특정 사용자의 최근 행동 기록 조회
     */
    List<UserBehavior> findByUserIdAndTimestampAfter(Long userId, LocalDateTime timestamp);

    /**
     * 특정 차량에 대한 모든 사용자 행동 조회
     */
    List<UserBehavior> findByCarIdOrderByTimestampDesc(Long carId);

    /**
     * 특정 차량에 대한 최근 행동 조회
     */
    List<UserBehavior> findByCarIdAndTimestampAfter(Long carId, LocalDateTime timestamp);

    /**
     * 특정 행동 타입 조회
     */
    List<UserBehavior> findByActionTypeOrderByTimestampDesc(String actionType);

    /**
     * 특정 시간 이후의 모든 행동 조회
     */
    List<UserBehavior> findByTimestampAfter(LocalDateTime timestamp);

    /**
     * 특정 사용자의 특정 차량에 대한 행동 조회
     */
    List<UserBehavior> findByUserIdAndCarIdOrderByTimestampDesc(Long userId, Long carId);

    /**
     * 특정 사용자의 특정 행동 타입 조회
     */
    List<UserBehavior> findByUserIdAndActionTypeOrderByTimestampDesc(Long userId, String actionType);

    /**
     * 사용자별 행동 수 집계
     */
    @Query("SELECT ub.userId, COUNT(ub) as actionCount " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since " +
            "GROUP BY ub.userId " +
            "ORDER BY actionCount DESC")
    List<Object[]> countActionsByUserSince(@Param("since") LocalDateTime since);

    /**
     * 차량별 관심도 집계
     */
    @Query("SELECT ub.carId, COUNT(ub) as interestCount " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since " +
            "GROUP BY ub.carId " +
            "ORDER BY interestCount DESC")
    List<Object[]> countInterestByCarSince(@Param("since") LocalDateTime since);

    /**
     * 행동 타입별 집계
     */
    @Query("SELECT ub.actionType, COUNT(ub) as actionCount " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since " +
            "GROUP BY ub.actionType " +
            "ORDER BY actionCount DESC")
    List<Object[]> countActionsByTypeSince(@Param("since") LocalDateTime since);

    /**
     * 특정 사용자의 최근 N개 행동 조회
     */
    @Query("SELECT ub FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "ORDER BY ub.timestamp DESC " +
            "LIMIT :limit")
    List<UserBehavior> findTopNByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 활성 사용자 조회 (최근 기간 내 행동이 있는 사용자)
     */
    @Query("SELECT DISTINCT ub.userId " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since")
    List<Long> findActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 인기 차량 조회 (최근 기간 내 관심도가 높은 차량)
     */
    @Query("SELECT ub.carId, COUNT(ub) as interestCount " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since " +
            "AND ub.actionType IN ('VIEW', 'CLICK', 'FAVORITE', 'INQUIRY') " +
            "GROUP BY ub.carId " +
            "HAVING COUNT(ub) >= :minInterest " +
            "ORDER BY interestCount DESC")
    List<Object[]> findPopularCarsSince(@Param("since") LocalDateTime since,
                                        @Param("minInterest") long minInterest);

    /**
     * 사용자의 선호 행동 패턴 분석
     */
    @Query("SELECT ub.actionType, COUNT(ub) as count, AVG(CAST(ub.value AS double)) as avgValue " +
            "FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "AND ub.timestamp > :since " +
            "GROUP BY ub.actionType")
    List<Object[]> analyzeUserBehaviorPattern(@Param("userId") Long userId,
                                              @Param("since") LocalDateTime since);

    /**
     * 시간대별 활동 패턴 조회
     */
    @Query("SELECT EXTRACT(HOUR FROM ub.timestamp) as hour, COUNT(ub) as count " +
            "FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "AND ub.timestamp > :since " +
            "GROUP BY EXTRACT(HOUR FROM ub.timestamp) " +
            "ORDER BY hour")
    List<Object[]> getHourlyActivityPattern(@Param("userId") Long userId,
                                            @Param("since") LocalDateTime since);

    /**
     * 세션별 행동 분석
     */
    @Query("SELECT ub.sessionId, COUNT(ub) as actionCount, " +
            "MIN(ub.timestamp) as sessionStart, MAX(ub.timestamp) as sessionEnd " +
            "FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "AND ub.sessionId IS NOT NULL " +
            "AND ub.timestamp > :since " +
            "GROUP BY ub.sessionId " +
            "ORDER BY sessionStart DESC")
    List<Object[]> getSessionAnalysis(@Param("userId") Long userId,
                                      @Param("since") LocalDateTime since);

    /**
     * 관심 차량 추이 분석 (시간별 관심도 변화)
     */
    @Query("SELECT ub.carId, " +
            "DATE_TRUNC('day', ub.timestamp) as day, " +
            "COUNT(ub) as dailyInterest " +
            "FROM UserBehavior ub " +
            "WHERE ub.carId IN :carIds " +
            "AND ub.timestamp > :since " +
            "GROUP BY ub.carId, DATE_TRUNC('day', ub.timestamp) " +
            "ORDER BY day DESC")
    List<Object[]> getCarInterestTrend(@Param("carIds") List<Long> carIds,
                                       @Param("since") LocalDateTime since);

    /**
     * 유사한 관심사를 가진 사용자 찾기
     */
    @Query("SELECT ub2.userId, COUNT(DISTINCT ub2.carId) as commonInterests " +
            "FROM UserBehavior ub1 " +
            "JOIN UserBehavior ub2 ON ub1.carId = ub2.carId " +
            "WHERE ub1.userId = :userId " +
            "AND ub2.userId != :userId " +
            "AND ub1.timestamp > :since " +
            "AND ub2.timestamp > :since " +
            "GROUP BY ub2.userId " +
            "HAVING COUNT(DISTINCT ub2.carId) >= :minCommon " +
            "ORDER BY commonInterests DESC")
    List<Object[]> findSimilarUsers(@Param("userId") Long userId,
                                    @Param("since") LocalDateTime since,
                                    @Param("minCommon") long minCommon);

    /**
     * 사용자의 행동 강도 계산 (가중치 적용)
     */
    @Query("SELECT ub.carId, " +
            "SUM(CASE " +
            "    WHEN ub.actionType = 'VIEW' THEN 1 " +
            "    WHEN ub.actionType = 'CLICK' THEN 1.5 " +
            "    WHEN ub.actionType = 'DETAIL_VIEW' THEN 2 " +
            "    WHEN ub.actionType = 'BOOKMARK' THEN 3 " +
            "    WHEN ub.actionType = 'INQUIRY' THEN 4 " +
            "    WHEN ub.actionType = 'FAVORITE' THEN 5 " +
            "    WHEN ub.actionType = 'CONTACT' THEN 6 " +
            "    ELSE 1 " +
            "END) as weightedScore " +
            "FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "AND ub.timestamp > :since " +
            "GROUP BY ub.carId " +
            "ORDER BY weightedScore DESC")
    List<Object[]> calculateWeightedInterestScores(@Param("userId") Long userId,
                                                   @Param("since") LocalDateTime since);

    /**
     * 행동 시퀀스 분석 (사용자의 행동 패턴)
     */
    @Query("SELECT ub.actionType, " +
            "LAG(ub.actionType) OVER (PARTITION BY ub.userId ORDER BY ub.timestamp) as previousAction, " +
            "COUNT(*) as transitionCount " +
            "FROM UserBehavior ub " +
            "WHERE ub.userId = :userId " +
            "AND ub.timestamp > :since " +
            "GROUP BY ub.actionType, LAG(ub.actionType) OVER (PARTITION BY ub.userId ORDER BY ub.timestamp) " +
            "ORDER BY transitionCount DESC")
    List<Object[]> analyzeBehaviorSequence(@Param("userId") Long userId,
                                           @Param("since") LocalDateTime since);

    /**
     * 데이터 정리 (오래된 행동 데이터 삭제)
     */
    @Query("DELETE FROM UserBehavior ub WHERE ub.timestamp < :cutoffDate")
    void deleteOldBehaviors(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 사용자별 최종 활동 시간 조회
     */
    @Query("SELECT ub.userId, MAX(ub.timestamp) as lastActivity " +
            "FROM UserBehavior ub " +
            "GROUP BY ub.userId " +
            "ORDER BY lastActivity DESC")
    List<Object[]> getLastActivityByUser();

    /**
     * 행동 데이터 통계 (전체)
     */
    @Query("SELECT " +
            "COUNT(DISTINCT ub.userId) as uniqueUsers, " +
            "COUNT(DISTINCT ub.carId) as uniqueCars, " +
            "COUNT(ub) as totalActions, " +
            "COUNT(DISTINCT ub.actionType) as uniqueActionTypes " +
            "FROM UserBehavior ub " +
            "WHERE ub.timestamp > :since")
    Object[] getOverallStatistics(@Param("since") LocalDateTime since);
}