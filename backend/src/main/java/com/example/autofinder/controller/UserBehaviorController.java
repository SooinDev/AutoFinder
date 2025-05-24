package com.example.autofinder.controller;

import com.example.autofinder.service.UserBehaviorService;
import com.example.autofinder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/behavior")
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;
    private final JwtUtil jwtUtil;

    /**
     * 사용자 행동 기록 API
     */
    @PostMapping("/track")
    public ResponseEntity<?> trackUserBehavior(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody TrackingRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = null;

            // JWT 토큰에서 사용자 ID 추출 (선택사항)
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                userId = jwtUtil.extractUserId(jwtToken);
            }

            // 요청에서 사용자 ID가 제공된 경우 사용
            if (request.getUserId() != null) {
                userId = request.getUserId();
            }

            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "사용자 ID가 필요합니다.",
                        "code", "USER_ID_REQUIRED"
                ));
            }

            // 추가 정보 수집
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);
            String referrer = httpRequest.getHeader("Referer");

            // 행동 기록
            userBehaviorService.recordUserAction(
                    userId,
                    request.getCarId(),
                    request.getActionType(),
                    request.getValue()
            );

            log.debug("사용자 행동 추적 완료 - 사용자: {}, 차량: {}, 행동: {}",
                    userId, request.getCarId(), request.getActionType());

            return ResponseEntity.ok(Map.of(
                    "message", "행동 기록 완료",
                    "userId", userId,
                    "carId", request.getCarId(),
                    "actionType", request.getActionType()
            ));

        } catch (Exception e) {
            log.error("사용자 행동 추적 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "행동 기록 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 배치 행동 기록 API (여러 행동을 한 번에 기록)
     */
    @PostMapping("/track/batch")
    public ResponseEntity<?> trackBatchBehaviors(
            @RequestHeader("Authorization") String token,
            @RequestBody BatchTrackingRequest request) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            int successCount = 0;
            for (TrackingRequest tracking : request.getBehaviors()) {
                try {
                    userBehaviorService.recordUserAction(
                            userId,
                            tracking.getCarId(),
                            tracking.getActionType(),
                            tracking.getValue()
                    );
                    successCount++;
                } catch (Exception e) {
                    log.warn("배치 기록 중 개별 실패 - 사용자: {}, 차량: {}",
                            userId, tracking.getCarId(), e);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "배치 행동 기록 완료",
                    "totalRequested", request.getBehaviors().size(),
                    "successCount", successCount,
                    "userId", userId
            ));

        } catch (Exception e) {
            log.error("배치 행동 추적 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "배치 기록 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 행동 데이터 조회 API
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBehaviorData(@PathVariable Long userId) {
        try {
            Map<String, Object> behaviorData = userBehaviorService.getUserBehaviorData(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "behaviorData", behaviorData,
                    "message", "사용자 행동 데이터 조회 완료"
            ));

        } catch (Exception e) {
            log.error("사용자 행동 데이터 조회 중 오류 - 사용자: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "행동 데이터 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 현재 로그인한 사용자의 행동 데이터 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyBehaviorData(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwtToken);

            if (userId == null) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            Map<String, Object> behaviorData = userBehaviorService.getUserBehaviorData(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "behaviorData", behaviorData,
                    "message", "내 행동 데이터 조회 완료"
            ));

        } catch (Exception e) {
            log.error("내 행동 데이터 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "행동 데이터 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 행동 통계 조회 API (관리자용)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getBehaviorStatistics() {
        try {
            Map<String, Object> stats = userBehaviorService.getBehaviorStatistics();

            return ResponseEntity.ok(Map.of(
                    "statistics", stats,
                    "message", "행동 통계 조회 완료"
            ));

        } catch (Exception e) {
            log.error("행동 통계 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "통계 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 실시간 활동 모니터링 API
     */
    @GetMapping("/activity/realtime")
    public ResponseEntity<?> getRealTimeActivity() {
        try {
            // 최근 1시간 활동 조회 (실제 구현은 UserBehaviorService에 추가 필요)
            Map<String, Object> activity = Map.of(
                    "message", "실시간 활동 데이터",
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(activity);

        } catch (Exception e) {
            log.error("실시간 활동 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "실시간 활동 조회 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * 캐시 정리 API (관리자용)
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<?> clearBehaviorCache() {
        try {
            userBehaviorService.clearExpiredCache();

            return ResponseEntity.ok(Map.of(
                    "message", "행동 데이터 캐시 정리 완료"
            ));

        } catch (Exception e) {
            log.error("캐시 정리 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "캐시 정리 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    // 유틸리티 메서드
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 첫 번째 IP 주소 반환 (여러 개인 경우)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // DTO 클래스들
    public static class TrackingRequest {
        private Long userId;        // 선택사항 (토큰에서 추출 가능)
        private Long carId;         // 필수
        private String actionType;  // 필수
        private Object value;       // 선택사항
        private String sessionId;  // 선택사항

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getCarId() { return carId; }
        public void setCarId(Long carId) { this.carId = carId; }

        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    public static class BatchTrackingRequest {
        private java.util.List<TrackingRequest> behaviors;

        public java.util.List<TrackingRequest> getBehaviors() { return behaviors; }
        public void setBehaviors(java.util.List<TrackingRequest> behaviors) { this.behaviors = behaviors; }
    }
}