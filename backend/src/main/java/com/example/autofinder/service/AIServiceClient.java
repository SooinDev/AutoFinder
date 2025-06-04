package com.example.autofinder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * 후보 차량과 함께 추천 요청 (머신러닝 방식)
     */
    public AIRecommendationResponse getRecommendationsWithCandidates(Long userId, List<Long> favoriteCarIds,
                                                                     List<Map<String, Object>> candidateCars,
                                                                     List<Long> excludeIds, int topK) {
        try {
            String url = aiServiceBaseUrl + "/recommend";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("favorite_car_ids", favoriteCarIds != null ? favoriteCarIds : List.of());
            requestBody.put("candidate_cars", candidateCars != null ? candidateCars : List.of());
            requestBody.put("exclude_ids", excludeIds != null ? excludeIds : List.of());
            requestBody.put("top_k", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("머신러닝 추천 요청: 사용자 {}, 즐겨찾기 {}개, 후보 차량 {}개",
                    userId, favoriteCarIds.size(), candidateCars.size());

            // 응답을 Map으로 받아서 직접 파싱
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // 추천 리스트 추출
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recommendationsList =
                        (List<Map<String, Object>>) responseBody.get("recommendations");

                if (recommendationsList == null) {
                    log.warn("응답에서 recommendations 필드가 null입니다");
                    return createEmptyResponse();
                }

                // RecommendationItem으로 변환
                List<RecommendationItem> recommendations = recommendationsList.stream()
                        .map(this::convertMapToRecommendationItem)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // AIRecommendationResponse 생성
                AIRecommendationResponse aiResponse = new AIRecommendationResponse();
                aiResponse.setRecommendations(recommendations);

                // totalCount 추출 (여러 키 이름 지원)
                Object totalCountObj = responseBody.get("totalCount");
                if (totalCountObj == null) {
                    totalCountObj = responseBody.get("total_count");
                }

                int totalCount = recommendations.size();
                if (totalCountObj instanceof Number) {
                    totalCount = ((Number) totalCountObj).intValue();
                }

                aiResponse.setTotalCount(totalCount);
                aiResponse.setTimestamp(LocalDateTime.now().toString());

                log.info("머신러닝 추천 성공: {}개 추천 받음", recommendations.size());
                return aiResponse;

            } else {
                log.error("머신러닝 추천 실패: HTTP {}", response.getStatusCode());
                return createEmptyResponse();
            }

        } catch (Exception e) {
            log.error("머신러닝 추천 요청 중 오류 발생: {}", e.getMessage(), e);
            return createEmptyResponse();
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

            log.debug("기존 AI 추천 요청: 즐겨찾기 {}개, 제외 {}개, topK {}",
                    favoriteCarIds != null ? favoriteCarIds.size() : 0,
                    excludeIds != null ? excludeIds.size() : 0, topK);

            // 응답을 Map으로 받아서 처리
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recommendationsList =
                        (List<Map<String, Object>>) responseBody.get("recommendations");

                if (recommendationsList == null) {
                    log.warn("기존 추천 응답에서 recommendations 필드가 null입니다");
                    return createEmptyResponse();
                }

                List<RecommendationItem> recommendations = recommendationsList.stream()
                        .map(this::convertMapToRecommendationItem)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                AIRecommendationResponse aiResponse = new AIRecommendationResponse();
                aiResponse.setRecommendations(recommendations);
                aiResponse.setTotalCount(recommendations.size());
                aiResponse.setTimestamp(LocalDateTime.now().toString());

                log.info("기존 AI 추천 성공: {}개 추천 받음", recommendations.size());
                return aiResponse;

            } else {
                log.error("기존 AI 추천 실패: HTTP {}", response.getStatusCode());
                return createEmptyResponse();
            }

        } catch (Exception e) {
            log.error("기존 AI 추천 요청 중 오류 발생: {}", e.getMessage());
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
        emptyResponse.setTimestamp(LocalDateTime.now().toString());
        return emptyResponse;
    }

    /**
     * Map을 RecommendationItem으로 변환
     */
    private RecommendationItem convertMapToRecommendationItem(Map<String, Object> itemMap) {
        try {
            if (itemMap == null) {
                return null;
            }

            RecommendationItem item = new RecommendationItem();

            // car 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> carData = (Map<String, Object>) itemMap.get("car");
            if (carData != null) {
                item.setCar(carData);
            } else {
                log.warn("추천 아이템에서 car 데이터가 없습니다");
                return null;
            }

            // similarity_score 추출
            Object scoreObj = itemMap.get("similarity_score");
            if (scoreObj instanceof Number) {
                item.setSimilarityScore(((Number) scoreObj).doubleValue());
            } else {
                item.setSimilarityScore(0.5); // 기본값
            }

            // recommendation_reason 추출
            Object reasonObj = itemMap.get("recommendation_reason");
            if (reasonObj != null) {
                item.setRecommendationReason(reasonObj.toString());
            } else {
                item.setRecommendationReason("머신러닝 추천");
            }

            return item;

        } catch (Exception e) {
            log.error("추천 아이템 변환 중 오류: {}", e.getMessage());
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