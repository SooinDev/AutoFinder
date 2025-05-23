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

    /**
     * 애플리케이션 시작 시 AI 모델 학습
     */
    @PostConstruct
    public void initializeAIModel() {
        trainAIModelAsync();
    }

    /**
     * 비동기적으로 AI 모델 학습
     */
    @Async
    public void trainAIModelAsync() {
        try {
            log.info("AI 모델 학습 시작...");

            // 모든 차량 데이터 조회
            List<Car> allCars = carRepository.findAll();

            if (allCars.isEmpty()) {
                log.warn("학습할 차량 데이터가 없습니다.");
                return;
            }

            // 차량 데이터를 AI 서비스 형식으로 변환
            List<Object> carsData = allCars.stream()
                    .map(this::convertCarToAIFormat)
                    .collect(Collectors.toList());

            // AI 서비스에 학습 요청
            boolean success = aiServiceClient.trainModel(carsData);

            if (success) {
                log.info("AI 모델 학습 완료: {} 개의 차량 데이터로 학습", allCars.size());
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
     * 사용자별 차량 추천
     */
    public List<RecommendedCar> getRecommendationsForUser(Long userId, int topK) {
        try {
            // 사용자 즐겨찾기 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            List<Favorite> favorites = favoriteRepository.findByUser(user);

            if (favorites.isEmpty()) {
                log.info("사용자 {}의 즐겨찾기가 없어 일반 추천을 제공합니다.", userId);
                return getPopularCarsRecommendation(topK);
            }

            // 즐겨찾기한 차량 ID 리스트
            List<Long> favoriteCarIds = favorites.stream()
                    .map(favorite -> favorite.getCar().getId())
                    .collect(Collectors.toList());

            // AI 서비스에 추천 요청
            AIServiceClient.AIRecommendationResponse response = aiServiceClient.getRecommendations(
                    favoriteCarIds, favoriteCarIds, topK
            );

            if (response == null || response.getRecommendations().isEmpty()) {
                log.warn("AI 추천 결과가 없어 일반 추천을 제공합니다.");
                return getPopularCarsRecommendation(topK);
            }

            // AI 추천 결과를 RecommendedCar 객체로 변환
            return response.getRecommendations().stream()
                    .map(this::convertAIRecommendationToRecommendedCar)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("사용자 {} 추천 생성 중 오류: {}", userId, e.getMessage(), e);
            return getPopularCarsRecommendation(topK);
        }
    }

    /**
     * 사용자 선호도 분석
     */
    public Map<String, Object> analyzeUserPreferences(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            List<Favorite> favorites = favoriteRepository.findByUser(user);

            if (favorites.isEmpty()) {
                return Map.of("message", "즐겨찾기한 차량이 없어 선호도 분석을 할 수 없습니다.");
            }

            List<Long> favoriteCarIds = favorites.stream()
                    .map(favorite -> favorite.getCar().getId())
                    .collect(Collectors.toList());

            AIServiceClient.UserPreferenceAnalysis analysis = aiServiceClient.analyzeUserPreferences(favoriteCarIds);

            if (analysis != null) {
                return analysis.getAnalysis();
            } else {
                return Map.of("message", "선호도 분석 중 오류가 발생했습니다.");
            }

        } catch (Exception e) {
            log.error("사용자 {} 선호도 분석 중 오류: {}", userId, e.getMessage(), e);
            return Map.of("error", "선호도 분석 중 오류가 발생했습니다.");
        }
    }

    /**
     * 인기 차량 기반 추천 (기본 추천)
     */
    private List<RecommendedCar> getPopularCarsRecommendation(int topK) {
        List<Car> popularCars = carRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId())) // 최신 등록 순
                .limit(topK)
                .collect(Collectors.toList());

        return popularCars.stream()
                .map(car -> new RecommendedCar(car, 0.5, "인기 차량 추천"))
                .collect(Collectors.toList());
    }

    /**
     * Car 객체를 AI 서비스 형식으로 변환
     */
    private Map<String, Object> convertCarToAIFormat(Car car) {
        Map<String, Object> carData = new HashMap<>();
        carData.put("id", car.getId());
        carData.put("model", car.getModel());
        carData.put("year", car.getYear());
        carData.put("price", car.getPrice());
        carData.put("mileage", car.getMileage());
        carData.put("fuel", car.getFuel());
        carData.put("region", car.getRegion());
        carData.put("carType", car.getCarType());
        return carData;
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
    }
}