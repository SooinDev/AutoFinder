package com.example.autofinder.controller;

import com.example.autofinder.service.CarComparisonService;
import com.example.autofinder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
@Slf4j
public class CarComparisonController {

    private final CarComparisonService carComparisonService;
    private final JwtUtil jwtUtil;

    /**
     * 차량 비교 API (최대 3대)
     * @param carIds 비교할 차량 ID 리스트 (최대 3개)
     * @return 차량 비교 결과
     */
    @PostMapping("/compare")
    public ResponseEntity<?> compareCars(@RequestBody CompareRequest request) {
        try {
            List<Long> carIds = request.getCarIds();

            // 입력 검증
            if (carIds == null || carIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "비교할 차량을 선택해주세요.",
                        "code", "NO_CARS_SELECTED"
                ));
            }

            if (carIds.size() > 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "최대 3대까지 비교 가능합니다.",
                        "code", "TOO_MANY_CARS",
                        "maxCars", 3
                ));
            }

            if (carIds.size() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "최소 2대 이상 선택해주세요.",
                        "code", "NOT_ENOUGH_CARS",
                        "minCars", 2
                ));
            }

            // 차량 비교 수행
            Map<String, Object> comparisonResult = carComparisonService.compareCars(carIds);

            return ResponseEntity.ok(Map.of(
                    "comparison", comparisonResult,
                    "carCount", carIds.size(),
                    "message", "차량 비교가 완료되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            log.warn("차량 비교 요청 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "code", "INVALID_REQUEST"
            ));

        } catch (Exception e) {
            log.error("차량 비교 중 서버 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "차량 비교 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 상세 차량 비교 API (고급 분석 포함)
     */
    @PostMapping("/compare/detailed")
    public ResponseEntity<?> compareDetailedCars(@RequestBody CompareRequest request) {
        try {
            List<Long> carIds = request.getCarIds();

            // 기본 검증
            if (carIds == null || carIds.size() < 2 || carIds.size() > 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "2~3대의 차량을 선택해주세요."
                ));
            }

            // 상세 비교 수행
            Map<String, Object> detailedComparison = carComparisonService.compareDetailedCars(carIds);

            return ResponseEntity.ok(Map.of(
                    "detailedComparison", detailedComparison,
                    "analysisType", "detailed",
                    "message", "상세 차량 비교가 완료되었습니다."
            ));

        } catch (Exception e) {
            log.error("상세 차량 비교 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "상세 비교 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자별 차량 비교 기록 조회
     */
    @GetMapping("/history")
    public ResponseEntity<?> getComparisonHistory(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            List<Map<String, Object>> history = carComparisonService.getComparisonHistory(userId);

            return ResponseEntity.ok(Map.of(
                    "history", history,
                    "userId", userId,
                    "total", history.size(),
                    "message", "비교 기록 조회 완료"
            ));

        } catch (Exception e) {
            log.error("비교 기록 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "비교 기록 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 차량 비교 결과 저장 (사용자가 로그인한 경우)
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveComparison(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SaveComparisonRequest request) {
        try {
            Long userId = null;

            // 토큰이 있으면 사용자 ID 추출
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                userId = jwtUtil.extractUserId(jwtToken);
            }

            // 비교 결과 저장
            Long comparisonId = carComparisonService.saveComparison(
                    userId,
                    request.getCarIds(),
                    request.getComparisonName()
            );

            return ResponseEntity.ok(Map.of(
                    "comparisonId", comparisonId,
                    "message", "비교 결과가 저장되었습니다.",
                    "saved", true
            ));

        } catch (Exception e) {
            log.error("비교 결과 저장 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "비교 결과 저장 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 추천 차량과 비교 API
     */
    @PostMapping("/compare-with-recommendations")
    public ResponseEntity<?> compareWithRecommendations(
            @RequestHeader("Authorization") String token,
            @RequestBody CompareWithRecommendationsRequest request) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            Long targetCarId = request.getTargetCarId();
            int recommendationCount = request.getRecommendationCount() != null ?
                    request.getRecommendationCount() : 2;

            // 타겟 차량과 추천 차량들 비교
            Map<String, Object> comparison = carComparisonService.compareWithRecommendations(
                    userId, targetCarId, recommendationCount);

            return ResponseEntity.ok(Map.of(
                    "comparison", comparison,
                    "targetCarId", targetCarId,
                    "recommendationCount", recommendationCount,
                    "message", "추천 차량과의 비교가 완료되었습니다."
            ));

        } catch (Exception e) {
            log.error("추천 차량 비교 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "추천 차량 비교 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 비교 통계 API (관리자용)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getComparisonStats() {
        try {
            Map<String, Object> stats = carComparisonService.getComparisonStatistics();

            return ResponseEntity.ok(Map.of(
                    "statistics", stats,
                    "message", "비교 통계 조회 완료"
            ));

        } catch (Exception e) {
            log.error("비교 통계 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "통계 조회 중 오류가 발생했습니다."
            ));
        }
    }

    // DTO 클래스들
    public static class CompareRequest {
        private List<Long> carIds;

        public List<Long> getCarIds() { return carIds; }
        public void setCarIds(List<Long> carIds) { this.carIds = carIds; }
    }

    public static class SaveComparisonRequest {
        private List<Long> carIds;
        private String comparisonName;

        public List<Long> getCarIds() { return carIds; }
        public void setCarIds(List<Long> carIds) { this.carIds = carIds; }

        public String getComparisonName() { return comparisonName; }
        public void setComparisonName(String comparisonName) { this.comparisonName = comparisonName; }
    }

    public static class CompareWithRecommendationsRequest {
        private Long targetCarId;
        private Integer recommendationCount;

        public Long getTargetCarId() { return targetCarId; }
        public void setTargetCarId(Long targetCarId) { this.targetCarId = targetCarId; }

        public Integer getRecommendationCount() { return recommendationCount; }
        public void setRecommendationCount(Integer recommendationCount) {
            this.recommendationCount = recommendationCount;
        }
    }
}