package com.example.autofinder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIServiceClient {

    @Value("${ai.service.base-url:http://localhost:5001}")
    private String aiServiceBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * AI 서비스 헬스 체크
     */
    public boolean isAIServiceHealthy() {
        try {
            String url = aiServiceBaseUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("AI 서비스 헬스 체크 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * AI 모델 학습 (전체 차량 데이터 전송) - 기존 메서드
     */
    public boolean trainModel(List<Object> carsData) {
        try {
            String url = aiServiceBaseUrl + "/train";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cars", carsData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("AI 모델 학습 성공: {} 개의 차량 데이터", carsData.size());
                return true;
            } else {
                log.error("AI 모델 학습 실패: HTTP {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            log.error("AI 모델 학습 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * AI 모델 학습 (즐겨찾기 데이터 포함) - 새로운 메서드
     */
    public boolean trainModelWithFavorites(List<Object> carsData, List<Map<String, Object>> favoritesData, Map<String, Object> userBehaviors) {
        try {
            String url = aiServiceBaseUrl + "/train";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cars", carsData);
            requestBody.put("favorites", favoritesData);

            // 사용자 행동 데이터가 있으면 추가
            if (userBehaviors != null && !userBehaviors.isEmpty()) {
                requestBody.put("user_behaviors", userBehaviors);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("AI 학습 요청 데이터: 차량 {}개, 즐겨찾기 {}개, 사용자 행동 {}개",
                    carsData.size(), favoritesData.size(),
                    userBehaviors != null ? userBehaviors.size() : 0);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("AI 모델 학습 성공: {} 개의 차량 데이터, {} 개의 즐겨찾기 데이터",
                        carsData.size(), favoritesData.size());
                return true;
            } else {
                log.error("AI 모델 학습 실패: HTTP {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            log.error("AI 모델 학습 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 차량 추천 요청 (기존 방식 - 안전성 강화)
     */
    public AIRecommendationResponse getRecommendations(List<Long> favoriteCarIds, List<Long> excludeIds, int topK) {
        try {
            String url = aiServiceBaseUrl + "/recommend";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("favorite_car_ids", favoriteCarIds != null ? favoriteCarIds : List.of());
            requestBody.put("exclude_ids", excludeIds != null ? excludeIds : List.of());
            requestBody.put("top_k", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("AI 추천 요청 (기존방식): 즐겨찾기 {}개, 제외 {}개, topK {}",
                    favoriteCarIds != null ? favoriteCarIds.size() : 0,
                    excludeIds != null ? excludeIds.size() : 0, topK);

            ResponseEntity<AIRecommendationResponse> response = restTemplate.postForEntity(
                    url, request, AIRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                AIRecommendationResponse body = response.getBody();
                if (body != null && body.getRecommendations() != null) {
                    log.info("AI 추천 성공: {} 개의 추천 차량", body.getRecommendations().size());
                    return body;
                } else {
                    log.warn("AI 추천 응답이 비어있습니다.");
                    return createEmptyResponse();
                }
            } else {
                log.error("AI 추천 실패: HTTP {}", response.getStatusCode());
                return createEmptyResponse();
            }

        } catch (RestClientException e) {
            log.error("AI 추천 요청 중 오류 발생: {}", e.getMessage());
            return createEmptyResponse();
        }
    }

    /**
     * 빈 응답 생성 (에러 시 폴백)
     */
    private AIRecommendationResponse createEmptyResponse() {
        AIRecommendationResponse emptyResponse = new AIRecommendationResponse();
        emptyResponse.setRecommendations(List.of());
        emptyResponse.setTotalCount(0);
        emptyResponse.setTimestamp(java.time.LocalDateTime.now().toString());
        return emptyResponse;
    }

    /**
     * 후보 차량과 함께 추천 요청 (새로운 방식)
     */
    public AIRecommendationResponse getRecommendationsWithCandidates(Long userId, List<Long> favoriteCarIds,
                                                                     List<Map<String, Object>> candidateCars,
                                                                     List<Long> excludeIds, int topK) {
        try {
            String url = aiServiceBaseUrl + "/recommend";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("favorite_car_ids", favoriteCarIds);
            requestBody.put("candidate_cars", candidateCars);
            requestBody.put("exclude_ids", excludeIds != null ? excludeIds : List.of());
            requestBody.put("top_k", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("AI 추천 요청: 사용자 {}, 즐겨찾기 {}개, 후보 차량 {}개",
                    userId, favoriteCarIds.size(), candidateCars.size());

            ResponseEntity<AIRecommendationResponse> response = restTemplate.postForEntity(
                    url, request, AIRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("AI 추천 성공: {} 개의 추천 차량", response.getBody().getRecommendations().size());
                return response.getBody();
            } else {
                log.error("AI 추천 실패: HTTP {}", response.getStatusCode());
                return null;
            }

        } catch (RestClientException e) {
            log.error("AI 추천 요청 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 사용자 선호도 분석 요청
     */
    public UserPreferenceAnalysis analyzeUserPreferences(List<Long> favoriteCarIds) {
        try {
            String url = aiServiceBaseUrl + "/user-analysis";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("favorite_car_ids", favoriteCarIds);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<UserPreferenceAnalysis> response = restTemplate.postForEntity(
                    url, request, UserPreferenceAnalysis.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("사용자 선호도 분석 성공");
                return response.getBody();
            } else {
                log.error("사용자 선호도 분석 실패: HTTP {}", response.getStatusCode());
                return null;
            }

        } catch (RestClientException e) {
            log.error("사용자 선호도 분석 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    // DTO 클래스들
    public static class AIRecommendationResponse {
        private List<RecommendationItem> recommendations;
        private int totalCount;
        private String timestamp;

        // getters and setters
        public List<RecommendationItem> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<RecommendationItem> recommendations) {
            this.recommendations = recommendations;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class RecommendationItem {
        private Map<String, Object> car;
        private double similarityScore;
        private String recommendationReason;

        // getters and setters
        public Map<String, Object> getCar() {
            return car;
        }

        public void setCar(Map<String, Object> car) {
            this.car = car;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
        }

        public String getRecommendationReason() {
            return recommendationReason;
        }

        public void setRecommendationReason(String recommendationReason) {
            this.recommendationReason = recommendationReason;
        }
    }

    public static class UserPreferenceAnalysis {
        private Map<String, Object> analysis;
        private int favoriteCaresCount;
        private String timestamp;

        // getters and setters
        public Map<String, Object> getAnalysis() {
            return analysis;
        }

        public void setAnalysis(Map<String, Object> analysis) {
            this.analysis = analysis;
        }

        public int getFavoriteCaresCount() {
            return favoriteCaresCount;
        }

        public void setFavoriteCaresCount(int favoriteCaresCount) {
            this.favoriteCaresCount = favoriteCaresCount;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}