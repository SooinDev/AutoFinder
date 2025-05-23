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
 * AI 추천 차량 목록 조회
 */
export const fetchAIRecommendations = async (limit = 10) => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/recommend?limit=${limit}`,
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("AI 추천 조회 오류:", error);
        throw error;
    }
};

/**
 * 사용자 선호도 분석 조회
 */
export const fetchUserPreferenceAnalysis = async () => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/ai/analysis`,
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
        const response = await axios.get(`${API_BASE_URL}/ai/status`);
        return response.data;
    } catch (error) {
        console.error("AI 서비스 상태 확인 오류:", error);
        throw error;
    }
};

/**
 * AI 모델 재학습 요청 (관리자용)
 */
export const triggerModelRetrain = async () => {
    try {
        const response = await axios.post(
            `${API_BASE_URL}/ai/retrain`,
            {},
            getAuthHeaders()
        );
        return response.data;
    } catch (error) {
        console.error("모델 재학습 요청 오류:", error);
        throw error;
    }
};