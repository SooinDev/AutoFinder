package com.example.autofinder.service;

import com.example.autofinder.model.UserBehavior;
import com.example.autofinder.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserBehaviorService {

    private final UserBehaviorRepository userBehaviorRepository;

    // 인메모리 캐시 (빠른 조회용)
    private final Map<String, UserBehaviorData> behaviorCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MINUTES = 10;

    /**
     * 사용자 행동 기록 (핵심 메서드)
     */
    @Async
    public void recordUserAction(Long userId, Long carId, String actionType, Object value) {
        try {
            // 1. 데이터베이스에 저장
            UserBehavior behavior = UserBehavior.builder()
                    .userId(userId)
                    .carId(carId)
                    .actionType(actionType)
                    .value(value != null ? value.toString() : "1")
                    .timestamp(LocalDateTime.now())
                    .build();

            userBehaviorRepository.save(behavior);

            // 2. 캐시 업데이트
            updateBehaviorCache(userId, carId, actionType, value);

            log.debug("사용자 행동 기록 완료 - 사용자: {}, 차량: {}, 행동: {}, 값: {}",
                    userId, carId, actionType, value);

        } catch (Exception e) {
            log.error("사용자 행동 기록 중 오류 - 사용자: {}, 차량: {}", userId, carId, e);
        }
    }

    /**
     * 특정 사용자의 행동 데이터 조회
     */
    public Map<String, Object> getUserBehaviorData(Long userId) {
        try {
            // 캐시 확인
            String cacheKey = "user_" + userId;
            UserBehaviorData cachedData = behaviorCache.get(cacheKey);

            if (cachedData != null && !cachedData.isExpired()) {
                log.debug("캐시에서 사용자 행동 데이터 반환 - 사용자: {}", userId);
                return cachedData.getData();
            }

            // 데이터베이스에서 조회 (최근 30일)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserIdAndTimestampAfter(userId, thirtyDaysAgo);

            // 행동 데이터 분석
            Map<String, Object> behaviorData = analyzeBehaviorData(behaviors);

            // 캐시 저장
            behaviorCache.put(cacheKey, new UserBehaviorData(behaviorData, LocalDateTime.now()));

            log.debug("사용자 행동 데이터 조회 완료 - 사용자: {}, 행동수: {}", userId, behaviors.size());
            return behaviorData;

        } catch (Exception e) {
            log.error("사용자 행동 데이터 조회 중 오류 - 사용자: {}", userId, e);
            return getDefaultBehaviorData();
        }
    }

    /**
     * 모든 사용자의 행동 데이터 조회 (AI 학습용)
     */
    public Map<String, Object> getAllUserBehaviors() {
        try {
            log.info("전체 사용자 행동 데이터 조회 시작...");

            // 최근 90일 데이터 조회 (학습에 충분한 데이터)
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minus(90, ChronoUnit.DAYS);
            List<UserBehavior> allBehaviors = userBehaviorRepository.findByTimestampAfter(ninetyDaysAgo);

            // 사용자별로 그룹화
            Map<Long, List<UserBehavior>> behaviorsByUser = allBehaviors.stream()
                    .collect(Collectors.groupingBy(UserBehavior::getUserId));

            Map<String, Object> allUserData = new HashMap<>();

            for (Map.Entry<Long, List<UserBehavior>> entry : behaviorsByUser.entrySet()) {
                Long userId = entry.getKey();
                List<UserBehavior> userBehaviors = entry.getValue();

                Map<String, Object> userData = analyzeBehaviorData(userBehaviors);
                allUserData.put(userId.toString(), userData);
            }

            log.info("전체 사용자 행동 데이터 조회 완료 - 사용자수: {}, 총 행동수: {}",
                    behaviorsByUser.size(), allBehaviors.size());

            return allUserData;

        } catch (Exception e) {
            log.error("전체 사용자 행동 데이터 조회 중 오류", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 행동 데이터 분석 (핵심 분석 로직)
     */
    private Map<String, Object> analyzeBehaviorData(List<UserBehavior> behaviors) {
        Map<String, Object> analysis = new HashMap<>();

        if (behaviors.isEmpty()) {
            return getDefaultBehaviorData();
        }

        // 1. 행동 타입별 집계
        Map<String, Long> actionCounts = behaviors.stream()
                .collect(Collectors.groupingBy(
                        UserBehavior::getActionType,
                        Collectors.counting()
                ));

        // 2. 차량별 관심도 계산
        Map<Long, Double> carInterestScores = calculateCarInterestScores(behaviors);

        // 3. 시간대별 활동 패턴
        Map<Integer, Long> hourlyActivity = behaviors.stream()
                .collect(Collectors.groupingBy(
                        behavior -> behavior.getTimestamp().getHour(),
                        Collectors.counting()
                ));

        // 4. 최근 활동 빈도
        Map<String, Long> recentActivity = calculateRecentActivity(behaviors);

        // 5. 선호도 점수 계산
        double engagementScore = calculateEngagementScore(behaviors);
        double diversityScore = calculateDiversityScore(behaviors);

        // 결과 구성
        analysis.put("action_counts", actionCounts);
        analysis.put("car_interest_scores", carInterestScores);
        analysis.put("hourly_activity", hourlyActivity);
        analysis.put("recent_activity", recentActivity);
        analysis.put("engagement_score", engagementScore);
        analysis.put("diversity_score", diversityScore);
        analysis.put("total_actions", behaviors.size());
        analysis.put("active_days", calculateActiveDays(behaviors));
        analysis.put("avg_session_duration", calculateAvgSessionDuration(behaviors));

        return analysis;
    }

    /**
     * 차량별 관심도 점수 계산 (가중치 적용)
     */
    private Map<Long, Double> calculateCarInterestScores(List<UserBehavior> behaviors) {
        Map<Long, Map<String, Integer>> carActionCounts = new HashMap<>();

        // 차량별, 행동별 집계
        for (UserBehavior behavior : behaviors) {
            carActionCounts.computeIfAbsent(behavior.getCarId(), k -> new HashMap<>())
                    .merge(behavior.getActionType(), 1, Integer::sum);
        }

        // 관심도 점수 계산 (행동별 가중치 적용)
        Map<String, Double> actionWeights = Map.of(
                "VIEW", 1.0,           // 조회
                "CLICK", 1.5,          // 클릭
                "DETAIL_VIEW", 2.0,    // 상세보기
                "INQUIRY", 4.0,        // 문의
                "BOOKMARK", 3.0,       // 북마크
                "SHARE", 2.5,          // 공유
                "FAVORITE", 5.0,       // 즐겨찾기
                "CONTACT", 6.0         // 연락하기
        );

        Map<Long, Double> interestScores = new HashMap<>();

        for (Map.Entry<Long, Map<String, Integer>> entry : carActionCounts.entrySet()) {
            Long carId = entry.getKey();
            Map<String, Integer> actions = entry.getValue();

            double score = 0.0;
            for (Map.Entry<String, Integer> actionEntry : actions.entrySet()) {
                String actionType = actionEntry.getKey();
                int count = actionEntry.getValue();
                double weight = actionWeights.getOrDefault(actionType, 1.0);

                score += count * weight;
            }

            // 최대 10점으로 정규화
            interestScores.put(carId, Math.min(score / 10.0, 10.0));
        }

        return interestScores;
    }

    /**
     * 최근 활동 패턴 분석 (시간대별)
     */
    private Map<String, Long> calculateRecentActivity(List<UserBehavior> behaviors) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> recentActivity = new HashMap<>();

        // 시간대별 구분
        recentActivity.put("last_hour", behaviors.stream()
                .filter(b -> b.getTimestamp().isAfter(now.minus(1, ChronoUnit.HOURS)))
                .count());

        recentActivity.put("last_24_hours", behaviors.stream()
                .filter(b -> b.getTimestamp().isAfter(now.minus(1, ChronoUnit.DAYS)))
                .count());

        recentActivity.put("last_week", behaviors.stream()
                .filter(b -> b.getTimestamp().isAfter(now.minus(7, ChronoUnit.DAYS)))
                .count());

        return recentActivity;
    }

    /**
     * 사용자 참여도 점수 계산
     */
    private double calculateEngagementScore(List<UserBehavior> behaviors) {
        if (behaviors.isEmpty()) return 0.0;

        // 다양한 지표를 종합한 참여도 점수
        double actionDiversity = behaviors.stream()
                .map(UserBehavior::getActionType)
                .distinct()
                .count() / 8.0; // 최대 8가지 행동 타입

        double frequency = Math.min(behaviors.size() / 100.0, 1.0); // 최대 100회

        LocalDateTime now = LocalDateTime.now();
        long daysActive = behaviors.stream()
                .map(b -> b.getTimestamp().toLocalDate())
                .distinct()
                .count();

        double consistency = Math.min(daysActive / 30.0, 1.0); // 최대 30일

        // 가중 평균
        return (actionDiversity * 0.3 + frequency * 0.4 + consistency * 0.3) * 10.0;
    }

    /**
     * 관심 다양성 점수 계산
     */
    private double calculateDiversityScore(List<UserBehavior> behaviors) {
        if (behaviors.isEmpty()) return 0.0;

        // 관심을 보인 차량의 다양성
        long uniqueCars = behaviors.stream()
                .map(UserBehavior::getCarId)
                .distinct()
                .count();

        return Math.min(uniqueCars / 20.0, 1.0) * 10.0; // 최대 20대
    }

    /**
     * 활동 일수 계산
     */
    private long calculateActiveDays(List<UserBehavior> behaviors) {
        return behaviors.stream()
                .map(behavior -> behavior.getTimestamp().toLocalDate())
                .distinct()
                .count();
    }

    /**
     * 평균 세션 지속 시간 계산 (분 단위)
     */
    private double calculateAvgSessionDuration(List<UserBehavior> behaviors) {
        if (behaviors.size() < 2) return 0.0;

        // 시간순 정렬
        List<UserBehavior> sortedBehaviors = behaviors.stream()
                .sorted(Comparator.comparing(UserBehavior::getTimestamp))
                .collect(Collectors.toList());

        List<Long> sessionDurations = new ArrayList<>();
        LocalDateTime sessionStart = sortedBehaviors.get(0).getTimestamp();
        LocalDateTime lastAction = sessionStart;

        for (int i = 1; i < sortedBehaviors.size(); i++) {
            LocalDateTime currentAction = sortedBehaviors.get(i).getTimestamp();
            long minutesSinceLastAction = ChronoUnit.MINUTES.between(lastAction, currentAction);

            if (minutesSinceLastAction > 30) { // 30분 이상 간격이면 새 세션
                long sessionDuration = ChronoUnit.MINUTES.between(sessionStart, lastAction);
                if (sessionDuration > 0) {
                    sessionDurations.add(sessionDuration);
                }
                sessionStart = currentAction;
            }
            lastAction = currentAction;
        }

        // 마지막 세션 처리
        long lastSessionDuration = ChronoUnit.MINUTES.between(sessionStart, lastAction);
        if (lastSessionDuration > 0) {
            sessionDurations.add(lastSessionDuration);
        }

        return sessionDurations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 캐시 업데이트
     */
    private void updateBehaviorCache(Long userId, Long carId, String actionType, Object value) {
        String cacheKey = "user_" + userId;
        UserBehaviorData cachedData = behaviorCache.get(cacheKey);

        if (cachedData != null && !cachedData.isExpired()) {
            // 기존 캐시 데이터 업데이트
            Map<String, Object> data = cachedData.getData();

            // 액션 카운트 업데이트
            @SuppressWarnings("unchecked")
            Map<String, Long> actionCounts = (Map<String, Long>) data.get("action_counts");
            if (actionCounts != null) {
                actionCounts.merge(actionType, 1L, Long::sum);
            }

            // 차량 관심도 업데이트
            @SuppressWarnings("unchecked")
            Map<Long, Double> carScores = (Map<Long, Double>) data.get("car_interest_scores");
            if (carScores != null) {
                carScores.merge(carId, 1.0, Double::sum);
            }

            // 총 액션 수 업데이트
            Long totalActions = (Long) data.get("total_actions");
            data.put("total_actions", totalActions != null ? totalActions + 1 : 1);
        }
    }

    /**
     * 기본 행동 데이터 반환 (신규 사용자용)
     */
    private Map<String, Object> getDefaultBehaviorData() {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("action_counts", Collections.emptyMap());
        defaultData.put("car_interest_scores", Collections.emptyMap());
        defaultData.put("hourly_activity", Collections.emptyMap());
        defaultData.put("recent_activity", Map.of(
                "last_hour", 0L,
                "last_24_hours", 0L,
                "last_week", 0L
        ));
        defaultData.put("engagement_score", 0.0);
        defaultData.put("diversity_score", 0.0);
        defaultData.put("total_actions", 0L);
        defaultData.put("active_days", 0L);
        defaultData.put("avg_session_duration", 0.0);

        return defaultData;
    }

    /**
     * 캐시 정리 (스케줄링)
     */
    @Async
    public void clearExpiredCache() {
        behaviorCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("만료된 행동 데이터 캐시 정리 완료");
    }

    /**
     * 사용자 행동 통계 조회 (관리자용)
     */
    public Map<String, Object> getBehaviorStatistics() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            List<UserBehavior> recentBehaviors = userBehaviorRepository.findByTimestampAfter(sevenDaysAgo);

            Map<String, Object> stats = new HashMap<>();

            // 전체 통계
            stats.put("total_behaviors", recentBehaviors.size());
            stats.put("unique_users", recentBehaviors.stream()
                    .map(UserBehavior::getUserId)
                    .distinct()
                    .count());
            stats.put("unique_cars", recentBehaviors.stream()
                    .map(UserBehavior::getCarId)
                    .distinct()
                    .count());

            // 행동 타입별 통계
            Map<String, Long> actionTypeStats = recentBehaviors.stream()
                    .collect(Collectors.groupingBy(
                            UserBehavior::getActionType,
                            Collectors.counting()
                    ));
            stats.put("action_type_stats", actionTypeStats);

            // 일별 활동량
            Map<String, Long> dailyActivity = recentBehaviors.stream()
                    .collect(Collectors.groupingBy(
                            behavior -> behavior.getTimestamp().toLocalDate().toString(),
                            Collectors.counting()
                    ));
            stats.put("daily_activity", dailyActivity);

            return stats;

        } catch (Exception e) {
            log.error("행동 통계 조회 중 오류", e);
            return Collections.emptyMap();
        }
    }

    // 내부 클래스
    private static class UserBehaviorData {
        private final Map<String, Object> data;
        private final LocalDateTime timestamp;

        public UserBehaviorData(Map<String, Object> data, LocalDateTime timestamp) {
            this.data = new HashMap<>(data);
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return timestamp.isBefore(LocalDateTime.now().minus(CACHE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        }

        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }
    }
}