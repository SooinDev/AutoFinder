package com.example.autofinder.controller;

import com.example.autofinder.service.AIRecommendationService;
import com.example.autofinder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRecommendationController {

    private final AIRecommendationService aiRecommendationService;
    private final JwtUtil jwtUtil;

    /**
     * 사용자별 AI 추천 차량 조회 (캐시 사용)
     */
    @GetMapping("/recommend")
    public ResponseEntity<?> getRecommendations(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            List<AIRecommendationService.RecommendedCar> recommendations =
                    aiRecommendationService.getRecommendationsForUser(userId, limit);

            return ResponseEntity.ok(Map.of(
                    "recommendations", recommendations,
                    "total", recommendations.size(),
                    "cached", true,
                    "message", "AI 추천 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "추천 생성 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자별 AI 추천 차량 조회 (강제 새로고침)
     */
    @GetMapping("/recommend/refresh")
    public ResponseEntity<?> getRecommendationsRefresh(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            // 강제 새로고침으로 추천 생성
            List<AIRecommendationService.RecommendedCar> recommendations =
                    aiRecommendationService.getRecommendationsForUser(userId, limit, true);

            return ResponseEntity.ok(Map.of(
                    "recommendations", recommendations,
                    "total", recommendations.size(),
                    "cached", false,
                    "refreshed", true,
                    "message", "AI 추천 새로고침 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "추천 새로고침 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 추천 캐시 삭제
     */
    @DeleteMapping("/recommend/cache")
    public ResponseEntity<?> clearUserCache(
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            aiRecommendationService.clearUserCache(userId);

            return ResponseEntity.ok(Map.of(
                    "message", "사용자 추천 캐시가 삭제되었습니다.",
                    "userId", userId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "캐시 삭제 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 전체 추천 캐시 삭제 (관리자용)
     */
    @DeleteMapping("/recommend/cache/all")
    public ResponseEntity<?> clearAllCache() {
        try {
            aiRecommendationService.clearAllCache();

            return ResponseEntity.ok(Map.of(
                    "message", "모든 추천 캐시가 삭제되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "전체 캐시 삭제 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 선호도 분석
     */
    @GetMapping("/analysis")
    public ResponseEntity<?> getUserPreferenceAnalysis(
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            Map<String, Object> analysis = aiRecommendationService.analyzeUserPreferences(userId);

            return ResponseEntity.ok(Map.of(
                    "analysis", analysis,
                    "userId", userId,
                    "message", "선호도 분석 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "선호도 분석 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * AI 서비스 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAIServiceStatus() {
        boolean isAvailable = aiRecommendationService.isAIServiceAvailable();

        return ResponseEntity.ok(Map.of(
                "aiServiceAvailable", isAvailable,
                "status", isAvailable ? "healthy" : "unavailable",
                "message", isAvailable ? "AI 서비스가 정상 작동 중입니다." : "AI 서비스에 연결할 수 없습니다."
        ));
    }

    /**
     * AI 모델 재학습 트리거 (관리자용)
     */
    @PostMapping("/retrain")
    public ResponseEntity<?> retrainModel() {
        try {
            aiRecommendationService.trainAIModelAsync();

            return ResponseEntity.ok(Map.of(
                    "message", "AI 모델 재학습이 시작되었습니다.",
                    "status", "started"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "모델 재학습 시작 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }
}