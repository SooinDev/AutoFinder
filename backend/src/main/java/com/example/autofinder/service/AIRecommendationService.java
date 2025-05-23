package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.Favorite;
import com.example.autofinder.model.User;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.FavoriteRepository;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final AIServiceClient aiServiceClient;
    private final CarRepository carRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    // 추천 캐시 (메모리에 일시적으로 저장)
    private final Map<Long, CachedRecommendation> recommendationCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MINUTES = 5; // 5분으로 단축

    // 마지막 모델 학습 시간 추적
    private LocalDateTime lastModelTrainingTime = null;
    private LocalDateTime lastCarDataUpdateTime = null;

    /**
     * 애플리케이션 시작 시 AI 모델 학습
     */
    @PostConstruct
    public void initializeAIModel() {
        log.info("AI 추천 시스템 초기화 시작...");
        trainAIModelAsync();
    }

    /**
     * 비동기적으로 AI 모델 학습 (개선된 버전)
     */
    @Async
    public void trainAIModelAsync() {
        try {
            log.info("개선된 AI 모델 학습 시작...");

            // 모든 차량 데이터 조회
            List<Car> allCars = carRepository.findAll();

            if (allCars.isEmpty()) {
                log.warn("학습할 차량 데이터가 없습니다.");
                return;
            }

            // 데이터 품질 검증 및 필터링
            List<Car> validCars = allCars.stream()
                    .filter(this::isValidCarData)
                    .collect(Collectors.toList());

            log.info("전체 차량: {}, 유효한 차량: {}", allCars.size(), validCars.size());

            // 차량 데이터를 AI 서비스 형식으로 변환
            List<Object> carsData = validCars.stream()
                    .map(this::convertCarToEnhancedAIFormat)
                    .collect(Collectors.toList());

            // AI 서비스에 학습 요청
            boolean success = aiServiceClient.trainModel(carsData);

            if (success) {
                log.info("AI 모델 학습 완료: {} 개의 차량 데이터로 학습", validCars.size());
                lastModelTrainingTime = LocalDateTime.now();
                lastCarDataUpdateTime = LocalDateTime.now();
                // 모델 재학습 시 모든 캐시 삭제
                clearAllCache();
            } else {
                log.error("AI 모델 학습 실패");
            }

        } catch (Exception e) {
            log.error("AI 모델 학습 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 새벽 2시에 AI 모델 재학습
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Async
    public void scheduleModelRetraining() {
        log.info("스케줄된 AI 모델 재학습 시작");
        trainAIModelAsync();
    }

    /**
     * 사용자별 차량 추천 (실시간 업데이트 버전)
     */
    public List<RecommendedCar> getRecommendationsForUser(Long userId, int topK) {
        return getRecommendationsForUser(userId, topK, false);
    }

    /**
     * 사용자별 차량 추천 (강제 새로고침 옵션 포함)
     */
    public List<RecommendedCar> getRecommendationsForUser(Long userId, int topK, boolean forceRefresh) {
        try {
            // 강제 새로고침이거나 캐시가 만료된 경우
            if (forceRefresh || shouldRefreshRecommendations(userId)) {
                log.info("사용자 {}의 추천을 새로 생성합니다. (강제새로고침: {})", userId, forceRefresh);
                clearUserCache(userId);
                return generateFreshRecommendations(userId, topK);
            }

            // 캐시 확인
            CachedRecommendation cached = recommendationCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                log.info("캐시된 추천 결과 반환 for user: {}", userId);
                return cached.getRecommendations().stream()
                        .limit(topK)
                        .collect(Collectors.toList());
            }

            // 캐시가 없거나 만료된 경우 새로 생성
            return generateFreshRecommendations(userId, topK);

        } catch (Exception e) {
            log.error("사용자 {} 추천 생성 중 오류: {}", userId, e.getMessage(), e);
            return getEnhancedPopularCarsRecommendation(topK);
        }
    }

    /**
     * 새로운 추천 생성
     */
    private List<RecommendedCar> generateFreshRecommendations(Long userId, int topK) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<Favorite> favorites = favoriteRepository.findByUser(user);

        if (favorites.isEmpty()) {
            log.info("사용자 {}의 즐겨찾기가 없어 인기 차량 추천을 제공합니다.", userId);
            List<RecommendedCar> recommendations = getEnhancedPopularCarsRecommendation(topK);
            cacheRecommendations(userId, recommendations);
            return recommendations;
        }

        // 데이터 변경 확인 후 필요시 모델 재학습
        checkAndUpdateModelIfNeeded();

        // 즐겨찾기 분석
        FavoriteAnalysis analysis = analyzeFavoritePatterns(favorites);

        // 즐겨찾기한 차량 ID 리스트
        List<Long> favoriteCarIds = favorites.stream()
                .map(favorite -> favorite.getCar().getId())
                .collect(Collectors.toList());

        // 가중치가 적용된 즐겨찾기 리스트
        List<Long> weightedFavoriteIds = getWeightedFavoriteIds(favorites);

        // AI 서비스에 추천 요청
        AIServiceClient.AIRecommendationResponse response = aiServiceClient.getRecommendations(
                weightedFavoriteIds, favoriteCarIds, Math.max(topK * 3, 30) // 더 많은 후보 요청
        );

        List<RecommendedCar> recommendations;

        if (response == null || response.getRecommendations().isEmpty()) {
            log.warn("AI 추천 결과가 없어 하이브리드 추천을 제공합니다.");
            recommendations = getHybridRecommendation(analysis, favoriteCarIds, topK);
        } else {
            // AI 추천 결과를 RecommendedCar 객체로 변환
            recommendations = response.getRecommendations().stream()
                    .map(this::convertAIRecommendationToRecommendedCar)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 다양성 개선
            recommendations = improveDiversity(recommendations, analysis);

            // 최종 필터링 및 정렬
            recommendations = applyFinalFiltering(recommendations, analysis, topK);
        }

        // 캐시에 저장
        cacheRecommendations(userId, recommendations);

        return recommendations.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 추천 새로고침이 필요한지 확인
     */
    private boolean shouldRefreshRecommendations(Long userId) {
        CachedRecommendation cached = recommendationCache.get(userId);

        // 캐시가 없으면 새로고침 필요
        if (cached == null) {
            return true;
        }

        // 캐시가 만료되었으면 새로고침 필요
        if (cached.isExpired()) {
            return true;
        }

        // 사용자의 즐겨찾기가 변경되었는지 확인
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            List<Favorite> currentFavorites = favoriteRepository.findByUser(user);
            if (currentFavorites.size() != cached.getFavoriteCount()) {
                log.info("사용자 {}의 즐겨찾기 개수가 변경됨: {} -> {}",
                        userId, cached.getFavoriteCount(), currentFavorites.size());
                return true;
            }

            // 즐겨찾기 목록이 변경되었는지 확인
            Set<Long> cachedFavoriteIds = cached.getFavoriteCarIds();
            Set<Long> currentFavoriteIds = currentFavorites.stream()
                    .map(f -> f.getCar().getId())
                    .collect(Collectors.toSet());

            if (!cachedFavoriteIds.equals(currentFavoriteIds)) {
                log.info("사용자 {}의 즐겨찾기 목록이 변경됨", userId);
                return true;
            }
        }

        return false;
    }

    /**
     * 데이터 변경 확인 후 모델 업데이트
     */
    private void checkAndUpdateModelIfNeeded() {
        // 새로운 차량이 추가되었는지 확인 (간단한 방식)
        List<Car> recentCars = carRepository.findAll().stream()
                .filter(car -> lastCarDataUpdateTime == null ||
                        (car.getCreatedAt() != null && car.getCreatedAt().isAfter(lastCarDataUpdateTime)))
                .collect(Collectors.toList());

        if (!recentCars.isEmpty()) {
            log.info("새로운 차량 {}대가 추가되어 모델을 업데이트합니다.", recentCars.size());
            trainAIModelAsync();
        }
    }

    /**
     * 특정 사용자 캐시 삭제
     */
    public void clearUserCache(Long userId) {
        recommendationCache.remove(userId);
        log.info("사용자 {} 캐시 삭제됨", userId);
    }

    /**
     * 모든 캐시 삭제
     */
    public void clearAllCache() {
        recommendationCache.clear();
        log.info("모든 추천 캐시 삭제됨");
    }

    /**
     * 사용자 즐겨찾기 변경 시 호출 (FavoriteService에서 호출)
     */
    public void onFavoriteChanged(Long userId) {
        clearUserCache(userId);
        log.info("사용자 {}의 즐겨찾기 변경으로 캐시 무효화", userId);
    }

    /**
     * 사용자 선호도 분석 (개선된 버전)
     */
    public Map<String, Object> analyzeUserPreferences(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            List<Favorite> favorites = favoriteRepository.findByUser(user);

            if (favorites.isEmpty()) {
                return Map.of("message", "즐겨찾기한 차량이 없어 선호도 분석을 할 수 없습니다.");
            }

            // 기본 AI 분석
            List<Long> favoriteCarIds = favorites.stream()
                    .map(favorite -> favorite.getCar().getId())
                    .collect(Collectors.toList());

            AIServiceClient.UserPreferenceAnalysis aiAnalysis = aiServiceClient.analyzeUserPreferences(favoriteCarIds);

            // 로컬 상세 분석
            FavoriteAnalysis localAnalysis = analyzeFavoritePatterns(favorites);

            Map<String, Object> enhancedAnalysis = new HashMap<>();

            if (aiAnalysis != null && aiAnalysis.getAnalysis() != null) {
                enhancedAnalysis.putAll(aiAnalysis.getAnalysis());
            }

            // 로컬 분석 결과 추가
            Map<String, Object> localAnalysisMap = new HashMap<>();
            localAnalysisMap.put("consistency_score", localAnalysis.getConsistencyScore());
            localAnalysisMap.put("price_trend", localAnalysis.getPriceTrend());
            localAnalysisMap.put("year_trend", localAnalysis.getYearTrend());
            localAnalysisMap.put("preferred_categories", localAnalysis.getPreferredCategories());
            localAnalysisMap.put("recommendation_confidence", localAnalysis.getRecommendationConfidence());

            enhancedAnalysis.put("local_analysis", localAnalysisMap);
            enhancedAnalysis.put("analysis_version", "enhanced_v2.0");
            enhancedAnalysis.put("last_updated", LocalDateTime.now().toString());

            return enhancedAnalysis;

        } catch (Exception e) {
            log.error("사용자 {} 선호도 분석 중 오류: {}", userId, e.getMessage(), e);
            return Map.of("error", "선호도 분석 중 오류가 발생했습니다.");
        }
    }

    // 이하 기존 메서드들 (analyzeFavoritePatterns, getWeightedFavoriteIds 등)은 동일하므로 생략...
    // [기존 메서드들을 여기에 포함]

    /**
     * 즐겨찾기 패턴 분석
     */
    private FavoriteAnalysis analyzeFavoritePatterns(List<Favorite> favorites) {
        List<Car> cars = favorites.stream()
                .map(Favorite::getCar)
                .collect(Collectors.toList());

        FavoriteAnalysis analysis = new FavoriteAnalysis();

        // 가격 패턴 분석
        List<Long> prices = cars.stream()
                .map(Car::getPrice)
                .filter(Objects::nonNull)
                .filter(price -> price != 9999)
                .collect(Collectors.toList());

        if (!prices.isEmpty()) {
            analysis.setAvgPrice(prices.stream().mapToLong(Long::longValue).average().orElse(0));
            analysis.setPriceRange(Collections.max(prices) - Collections.min(prices));

            double priceStd = calculateStandardDeviation(prices);
            analysis.setConsistencyScore(Math.max(0, 1 - (priceStd / analysis.getAvgPrice())));
        }

        // 연식 패턴 분석
        List<Integer> years = cars.stream()
                .map(car -> extractYear(car.getYear()))
                .filter(year -> year > 0)
                .collect(Collectors.toList());

        if (!years.isEmpty()) {
            analysis.setAvgYear(years.stream().mapToInt(Integer::intValue).average().orElse(0));
            analysis.setYearRange(Collections.max(years) - Collections.min(years));
        }

        // 브랜드/모델 선호도
        Map<String, Long> modelCounts = cars.stream()
                .map(Car::getModel)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        model -> extractBrand(model),
                        Collectors.counting()
                ));

        analysis.setPreferredBrands(modelCounts);

        // 연료 타입 선호도
        Map<String, Long> fuelCounts = cars.stream()
                .map(Car::getFuel)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(fuel -> fuel, Collectors.counting()));

        analysis.setPreferredFuelTypes(fuelCounts);

        // 추천 신뢰도 계산
        double confidence = calculateRecommendationConfidence(analysis, cars.size());
        analysis.setRecommendationConfidence(confidence);

        return analysis;
    }

    /**
     * 가중치가 적용된 즐겨찾기 ID 리스트 생성
     */
    private List<Long> getWeightedFavoriteIds(List<Favorite> favorites) {
        return favorites.stream()
                .sorted((f1, f2) -> f2.getCreatedAt().compareTo(f1.getCreatedAt()))
                .map(favorite -> favorite.getCar().getId())
                .collect(Collectors.toList());
    }

    /**
     * 하이브리드 추천 (AI + 룰 기반)
     */
    private List<RecommendedCar> getHybridRecommendation(FavoriteAnalysis analysis, List<Long> excludeIds, int topK) {
        List<RecommendedCar> recommendations = new ArrayList<>();

        if (analysis.getAvgPrice() > 0) {
            double priceMargin = 0.3;
            Long minPrice = (long) (analysis.getAvgPrice() * (1 - priceMargin));
            Long maxPrice = (long) (analysis.getAvgPrice() * (1 + priceMargin));

            List<Car> similarPriceCars = carRepository.findAll().stream()
                    .filter(car -> !excludeIds.contains(car.getId()))
                    .filter(car -> car.getPrice() != null && car.getPrice() != 9999)
                    .filter(car -> car.getPrice() >= minPrice && car.getPrice() <= maxPrice)
                    .limit(topK / 2)
                    .collect(Collectors.toList());

            for (Car car : similarPriceCars) {
                recommendations.add(new RecommendedCar(car, 0.7, "유사한 가격대 추천"));
            }
        }

        String preferredBrand = analysis.getPreferredBrands().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        if (!preferredBrand.isEmpty()) {
            List<Car> brandCars = carRepository.findAll().stream()
                    .filter(car -> !excludeIds.contains(car.getId()))
                    .filter(car -> extractBrand(car.getModel()).equals(preferredBrand))
                    .limit(topK / 2)
                    .collect(Collectors.toList());

            for (Car car : brandCars) {
                recommendations.add(new RecommendedCar(car, 0.6, "선호 브랜드 추천"));
            }
        }

        return recommendations.stream()
                .distinct()
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 추천 다양성 개선
     */
    private List<RecommendedCar> improveDiversity(List<RecommendedCar> recommendations, FavoriteAnalysis analysis) {
        List<RecommendedCar> diversifiedList = new ArrayList<>();
        Set<String> usedBrands = new HashSet<>();
        Set<String> usedPriceRanges = new HashSet<>();

        for (RecommendedCar rec : recommendations) {
            String brand = extractBrand(rec.getCar().getModel());
            String priceRange = getPriceRange(rec.getCar().getPrice());

            if (diversifiedList.size() < 3 ||
                    (!usedBrands.contains(brand) || !usedPriceRanges.contains(priceRange))) {
                diversifiedList.add(rec);
                usedBrands.add(brand);
                usedPriceRanges.add(priceRange);
            }

            if (diversifiedList.size() >= recommendations.size()) {
                break;
            }
        }

        return diversifiedList;
    }

    /**
     * 최종 필터링 및 정렬
     */
    private List<RecommendedCar> applyFinalFiltering(List<RecommendedCar> recommendations,
                                                     FavoriteAnalysis analysis, int topK) {
        return recommendations.stream()
                .filter(rec -> rec.getCar().getPrice() != null && rec.getCar().getPrice() != 9999)
                .sorted((r1, r2) -> {
                    int scoreCompare = Double.compare(r2.getSimilarityScore(), r1.getSimilarityScore());
                    if (scoreCompare != 0) return scoreCompare;

                    double price1Diff = Math.abs(r1.getCar().getPrice() - analysis.getAvgPrice());
                    double price2Diff = Math.abs(r2.getCar().getPrice() - analysis.getAvgPrice());
                    return Double.compare(price1Diff, price2Diff);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 개선된 인기 차량 추천
     */
    private List<RecommendedCar> getEnhancedPopularCarsRecommendation(int topK) {
        List<Car> recentCars = carRepository.findAll().stream()
                .filter(car -> car.getCreatedAt() != null)
                .filter(car -> car.getCreatedAt().isAfter(LocalDateTime.now().minus(30, ChronoUnit.DAYS)))
                .filter(car -> car.getPrice() != null && car.getPrice() != 9999)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(topK * 2)
                .collect(Collectors.toList());

        if (recentCars.size() < topK) {
            List<Car> allCars = carRepository.findAll().stream()
                    .filter(car -> car.getPrice() != null && car.getPrice() != 9999)
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .limit(topK)
                    .collect(Collectors.toList());
            recentCars.addAll(allCars);
        }

        return recentCars.stream()
                .distinct()
                .limit(topK)
                .map(car -> new RecommendedCar(car, 0.5, "인기 차량 추천"))
                .collect(Collectors.toList());
    }

    /**
     * 데이터 유효성 검증
     */
    private boolean isValidCarData(Car car) {
        return car.getModel() != null && !car.getModel().trim().isEmpty() &&
                car.getPrice() != null && car.getPrice() != 9999 && car.getPrice() > 0 &&
                car.getYear() != null && !car.getYear().trim().isEmpty() &&
                car.getFuel() != null && !car.getFuel().trim().isEmpty();
    }

    /**
     * 개선된 차량 데이터 변환
     */
    private Map<String, Object> convertCarToEnhancedAIFormat(Car car) {
        Map<String, Object> carData = new HashMap<>();
        carData.put("id", car.getId());
        carData.put("model", car.getModel());
        carData.put("year", car.getYear());
        carData.put("price", car.getPrice());
        carData.put("mileage", car.getMileage() != null ? car.getMileage() : 0);
        carData.put("fuel", car.getFuel());
        carData.put("region", car.getRegion());
        carData.put("carType", car.getCarType());

        carData.put("brand", extractBrand(car.getModel()));
        carData.put("price_range", getPriceRange(car.getPrice()));
        carData.put("car_age", 2024 - extractYear(car.getYear()));

        return carData;
    }

    /**
     * 캐시 관리 (개선된 버전)
     */
    private void cacheRecommendations(Long userId, List<RecommendedCar> recommendations) {
        // 현재 즐겨찾기 정보도 함께 캐시
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            List<Favorite> favorites = favoriteRepository.findByUser(user);
            Set<Long> favoriteCarIds = favorites.stream()
                    .map(f -> f.getCar().getId())
                    .collect(Collectors.toSet());

            recommendationCache.put(userId, new CachedRecommendation(
                    recommendations,
                    LocalDateTime.now(),
                    favorites.size(),
                    favoriteCarIds
            ));
        } else {
            recommendationCache.put(userId, new CachedRecommendation(
                    recommendations,
                    LocalDateTime.now(),
                    0,
                    new HashSet<>()
            ));
        }

        // 캐시 크기 제한
        if (recommendationCache.size() > 1000) {
            LocalDateTime cutoff = LocalDateTime.now().minus(CACHE_EXPIRY_MINUTES, ChronoUnit.MINUTES);
            recommendationCache.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));
        }
    }

    // 유틸리티 메서드들
    private String extractBrand(String model) {
        if (model == null) return "";
        String[] brands = {"현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디"};
        for (String brand : brands) {
            if (model.contains(brand)) return brand;
        }
        String[] parts = model.split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private String getPriceRange(Long price) {
        if (price == null) return "알 수 없음";
        if (price < 1000) return "1천만원 미만";
        if (price < 3000) return "1천-3천만원";
        if (price < 5000) return "3천-5천만원";
        return "5천만원 이상";
    }

    private int extractYear(String yearStr) {
        if (yearStr == null) return 0;
        String digits = yearStr.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return Integer.parseInt(digits.substring(0, 4));
        } else if (digits.length() == 2) {
            int year = Integer.parseInt(digits);
            return year > 50 ? 1900 + year : 2000 + year;
        }
        return 0;
    }

    private double calculateStandardDeviation(List<Long> values) {
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double calculateRecommendationConfidence(FavoriteAnalysis analysis, int sampleSize) {
        double baseConfidence = Math.min(sampleSize / 10.0, 1.0);
        double consistencyBonus = analysis.getConsistencyScore() * 0.3;
        return Math.min(baseConfidence + consistencyBonus, 1.0);
    }

    /**
     * AI 추천 결과를 RecommendedCar로 변환
     */
    private RecommendedCar convertAIRecommendationToRecommendedCar(AIServiceClient.RecommendationItem item) {
        try {
            Map<String, Object> carData = item.getCar();
            Long carId = Long.valueOf(carData.get("id").toString());

            Optional<Car> carOpt = carRepository.findById(carId);
            if (carOpt.isPresent()) {
                return new RecommendedCar(
                        carOpt.get(),
                        item.getSimilarityScore(),
                        item.getRecommendationReason()
                );
            } else {
                log.warn("추천된 차량 ID {}를 데이터베이스에서 찾을 수 없습니다.", carId);
                return null;
            }
        } catch (Exception e) {
            log.error("AI 추천 결과 변환 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * AI 서비스 상태 확인
     */
    public boolean isAIServiceAvailable() {
        return aiServiceClient.isAIServiceHealthy();
    }

    // 내부 클래스들
    private static class FavoriteAnalysis {
        private double avgPrice;
        private double priceRange;
        private double avgYear;
        private double yearRange;
        private double consistencyScore;
        private String priceTrend;
        private String yearTrend;
        private Map<String, Long> preferredBrands = new HashMap<>();
        private Map<String, Long> preferredFuelTypes = new HashMap<>();
        private Map<String, String> preferredCategories = new HashMap<>();
        private double recommendationConfidence;

        // Getters and setters
        public double getAvgPrice() { return avgPrice; }
        public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }

        public double getPriceRange() { return priceRange; }
        public void setPriceRange(double priceRange) { this.priceRange = priceRange; }

        public double getAvgYear() { return avgYear; }
        public void setAvgYear(double avgYear) { this.avgYear = avgYear; }

        public double getYearRange() { return yearRange; }
        public void setYearRange(double yearRange) { this.yearRange = yearRange; }

        public double getConsistencyScore() { return consistencyScore; }
        public void setConsistencyScore(double consistencyScore) { this.consistencyScore = consistencyScore; }

        public String getPriceTrend() { return priceTrend; }
        public void setPriceTrend(String priceTrend) { this.priceTrend = priceTrend; }

        public String getYearTrend() { return yearTrend; }
        public void setYearTrend(String yearTrend) { this.yearTrend = yearTrend; }

        public Map<String, Long> getPreferredBrands() { return preferredBrands; }
        public void setPreferredBrands(Map<String, Long> preferredBrands) { this.preferredBrands = preferredBrands; }

        public Map<String, Long> getPreferredFuelTypes() { return preferredFuelTypes; }
        public void setPreferredFuelTypes(Map<String, Long> preferredFuelTypes) { this.preferredFuelTypes = preferredFuelTypes; }

        public Map<String, String> getPreferredCategories() { return preferredCategories; }
        public void setPreferredCategories(Map<String, String> preferredCategories) { this.preferredCategories = preferredCategories; }

        public double getRecommendationConfidence() { return recommendationConfidence; }
        public void setRecommendationConfidence(double recommendationConfidence) { this.recommendationConfidence = recommendationConfidence; }
    }

    private static class CachedRecommendation {
        private final List<RecommendedCar> recommendations;
        private final LocalDateTime timestamp;
        private final int favoriteCount;
        private final Set<Long> favoriteCarIds;

        public CachedRecommendation(List<RecommendedCar> recommendations, LocalDateTime timestamp,
                                    int favoriteCount, Set<Long> favoriteCarIds) {
            this.recommendations = new ArrayList<>(recommendations);
            this.timestamp = timestamp;
            this.favoriteCount = favoriteCount;
            this.favoriteCarIds = new HashSet<>(favoriteCarIds);
        }

        public boolean isExpired() {
            return timestamp.isBefore(LocalDateTime.now().minus(CACHE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        }

        public List<RecommendedCar> getRecommendations() {
            return new ArrayList<>(recommendations);
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public Set<Long> getFavoriteCarIds() {
            return new HashSet<>(favoriteCarIds);
        }
    }

    /**
     * 추천 차량 정보를 담는 내부 클래스
     */
    public static class RecommendedCar {
        private final Car car;
        private final double similarityScore;
        private final String recommendationReason;

        public RecommendedCar(Car car, double similarityScore, String recommendationReason) {
            this.car = car;
            this.similarityScore = similarityScore;
            this.recommendationReason = recommendationReason;
        }

        public Car getCar() {
            return car;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public String getRecommendationReason() {
            return recommendationReason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecommendedCar that = (RecommendedCar) o;
            return Objects.equals(car.getId(), that.car.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(car.getId());
        }
    }
}