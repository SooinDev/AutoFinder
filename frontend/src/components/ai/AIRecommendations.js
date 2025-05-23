// src/components/ai/AIRecommendations.js
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchAIRecommendations, fetchAIServiceStatus } from '../../api/aiServices';
import CarCard from '../car/CarCard';

const AIRecommendations = ({ userId, favorites, setFavorites, onToggleFavorite }) => {
    const [recommendations, setRecommendations] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [aiServiceStatus, setAiServiceStatus] = useState(null);

    useEffect(() => {
        if (userId) {
            checkAIServiceStatus();
            loadRecommendations();
        }
    }, [userId]);

    const checkAIServiceStatus = async () => {
        try {
            const status = await fetchAIServiceStatus();
            setAiServiceStatus(status);
        } catch (err) {
            console.error("AI 서비스 상태 확인 실패:", err);
            setAiServiceStatus({ aiServiceAvailable: false });
        }
    };

    const loadRecommendations = async () => {
        if (!userId) return;

        setIsLoading(true);
        setError(null);

        try {
            const data = await fetchAIRecommendations(8);
            setRecommendations(data.recommendations || []);
        } catch (err) {
            console.error("AI 추천 로드 실패:", err);
            setError("AI 추천을 불러오는 중 오류가 발생했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleRetryLoad = () => {
        loadRecommendations();
    };

    if (!userId) {
        return null; // 로그인하지 않은 사용자에게는 표시하지 않음
    }

    if (isLoading) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="flex items-center mb-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-3">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">AI 맞춤 추천</h3>
                </div>

                <div className="animate-pulse">
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                        {[...Array(4)].map((_, index) => (
                            <div key={index} className="bg-gray-200 dark:bg-gray-700 h-48 rounded-lg"></div>
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    if (error || (aiServiceStatus && !aiServiceStatus.aiServiceAvailable)) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="flex items-center mb-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-3">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">AI 맞춤 추천</h3>
                </div>

                <div className="text-center py-8">
                    <div className="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 mb-4">
                        {error || "AI 추천 서비스를 사용할 수 없습니다."}
                    </p>
                    <button
                        onClick={handleRetryLoad}
                        className="inline-flex items-center px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 transition-colors"
                    >
                        <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                        </svg>
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    if (recommendations.length === 0) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="flex items-center mb-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-3">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">AI 맞춤 추천</h3>
                </div>

                <div className="text-center py-8">
                    <div className="w-16 h-16 mx-auto mb-4 bg-purple-100 dark:bg-purple-900 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                        </svg>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 mb-2">아직 추천할 차량이 없습니다</p>
                    <p className="text-sm text-gray-400 dark:text-gray-500 mb-4">
                        차량을 즐겨찾기에 추가하면 더 정확한 AI 추천을 받을 수 있습니다.
                    </p>
                    <Link
                        to="/cars"
                        className="inline-flex items-center px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 transition-colors"
                    >
                        차량 둘러보기
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center">
                    <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-3">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                    </div>
                    <div>
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">AI 맞춤 추천</h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                            당신의 취향을 분석한 개인화 추천
                        </p>
                    </div>
                </div>

                <Link
                    to="/ai-recommendations"
                    className="text-sm text-purple-600 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium"
                >
                    전체보기 →
                </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {recommendations.slice(0, 4).map((recommendation, index) => {
                    const car = recommendation.car;
                    return (
                        <div key={car.id || index} className="relative">
                            <CarCard
                                car={car}
                                isFavorite={favorites && favorites.has ? favorites.has(car.id) : false}
                                onToggleFavorite={onToggleFavorite}
                            />

                            {/* AI 추천 배지 */}
                            <div className="absolute top-2 left-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-2 py-1 rounded-full font-medium">
                                AI 추천
                            </div>

                            {/* 추천 이유 */}
                            {recommendation.recommendationReason && (
                                <div className="mt-2 p-2 bg-purple-50 dark:bg-purple-900 dark:bg-opacity-20 rounded-md">
                                    <p className="text-xs text-purple-700 dark:text-purple-300">
                                        💡 {recommendation.recommendationReason}
                                    </p>
                                    {recommendation.similarityScore && (
                                        <div className="flex items-center mt-1">
                                            <span className="text-xs text-purple-600 dark:text-purple-400 mr-1">
                                                매칭도:
                                            </span>
                                            <div className="flex-1 bg-purple-200 dark:bg-purple-700 rounded-full h-1">
                                                <div
                                                    className="bg-purple-500 h-1 rounded-full"
                                                    style={{ width: `${Math.round(recommendation.similarityScore * 100)}%` }}
                                                ></div>
                                            </div>
                                            <span className="text-xs text-purple-600 dark:text-purple-400 ml-1">
                                                {Math.round(recommendation.similarityScore * 100)}%
                                            </span>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {recommendations.length > 4 && (
                <div className="mt-6 text-center">
                    <Link
                        to="/ai-recommendations"
                        className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all duration-200 transform hover:scale-105"
                    >
                        <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                        더 많은 AI 추천 보기
                    </Link>
                </div>
            )}
        </div>
    );
};

export default AIRecommendations;