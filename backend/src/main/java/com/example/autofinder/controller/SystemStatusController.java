package com.example.autofinder.controller;

import com.example.autofinder.repository.FavoriteRepository;
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
  private final FavoriteRepository favoriteRepository;


  /**
   * 전체 시스템 상태 확인 API (실시간 학습 포함)
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

      // 🔥 실시간 학습 상태 추가
      Map<String, Object> realTimeStatus = aiRecommendationService.getRealTimeTrainingStatus();
      status.put("realTimeTraining", realTimeStatus);

      // 전체 시스템 준비 상태
      boolean systemReady = totalCars > 0;
      boolean aiReady = (Boolean) aiStatus.get("aiModelTrained");
      boolean personalizedReady = (Boolean) aiStatus.get("personalizedRecommendationReady");
      boolean realTimeEnabled = (Boolean) aiStatus.get("realTimeTrainingEnabled");

      status.put("systemReady", systemReady);
      status.put("realTimeTrainingEnabled", realTimeEnabled);
      status.put("recommendationLevel", getRecommendationLevel(systemReady, aiReady, personalizedReady));

      // 사용자를 위한 메시지
      status.put("userMessage", generateUserMessage(systemReady, aiReady, personalizedReady, favoriteStats, realTimeStatus));

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
   * 🔍 실시간 학습 상태 전용 API
   */
  @GetMapping("/realtime-training")
  public ResponseEntity<Map<String, Object>> getRealTimeTrainingStatus() {
    try {
      Map<String, Object> realTimeStatus = aiRecommendationService.getRealTimeTrainingStatus();

      // 추가 컨텍스트 정보
      FavoriteService.FavoriteStatistics favoriteStats = favoriteService.getFavoriteStatistics();
      realTimeStatus.put("favoriteStats", Map.of(
              "totalFavorites", favoriteStats.getTotalFavorites(),
              "usersWithFavorites", favoriteStats.getUsersWithFavorites(),
              "readyForPersonalization", favoriteStats.getTotalFavorites() >= 5
      ));

      return ResponseEntity.ok(Map.of(
              "realTimeTraining", realTimeStatus,
              "message", generateRealTimeStatusMessage(realTimeStatus, favoriteStats),
              "timestamp", System.currentTimeMillis()
      ));

    } catch (Exception e) {
      log.error("실시간 학습 상태 조회 중 오류", e);
      return ResponseEntity.status(500).body(Map.of(
              "error", "실시간 학습 상태 확인 중 오류가 발생했습니다.",
              "details", e.getMessage()
      ));
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
   * 🔥 AI 모델 수동 재학습 트리거 (관리자용) - 실시간 학습 지원
   */
  @PostMapping("/ai/retrain")
  public ResponseEntity<Map<String, Object>> triggerAIRetraining() {
    try {
      FavoriteService.FavoriteStatistics stats = favoriteService.getFavoriteStatistics();

      if (stats.getTotalFavorites() == 0) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "즐겨찾기 데이터가 없어 AI 재학습을 할 수 없습니다.",
                "suggestion", "사용자가 차량을 즐겨찾기에 추가한 후 다시 시도해주세요.",
                "currentFavorites", 0
        ));
      }

      // 🔥 FavoriteService를 통한 수동 재학습 트리거
      boolean success = favoriteService.triggerManualAIRetraining();

      if (success) {
        log.info("🔧 관리자가 AI 재학습을 수동으로 트리거했습니다.");

        return ResponseEntity.ok(Map.of(
                "message", "🚀 실시간 AI 재학습이 시작되었습니다.",
                "favoriteCount", stats.getTotalFavorites(),
                "usersWithFavorites", stats.getUsersWithFavorites(),
                "status", "training_started",
                "trainingType", "manual_trigger",
                "expectedDuration", "30-60초"
        ));
      } else {
        return ResponseEntity.status(500).body(Map.of(
                "error", "AI 재학습 시작에 실패했습니다.",
                "favoriteCount", stats.getTotalFavorites()
        ));
      }

    } catch (Exception e) {
      log.error("AI 재학습 트리거 중 오류", e);
      return ResponseEntity.status(500).body(Map.of(
              "error", "AI 재학습 시작 중 오류가 발생했습니다.",
              "details", e.getMessage()
      ));
    }
  }

  /**
   * 🧹 추천 캐시 클리어 (관리자용)
   */
  @DeleteMapping("/cache/clear")
  public ResponseEntity<Map<String, Object>> clearRecommendationCache() {
    try {
      aiRecommendationService.clearAllCache();

      return ResponseEntity.ok(Map.of(
              "message", "🧹 모든 추천 캐시가 삭제되었습니다.",
              "status", "cache_cleared",
              "effect", "다음 추천 요청 시 최신 AI 모델 결과가 반영됩니다."
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
   * 📊 실시간 학습 통계 API (관리자용)
   */
  @GetMapping("/ai/training-stats")
  public ResponseEntity<Map<String, Object>> getTrainingStatistics() {
    try {
      Map<String, Object> realTimeStatus = aiRecommendationService.getRealTimeTrainingStatus();
      FavoriteService.FavoriteStatistics favoriteStats = favoriteService.getFavoriteStatistics();

      Map<String, Object> trainingStats = new HashMap<>();

      // 학습 현황
      trainingStats.put("currentlyTraining", realTimeStatus.get("isCurrentlyTraining"));
      trainingStats.put("totalTrainingCount", realTimeStatus.get("consecutiveTrainingCount"));
      trainingStats.put("lastTrainingTime", realTimeStatus.get("lastTrainingTime"));
      trainingStats.put("lastFavoriteChange", realTimeStatus.get("lastFavoriteChangeTime"));

      // 데이터 현황
      trainingStats.put("totalFavorites", favoriteStats.getTotalFavorites());
      trainingStats.put("activeUsers", favoriteStats.getUsersWithFavorites());
      trainingStats.put("totalCars", carRepository.count());

      // 추천 시스템 준비도
      boolean readyForBasic = favoriteStats.getTotalFavorites() >= 1;
      boolean readyForAdvanced = favoriteStats.getTotalFavorites() >= 5;
      boolean readyForOptimal = favoriteStats.getTotalFavorites() >= 20;

      trainingStats.put("readinessLevel", Map.of(
              "basic", readyForBasic,
              "advanced", readyForAdvanced,
              "optimal", readyForOptimal,
              "currentLevel", readyForOptimal ? "최적" : (readyForAdvanced ? "고급" : (readyForBasic ? "기본" : "대기"))
      ));

      // 캐시 현황
      trainingStats.put("cacheSize", realTimeStatus.get("cacheSize"));

      return ResponseEntity.ok(Map.of(
              "trainingStatistics", trainingStats,
              "message", "실시간 학습 통계 조회 완료",
              "systemHealth", "정상"
      ));

    } catch (Exception e) {
      log.error("학습 통계 조회 중 오류", e);
      return ResponseEntity.status(500).body(Map.of(
              "error", "학습 통계 조회 중 오류가 발생했습니다.",
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
      long totalFavorites = favoriteRepository.count();
      boolean systemReady = totalCars > 0;
      boolean aiModelTrained = aiRecommendationService.isAIModelTrained();
      Map<String, Object> realTimeStatus = aiRecommendationService.getRealTimeTrainingStatus();

      Map<String, Object> health = Map.of(
              "status", systemReady ? "UP" : "DOWN",
              "carsAvailable", totalCars > 0,
              "aiModelTrained", aiModelTrained,
              "realTimeTrainingEnabled", true,
              "isCurrentlyTraining", realTimeStatus.get("isCurrentlyTraining"),
              "totalCars", totalCars,
              "totalFavorites", totalFavorites,
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

  // === 유틸리티 메서드들 ===

  /**
   * 추천 시스템 레벨 결정
   */
  private String getRecommendationLevel(boolean systemReady, boolean aiReady, boolean personalizedReady) {
    if (!systemReady) {
      return "UNAVAILABLE"; // 차량 데이터 없음
    } else if (personalizedReady) {
      return "PERSONALIZED_REALTIME"; // 실시간 개인화 AI 추천
    } else if (aiReady) {
      return "AI_BASIC"; // 기본 AI 추천
    } else {
      return "BASIC"; // 기본 인기 차량 추천
    }
  }

  /**
   * 사용자를 위한 안내 메시지 생성 (실시간 학습 포함)
   */
  private String generateUserMessage(boolean systemReady, boolean aiReady, boolean personalizedReady,
                                     FavoriteService.FavoriteStatistics favoriteStats,
                                     Map<String, Object> realTimeStatus) {

    boolean isTraining = (Boolean) realTimeStatus.get("isCurrentlyTraining");

    if (!systemReady) {
      return "🔄 시스템 준비 중입니다. 차량 데이터를 수집하고 있어요.";
    } else if (isTraining) {
      return String.format("🤖 AI가 실시간으로 학습 중입니다! (%d개 즐겨찾기 데이터 반영 중)",
              favoriteStats.getTotalFavorites());
    } else if (personalizedReady) {
      return String.format("✨ 실시간 개인화 AI 추천이 활성화되었습니다! 총 %d개의 즐겨찾기로 학습된 최신 모델이에요.",
              favoriteStats.getTotalFavorites());
    } else if (favoriteStats.getTotalFavorites() == 0) {
      return "💡 차량을 즐겨찾기에 추가하면 즉시 개인화된 AI 추천을 받을 수 있어요!";
    } else {
      return String.format("🚀 즐겨찾기 %d개로 AI가 학습 중입니다. 곧 개인화 추천이 시작됩니다!",
              favoriteStats.getTotalFavorites());
    }
  }

  /**
   * 실시간 학습 상태 메시지 생성
   */
  private String generateRealTimeStatusMessage(Map<String, Object> realTimeStatus,
                                               FavoriteService.FavoriteStatistics favoriteStats) {

    boolean isTraining = (Boolean) realTimeStatus.get("isCurrentlyTraining");
    boolean modelTrained = (Boolean) realTimeStatus.get("modelTrained");
    int trainingCount = (Integer) realTimeStatus.get("consecutiveTrainingCount");

    if (isTraining) {
      return "🔄 AI 모델이 실시간으로 학습 중입니다...";
    } else if (modelTrained && favoriteStats.getTotalFavorites() >= 5) {
      return String.format("✅ 실시간 학습 완료! AI가 %d번 학습하여 최신 선호도를 반영했습니다.", trainingCount);
    } else if (favoriteStats.getTotalFavorites() > 0) {
      return String.format("⚡ 즉시 학습 대기 중 (%d개 즐겨찾기)", favoriteStats.getTotalFavorites());
    } else {
      return "💤 학습 대기 중 - 첫 즐겨찾기 추가 시 즉시 학습이 시작됩니다.";
    }
  }
}