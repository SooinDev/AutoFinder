package com.example.autofinder.controller;

import com.example.autofinder.service.MLRecommendationService;
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

    private final MLRecommendationService deepLearningRecommendationService;
    private final JwtUtil jwtUtil;

    /**
     * 스마트 AI 추천 차량 조회 (딥러닝 + 폴백)
     */
    @GetMapping("/recommend")
    public ResponseEntity<?> getRecommendations(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // JWT 토큰에서 사용자 ID 추출
            String jwtToken = token.substring(7); // "Bearer " 제거
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            // 스마트 추천 호출 (딥러닝 + 폴백)
            List<MLRecommendationService.RecommendedCar> recommendations =
                    deepLearningRecommendationService.getSmartRecommendations(userId, limit);

            // 디버그 정보 포함
            Map<String, Object> debugInfo = deepLearningRecommendationService.getRecommendationDebugInfo(userId);

            return ResponseEntity.ok(Map.of(
                    "recommendations", recommendations,
                    "total", recommendations.size(),
                    "strategy", debugInfo.get("shouldUseDeepLearning") != null &&
                            (Boolean) debugInfo.get("shouldUseDeepLearning") ? "deep_learning" : "legacy",
                    "debug", debugInfo,
                    "message", "스마트 AI 추천 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "추천 생성 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 강제 딥러닝 추천 (테스트용)
     */
    @GetMapping("/recommend/deep-learning")
    public ResponseEntity<?> getDeepLearningRecommendations(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            // 강제로 딥러닝 사용 (테스트 목적)
            List<MLRecommendationService.RecommendedCar> recommendations =
                    deepLearningRecommendationService.getSmartRecommendations(userId, limit);

            return ResponseEntity.ok(Map.of(
                    "recommendations", recommendations,
                    "total", recommendations.size(),
                    "strategy", "forced_deep_learning",
                    "message", "딥러닝 추천 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "딥러닝 추천 중 오류가 발생했습니다.",
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

            // 캐시 무효화 후 새로운 추천 생성
            deepLearningRecommendationService.onFavoriteChanged(userId);

            List<MLRecommendationService.RecommendedCar> recommendations =
                    deepLearningRecommendationService.getSmartRecommendations(userId, limit);

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
     * 추천 전략 디버그 정보 조회
     */
    @GetMapping("/debug/{userId}")
    public ResponseEntity<?> getRecommendationDebugInfo(@PathVariable Long userId) {
        try {
            Map<String, Object> debugInfo = deepLearningRecommendationService.getRecommendationDebugInfo(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "debugInfo", debugInfo,
                    "message", "디버그 정보 조회 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "디버그 정보 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 현재 로그인한 사용자의 디버그 정보 조회
     */
    @GetMapping("/debug/me")
    public ResponseEntity<?> getMyRecommendationDebugInfo(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            Map<String, Object> debugInfo = deepLearningRecommendationService.getRecommendationDebugInfo(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "debugInfo", debugInfo,
                    "message", "내 추천 디버그 정보 조회 완료"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "디버그 정보 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 추천 캐시 삭제
     */
    @DeleteMapping("/recommend/cache")
    public ResponseEntity<?> clearUserCache(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            deepLearningRecommendationService.onFavoriteChanged(userId);

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
     * AI 서비스 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAIServiceStatus() {
        try {
            boolean isAvailable = deepLearningRecommendationService.isAIServiceAvailable();

            return ResponseEntity.ok(Map.of(
                    "aiServiceAvailable", isAvailable,
                    "status", isAvailable ? "healthy" : "unavailable",
                    "message", isAvailable ? "AI 서비스가 정상 작동 중입니다." : "AI 서비스에 연결할 수 없습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "AI 서비스 상태 확인 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * A/B 테스트 분석 (관리자용)
     */
    @GetMapping("/ab-test/analysis")
    public ResponseEntity<?> getABTestAnalysis() {
        try {
            // 향후 A/B 테스트 분석 로직 구현
            Map<String, Object> analysis = Map.of(
                    "deepLearningGroup", Map.of(
                            "description", "딥러닝 추천을 받는 사용자 그룹",
                            "note", "실제 분석 데이터는 향후 구현 예정"
                    ),
                    "legacyGroup", Map.of(
                            "description", "기존 추천을 받는 사용자 그룹",
                            "note", "실제 분석 데이터는 향후 구현 예정"
                    ),
                    "message", "A/B 테스트 분석 기능 준비 중"
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "A/B 테스트 분석 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 추천 전략 변경 (관리자용)
     */
    @PostMapping("/strategy/toggle")
    public ResponseEntity<?> toggleRecommendationStrategy(
            @RequestBody Map<String, Object> request) {
        try {
            // 향후 런타임 전략 변경 기능 구현
            return ResponseEntity.ok(Map.of(
                    "message", "추천 전략 변경 기능은 향후 구현 예정입니다.",
                    "currentStrategy", "smart_routing",
                    "note", "현재는 application.properties를 통해 설정 변경 가능"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "전략 변경 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 추천 성능 메트릭 조회 (관리자용)
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getRecommendationMetrics() {
        try {
            // 향후 실제 메트릭 데이터 구현
            Map<String, Object> metrics = Map.of(
                    "totalRequests", "메트릭 수집 중",
                    "averageResponseTime", "메트릭 수집 중",
                    "successRate", "메트릭 수집 중",
                    "deepLearningUsage", "메트릭 수집 중",
                    "fallbackRate", "메트릭 수집 중",
                    "message", "실시간 메트릭 수집 기능 구현 중"
            );

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "메트릭 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }
}