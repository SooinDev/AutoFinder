package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.Favorite;
import com.example.autofinder.model.User;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.FavoriteRepository;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLRecommendationService {

    private final AIServiceClient aiServiceClient;
    private final CarRepository carRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final UserBehaviorService userBehaviorService;
    private final Environment environment;

    @Value("${ai.recommendation.use-machine-learning:true}")
    private boolean useMachineLearning;

    @Value("${ai.recommendation.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${ai.recommendation.ab-test.enabled:false}")
    private boolean abTestEnabled;

    @Value("${ai.recommendation.ab-test.machine-learning-ratio:0.5}")
    private double machineLearningRatio;

    @Value("${ai.recommendation.target-users.enabled:false}")
    private boolean targetUsersEnabled;

    @Value("${ai.recommendation.target-users.whitelist:}")
    private List<Long> whitelistUsers;

    // 캐시 관리
    private final Map<Long, CachedRecommendation> recommendationCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MINUTES = 5;

    /**
     * 스마트 추천 라우팅 - 개발/테스트/운영 모든 환경 지원
     */
    public List<RecommendedCar> getSmartRecommendations(Long userId, int topK) {
        // 1. 사용자별 머신러닝 사용 여부 결정
        boolean shouldUseMachineLearning = shouldUseMachineLearningForUser(userId);

        log.info("추천 전략 결정 - 사용자: {}, 머신러닝: {}, 환경: {}",
                userId, shouldUseMachineLearning, getCurrentProfile());

        if (shouldUseMachineLearning) {
            return getMachineLearningRecommendationsWithFallback(userId, topK);
        } else {
            return getLegacyRecommendations(userId, topK);
        }
    }

    /**
     * 사용자별 머신러닝 사용 여부 결정 로직
     */
    private boolean shouldUseMachineLearningForUser(Long userId) {
        // 1. 전역 설정 확인
        if (!useMachineLearning) {
            log.debug("머신러닝 전역 비활성화");
            return false;
        }

        // 2. 화이트리스트 확인 (특정 사용자만)
        if (targetUsersEnabled && !whitelistUsers.isEmpty()) {
            boolean inWhitelist = whitelistUsers.contains(userId);
            log.debug("화이트리스트 확인 - 사용자: {}, 포함여부: {}", userId, inWhitelist);
            return inWhitelist;
        }

        // 3. A/B 테스트 확인
        if (abTestEnabled) {
            // 사용자 ID 기반 해시로 일관된 그룹 배정
            double hash = Math.abs(userId.hashCode() % 100) / 100.0;
            boolean inMachineLearningGroup = hash < machineLearningRatio;
            log.debug("A/B 테스트 - 사용자: {}, 해시: {:.2f}, 머신러닝 그룹: {}",
                    userId, hash, inMachineLearningGroup);
            return inMachineLearningGroup;
        }

        // 4. 기본값: 머신러닝 사용
        return true;
    }

    /**
     * 머신러닝 추천 + 자동 폴백
     */
    private List<RecommendedCar> getMachineLearningRecommendationsWithFallback(Long userId, int topK) {
        long startTime = System.currentTimeMillis();

        try {
            // 머신러닝 시도
            List<RecommendedCar> recommendations = attemptMachineLearningRecommendation(userId, topK);

            long duration = System.currentTimeMillis() - startTime;
            log.info("머신러닝 추천 성공 - 사용자: {}, 결과: {}개, 소요시간: {}ms",
                    userId, recommendations.size(), duration);

            // 성공 시 메트릭 기록
            recordRecommendationMetrics(userId, "machine_learning", true, duration, recommendations.size());

            return recommendations;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("머신러닝 추천 실패 - 사용자: {}, 소요시간: {}ms, 오류: {}",
                    userId, duration, e.getMessage());

            // 실패 메트릭 기록
            recordRecommendationMetrics(userId, "machine_learning", false, duration, 0);

            // 폴백 활성화 시 기존 방식 사용
            if (fallbackEnabled) {
                log.info("기존 방식으로 폴백 - 사용자: {}", userId);
                List<RecommendedCar> fallbackRecommendations = getLegacyRecommendations(userId, topK);

                // 폴백 메트릭 기록
                recordRecommendationMetrics(userId, "legacy_fallback", true,
                        System.currentTimeMillis() - startTime, fallbackRecommendations.size());

                return fallbackRecommendations;
            } else {
                throw new RuntimeException("머신러닝 추천 실패 및 폴백 비활성화", e);
            }
        }
    }

    /**
     * 머신러닝 추천 시도
     */
    private List<RecommendedCar> attemptMachineLearningRecommendation(Long userId, int topK) {
        try {
            // 1. 후보 차량 생성
            List<Car> candidateCars = generateOptimizedCandidateCars(userId);

            if (candidateCars.isEmpty()) {
                log.warn("후보 차량 없음 - 사용자: {}", userId);
                return Collections.emptyList();
            }

            log.info("머신러닝 추천 시도 - 사용자: {}, 후보차량: {}개", userId, candidateCars.size());

            // 2. 사용자 즐겨찾기 조회
            List<Long> favoriteCarIds = getUserFavoriteCarIds(userId);

            // 3. 후보 차량을 AI 서비스 형식으로 변환
            List<Map<String, Object>> candidateCarData = candidateCars.stream()
                    .map(this::convertCarToAIFormat)
                    .collect(Collectors.toList());

            // 4. AI 서버에 머신러닝 요청
            AIServiceClient.AIRecommendationResponse response = aiServiceClient.getRecommendationsWithCandidates(
                    userId,
                    favoriteCarIds,
                    candidateCarData,
                    favoriteCarIds,  // exclude 목록
                    topK
            );

            // 5. 응답 처리
            if (response == null) {
                log.warn("AI 서버 응답이 null - 사용자: {}", userId);
                return Collections.emptyList();
            }

            if (response.getRecommendations() == null || response.getRecommendations().isEmpty()) {
                log.warn("AI 서버 추천 결과 없음 - 사용자: {}", userId);
                return Collections.emptyList();
            }

            // 6. 결과 변환 및 후처리
            List<RecommendedCar> recommendations = response.getRecommendations().stream()
                    .map(this::convertAIRecommendationToRecommendedCar)
                    .filter(Objects::nonNull)
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("머신러닝 추천 성공 - 사용자: {}, 결과: {}개", userId, recommendations.size());
            return recommendations;

        } catch (Exception e) {
            log.error("머신러닝 추천 시도 중 오류 - 사용자: {}", userId, e);
            throw e;
        }
    }

    /**
     * 차량을 AI 서비스 형식으로 변환 (개선된 버전)
     */
    private Map<String, Object> convertCarToAIFormat(Car car) {
        Map<String, Object> carData = new HashMap<>();

        // 기본 정보
        carData.put("id", car.getId());
        carData.put("model", car.getModel() != null ? car.getModel() : "");
        carData.put("year", car.getYear() != null ? car.getYear() : "2020");
        carData.put("price", car.getPrice() != null ? car.getPrice() : 2000);
        carData.put("mileage", car.getMileage() != null ? car.getMileage() : 50000);
        carData.put("fuel", car.getFuel() != null ? car.getFuel() : "가솔린");
        carData.put("region", car.getRegion() != null ? car.getRegion() : "서울");
        carData.put("carType", car.getCarType() != null ? car.getCarType() : "중형차");

        // 파생 정보
        carData.put("brand", extractBrand(car.getModel()));

        // 연도 추출
        int year = extractYear(car.getYear());
        carData.put("year_numeric", year);
        carData.put("age", 2024 - year);

        return carData;
    }

    /**
     * 연식에서 연도 추출 (예: "22/01식" -> 2022)
     */
    private int extractYear(String yearStr) {
        if (yearStr == null || yearStr.trim().isEmpty()) {
            return 2020; // 기본값
        }

        try {
            // 숫자만 추출
            String digits = yearStr.replaceAll("[^0-9]", "");

            if (digits.length() >= 4) {
                // 4자리 연도 (예: "2023")
                return Integer.parseInt(digits.substring(0, 4));
            } else if (digits.length() >= 2) {
                // 2자리 연도 (예: "23")
                int year = Integer.parseInt(digits.substring(0, 2));
                return year > 50 ? 1900 + year : 2000 + year;
            }
        } catch (NumberFormatException e) {
            log.debug("연식 파싱 실패: {}", yearStr);
        }

        return 2020; // 파싱 실패 시 기본값
    }

    /**
     * 기존 방식 추천
     */
    private List<RecommendedCar> getLegacyRecommendations(Long userId, int topK) {
        long startTime = System.currentTimeMillis();

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            List<Favorite> favorites = favoriteRepository.findByUser(user);

            if (favorites.isEmpty()) {
                return getPopularCarsRecommendation(topK);
            }

            // 기존 로직 활용
            List<Long> favoriteCarIds = favorites.stream()
                    .map(favorite -> favorite.getCar().getId())
                    .collect(Collectors.toList());

            List<Long> weightedFavoriteIds = getWeightedFavoriteIds(favorites);

            // 기존 AI 서버 호출
            AIServiceClient.AIRecommendationResponse response =
                    aiServiceClient.getRecommendations(weightedFavoriteIds, favoriteCarIds, topK);

            List<RecommendedCar> recommendations;

            if (response == null || response.getRecommendations().isEmpty()) {
                recommendations = getHybridRecommendation(userId, favoriteCarIds, topK);
            } else {
                recommendations = response.getRecommendations().stream()
                        .map(this::convertAIRecommendationToRecommendedCar)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("기존 방식 추천 완료 - 사용자: {}, 결과: {}개, 소요시간: {}ms",
                    userId, recommendations.size(), duration);

            recordRecommendationMetrics(userId, "legacy", true, duration, recommendations.size());

            return recommendations;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("기존 방식 추천 실패 - 사용자: {}, 소요시간: {}ms", userId, duration, e);

            recordRecommendationMetrics(userId, "legacy", false, duration, 0);

            // 최후 수단: 인기 차량
            return getPopularCarsRecommendation(topK);
        }
    }

    /**
     * 사용자의 즐겨찾기 차량 ID 조회
     */
    private List<Long> getUserFavoriteCarIds(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }

        return favoriteRepository.findByUser(user).stream()
                .map(favorite -> favorite.getCar().getId())
                .collect(Collectors.toList());
    }

    /**
     * 최적화된 후보 차량 생성
     */
    private List<Car> generateOptimizedCandidateCars(Long userId) {
        // 사용자 즐겨찾기 제외
        Set<Long> favoriteCarIds = getUserFavoriteCarIds(userId).stream()
                .collect(Collectors.toSet());

        // 스마트 필터링
        return carRepository.findAll().stream()
                .filter(car -> !favoriteCarIds.contains(car.getId()))
                .filter(this::isValidCarData)
                .filter(this::isRecentOrPopular)
                .sorted((a, b) -> {
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return Long.compare(b.getId(), a.getId());
                })
                .limit(1000)
                .collect(Collectors.toList());
    }

    /**
     * 최근 또는 인기 차량 필터
     */
    private boolean isRecentOrPopular(Car car) {
        // 최근 30일 내 등록
        if (car.getCreatedAt() != null) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
            if (car.getCreatedAt().isAfter(thirtyDaysAgo)) {
                return true;
            }
        }

        // 인기 가격대
        if (car.getPrice() != null && car.getPrice() >= 1000 && car.getPrice() <= 5000) {
            return true;
        }

        // 인기 브랜드
        String brand = extractBrand(car.getModel());
        return Arrays.asList("현대", "기아", "제네시스", "BMW", "벤츠").contains(brand);
    }

    /**
     * 하이브리드 추천 (AI 실패 시 대안)
     */
    private List<RecommendedCar> getHybridRecommendation(Long userId, List<Long> favoriteCarIds, int topK) {
        try {
            List<Car> candidateCars = carRepository.findAll().stream()
                    .filter(car -> !favoriteCarIds.contains(car.getId()))
                    .filter(this::isValidCarData)
                    .limit(topK * 2)
                    .collect(Collectors.toList());

            return candidateCars.stream()
                    .map(car -> new RecommendedCar(car, 0.5, "하이브리드 추천"))
                    .limit(topK)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("하이브리드 추천 실패 - 사용자: {}", userId, e);
            return getPopularCarsRecommendation(topK);
        }
    }

    /**
     * 인기 차량 추천
     */
    private List<RecommendedCar> getPopularCarsRecommendation(int topK) {
        return carRepository.findAll().stream()
                .filter(this::isValidCarData)
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(topK)
                .map(car -> new RecommendedCar(car, 0.5, "인기 차량 추천"))
                .collect(Collectors.toList());
    }

    /**
     * 가중치가 적용된 즐겨찾기 ID 리스트
     */
    private List<Long> getWeightedFavoriteIds(List<Favorite> favorites) {
        return favorites.stream()
                .sorted((f1, f2) -> f2.getCreatedAt().compareTo(f1.getCreatedAt()))
                .map(favorite -> favorite.getCar().getId())
                .collect(Collectors.toList());
    }

    /**
     * AI 추천 결과 변환 (에러 처리 강화)
     */
    private RecommendedCar convertAIRecommendationToRecommendedCar(AIServiceClient.RecommendationItem item) {
        try {
            if (item == null || item.getCar() == null) {
                log.warn("추천 아이템이 null입니다");
                return null;
            }

            Map<String, Object> carData = item.getCar();

            // ID 추출 (여러 형태 지원)
            Object idObj = carData.get("id");
            Long carId;

            if (idObj instanceof Number) {
                carId = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    carId = Long.valueOf((String) idObj);
                } catch (NumberFormatException e) {
                    log.warn("잘못된 차량 ID 형식: {}", idObj);
                    return null;
                }
            } else {
                log.warn("알 수 없는 차량 ID 타입: {}", idObj);
                return null;
            }

            // 차량 조회
            Optional<Car> carOpt = carRepository.findById(carId);
            if (carOpt.isPresent()) {
                double similarityScore = item.getSimilarityScore();
                String reason = item.getRecommendationReason();

                // 점수 정규화 (0-1 범위)
                if (similarityScore > 1.0) {
                    similarityScore = similarityScore / 5.0;  // 5점 만점을 1점 만점으로
                }

                return new RecommendedCar(
                        carOpt.get(),
                        Math.max(0.0, Math.min(1.0, similarityScore)),
                        reason != null ? reason : "머신러닝 추천"
                );
            } else {
                log.warn("추천된 차량 ID {}를 데이터베이스에서 찾을 수 없습니다", carId);
                return null;
            }

        } catch (Exception e) {
            log.error("AI 추천 결과 변환 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 메트릭 기록 (모니터링용)
     */
    private void recordRecommendationMetrics(Long userId, String method, boolean success,
                                             long duration, int resultCount) {
        log.info("METRICS - 사용자: {}, 방식: {}, 성공: {}, 소요시간: {}ms, 결과수: {}",
                userId, method, success, duration, resultCount);
    }

    /**
     * 개발자용 디버그 API
     */
    public Map<String, Object> getRecommendationDebugInfo(Long userId) {
        Map<String, Object> debugInfo = new HashMap<>();

        debugInfo.put("userId", userId);
        debugInfo.put("globalMachineLearningEnabled", useMachineLearning);
        debugInfo.put("fallbackEnabled", fallbackEnabled);
        debugInfo.put("abTestEnabled", abTestEnabled);
        debugInfo.put("targetUsersEnabled", targetUsersEnabled);
        debugInfo.put("shouldUseMachineLearning", shouldUseMachineLearningForUser(userId));
        debugInfo.put("environment", getCurrentProfile());

        if (abTestEnabled) {
            double hash = Math.abs(userId.hashCode() % 100) / 100.0;
            debugInfo.put("abTestHash", hash);
            debugInfo.put("machineLearningRatio", machineLearningRatio);
            debugInfo.put("inMachineLearningGroup", hash < machineLearningRatio);
        }

        if (targetUsersEnabled && !whitelistUsers.isEmpty()) {
            debugInfo.put("inWhitelist", whitelistUsers.contains(userId));
            debugInfo.put("whitelistSize", whitelistUsers.size());
        }

        return debugInfo;
    }

    // 유틸리티 메서드들
    private boolean isValidCarData(Car car) {
        return car.getModel() != null && !car.getModel().trim().isEmpty() &&
                car.getPrice() != null && car.getPrice() != 9999 && car.getPrice() > 0 &&
                car.getFuel() != null && !car.getFuel().trim().isEmpty();
    }

    /**
     * 모델명에서 브랜드 추출
     */
    private String extractBrand(String model) {
        if (model == null || model.trim().isEmpty()) {
            return "기타";
        }

        // 주요 브랜드 목록
        String[] brands = {"현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디", "볼보", "폭스바겐"};

        for (String brand : brands) {
            if (model.contains(brand)) {
                return brand;
            }
        }

        // 브랜드를 찾지 못한 경우 첫 번째 단어 반환
        String[] parts = model.split("\\s+");
        return parts.length > 0 ? parts[0] : "기타";
    }

    private String getCurrentProfile() {
        return Arrays.toString(environment.getActiveProfiles());
    }

    // 즐겨찾기 변경 시 캐시 무효화
    public void onFavoriteChanged(Long userId) {
        recommendationCache.remove(userId);
        log.info("즐겨찾기 변경으로 캐시 무효화 - 사용자: {}", userId);
    }

    // AI 서비스 상태 확인
    public boolean isAIServiceAvailable() {
        return aiServiceClient.isAIServiceHealthy();
    }

    // 내부 클래스
    public static class RecommendedCar {
        private final Car car;
        private final double similarityScore;
        private final String recommendationReason;

        public RecommendedCar(Car car, double similarityScore, String recommendationReason) {
            this.car = car;
            this.similarityScore = similarityScore;
            this.recommendationReason = recommendationReason;
        }

        public Car getCar() { return car; }
        public double getSimilarityScore() { return similarityScore; }
        public String getRecommendationReason() { return recommendationReason; }
    }

    private static class CachedRecommendation {
        private final List<RecommendedCar> recommendations;
        private final LocalDateTime timestamp;

        public CachedRecommendation(List<RecommendedCar> recommendations, LocalDateTime timestamp) {
            this.recommendations = new ArrayList<>(recommendations);
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return timestamp.isBefore(LocalDateTime.now().minus(CACHE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        }

        public List<RecommendedCar> getRecommendations() {
            return new ArrayList<>(recommendations);
        }
    }
}