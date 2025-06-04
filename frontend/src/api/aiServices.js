// src/api/aiServices.js
import axios from 'axios';

const API_BASE_URL = "http://localhost:8080/api";

// 인증 헤더 설정
const getAuthHeaders = () => {
    const token = localStorage.getItem("token") || sessionStorage.getItem("token");
    return {
        headers: {
            "Authorization": token ? `Bearer ${token}` : undefined
        },
        withCredentials: true
    };
};

/**
 * AI 추천 차량 목록 조회 (머신러닝 기반)
 */
export const fetchAIRecommendations = async (limit = 10) => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/recommend?limit=${limit}`, // ✅ 올바른 엔드포인트
            getAuthHeaders()
        );

        console.log('AI 추천 응답:', response.data); // 디버깅용
        return response.data;
    } catch (error) {
        console.error("AI 추천 조회 오류:", error);

        // 상세 에러 정보 출력
        if (error.response) {
            console.error("응답 상태:", error.response.status);
            console.error("응답 데이터:", error.response.data);
        }

        throw error;
    }
};

/**
 * 강제 머신러닝 추천 조회 (테스트용)
 */
export const fetchMachineLearningRecommendations = async (limit = 10) => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/recommend/machine-learning?limit=${limit}`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("머신러닝 추천 조회 오류:", error);
        throw error;
    }
};

/**
 * AI 추천 새로고침
 */
export const refreshAIRecommendations = async (limit = 10) => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/recommend/refresh?limit=${limit}`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("AI 추천 새로고침 오류:", error);
        throw error;
    }
};

/**
 * 사용자별 디버그 정보 조회
 */
export const fetchAIDebugInfo = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/debug/me`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("AI 디버그 정보 조회 오류:", error);
        throw error;
    }
};

/**
 * 사용자 선호도 분석 조회 (기존 메서드 - 아직 구현되지 않음)
 */
export const fetchUserPreferenceAnalysis = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/analysis`, // 이 엔드포인트는 아직 구현되지 않음
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("선호도 분석 조회 오류:", error);
        throw error;
    }
};

/**
 * AI 서비스 상태 확인
 */
export const fetchAIServiceStatus = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/status`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("AI 서비스 상태 확인 오류:", error);

        // 기본 응답 반환 (서비스 불가능 상태)
        return {
            aiServiceAvailable: false,
            status: 'unavailable',
            message: 'AI 서비스에 연결할 수 없습니다.'
        };
    }
};

/**
 * AI 모델 재학습 요청 (관리자용) - 시스템 API 사용
 */
export const triggerModelRetrain = async () => {
    try {
        const response = await axios.post(
            `${API_BASE_URL}/system/ai/retrain`, // ✅ 올바른 시스템 엔드포인트
            {},
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("모델 재학습 요청 오류:", error);
        throw error;
    }
};

/**
 * 추천 캐시 삭제
 */
export const clearRecommendationCache = async () => {
    try {
        const response = await axios.delete(
            `${API_BASE_URL}/ai/recommend/cache`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("캐시 삭제 오류:", error);
        throw error;
    }
};

/**
 * A/B 테스트 분석 조회 (관리자용)
 */
export const fetchABTestAnalysis = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/ab-test/analysis`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("A/B 테스트 분석 조회 오류:", error);
        throw error;
    }
};

/**
 * 추천 성능 메트릭 조회 (관리자용)
 */
export const fetchRecommendationMetrics = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/metrics`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("추천 메트릭 조회 오류:", error);
        throw error;
    }
};