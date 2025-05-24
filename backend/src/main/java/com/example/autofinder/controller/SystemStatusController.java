package com.example.autofinder.controller;

import com.example.autofinder.service.AIRecommendationService;
import com.example.autofinder.service.FavoriteService;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemStatusController {

    private final AIRecommendationService aiRecommendationService;
    private final FavoriteService favoriteService;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    /**
     * 전체 시스템 상태 확인 API
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // 기본 데이터 현황
            long totalCars = carRepository.count();
            long totalUsers = userRepository.count();

            status.put("dataStatus", Map.of(
                    "totalCars", totalCars,
                    "totalUsers", totalUsers,
                    "carsReady", totalCars > 0,
                    "usersReady", totalUsers > 0
            ));

            // 즐겨찾기 현황
            FavoriteService.FavoriteStatistics favoriteStats = favoriteService.getFavoriteStatistics();
            status.put("favoriteStatus", Map.of(
                    "totalFavorites", favoriteStats.getTotalFavorites(),
                    "usersWithFavorites", favoriteStats.getUsersWithFavorites(),
                    "favoritedCars", favoriteStats.getFavoritedCars(),
                    "userParticipationRate", Math.round(favoriteStats.getUserParticipationRate() * 100),
                    "carCoverageRate", Math.round(favoriteStats.getCarCoverageRate() * 100),
                    "favoriteDataReady", favoriteStats.getTotalFavorites() > 0
            ));

            // AI 시스템 현황
            Map<String, Object> aiStatus = aiRecommendationService.getAISystemStatus();
            status.put("aiStatus", aiStatus);

            // 전체 시스템 준비 상태
            boolean systemReady = totalCars > 0;
            boolean aiReady = (Boolean) aiStatus.get("aiModelTrained");
            boolean personalizedReady = (Boolean) aiStatus.get("personalizedRecommendationReady");

            status.put("systemReady", systemReady);
            status.put("recommendationLevel", getRecommendationLevel(systemReady, aiReady, personalizedReady));

            // 사용자를 위한 메시지
            status.put("userMessage", generateUserMessage(systemReady, aiReady, personalizedReady, favoriteStats));

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("시스템 상태 확인 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "시스템 상태 확인 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 추천 시스템 레벨 결정
     */
    private String getRecommendationLevel(boolean systemReady, boolean aiReady, boolean personalizedReady) {
        if (!systemReady) {
            return "UNAVAILABLE"; // 차량 데이터 없음
        } else if (personalizedReady) {
            return "PERSONALIZED"; // 개인화 AI 추천
        } else if (aiReady) {
            return "AI_BASIC"; // 기본 AI 추천
        } else {
            return "BASIC"; // 기본 인기 차량 추천
        }
    }

    /**
     * 사용자를 위한 안내 메시지 생성
     */
    private String generateUserMessage(boolean systemReady, boolean aiReady, boolean personalizedReady,
                                       FavoriteService.FavoriteStatistics favoriteStats) {
        if (!systemReady) {
            return "🔄 시스템 준비 중입니다. 차량 데이터를 수집하고 있어요.";
        } else if (personalizedReady) {
            return String.format("✨ 개인화 AI 추천이 활성화되었습니다! 총 %d개의 즐겨찾기 데이터로 학습되었어요.",
                    favoriteStats.getTotalFavorites());
        } else if (favoriteStats.getTotalFavorites() == 0) {
            return "💡 차량을 즐겨찾기에 추가하면 개인화된 AI 추천을 받을 수 있어요!";
        } else {
            return "🤖 AI가 학습 중입니다. 잠시만 기다려주세요...";
        }
    }

    /**
     * 즐겨찾기 통계만 조회
     */
    @GetMapping("/favorites/stats")
    public ResponseEntity<FavoriteService.FavoriteStatistics> getFavoriteStatistics() {
        try {
            FavoriteService.FavoriteStatistics stats = favoriteService.getFavoriteStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("즐겨찾기 통계 조회 중 오류", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * AI 시스템 상태만 조회
     */
    @GetMapping("/ai/status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        try {
            Map<String, Object> aiStatus = aiRecommendationService.getAISystemStatus();
            return ResponseEntity.ok(aiStatus);
        } catch (Exception e) {
            log.error("AI 상태 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "AI 상태 확인 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * AI 모델 수동 재학습 트리거 (관리자용)
     */
    @PostMapping("/ai/retrain")
    public ResponseEntity<Map<String, Object>> triggerAIRetraining() {
        try {
            FavoriteService.FavoriteStatistics stats = favoriteService.getFavoriteStatistics();

            if (stats.getTotalFavorites() == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "즐겨찾기 데이터가 없어 AI 재학습을 할 수 없습니다.",
                        "suggestion", "사용자가 차량을 즐겨찾기에 추가한 후 다시 시도해주세요."
                ));
            }

            log.info("관리자가 AI 재학습을 수동으로 트리거했습니다.");
            aiRecommendationService.trainAIModelAsync();

            return ResponseEntity.ok(Map.of(
                    "message", "AI 재학습이 시작되었습니다.",
                    "favoriteCount", stats.getTotalFavorites(),
                    "status", "training_started"
            ));

        } catch (Exception e) {
            log.error("AI 재학습 트리거 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "AI 재학습 시작 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 추천 캐시 클리어 (관리자용)
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearRecommendationCache() {
        try {
            aiRecommendationService.clearAllCache();

            return ResponseEntity.ok(Map.of(
                    "message", "모든 추천 캐시가 삭제되었습니다.",
                    "status", "cache_cleared"
            ));

        } catch (Exception e) {
            log.error("캐시 클리어 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "캐시 클리어 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 시스템 준비 상태 간단 확인 (헬스체크용)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long totalCars = carRepository.count();
            boolean systemReady = totalCars > 0;
            boolean aiModelTrained = aiRecommendationService.isAIModelTrained();

            Map<String, Object> health = Map.of(
                    "status", systemReady ? "UP" : "DOWN",
                    "carsAvailable", totalCars > 0,
                    "aiModelTrained", aiModelTrained,
                    "totalCars", totalCars,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}