// src/pages/AIRecommendationsPage.js
import React, { useState, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { fetchAIRecommendations } from '../api/aiServices';
import CarCard from '../components/car/CarCard';
import Pagination from '../components/common/Pagination';

const AIRecommendationsPage = ({ userId, favorites, setFavorites, onToggleFavorite }) => {
    const [recommendations, setRecommendations] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const history = useHistory();
    const pageSize = 12;

    useEffect(() => {
        if (!userId) {
            history.push('/login');
            return;
        }
        loadRecommendations();
    }, [userId, history]);

    const loadRecommendations = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const data = await fetchAIRecommendations(50); // 더 많은 추천 로드
            setRecommendations(data.recommendations || []);
            setTotalPages(Math.ceil((data.recommendations || []).length / pageSize));
        } catch (err) {
            console.error("AI 추천 로드 실패:", err);
            setError("AI 추천을 불러오는 중 오류가 발생했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    const handlePageChange = (newPage) => {
        setCurrentPage(newPage);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const getCurrentPageRecommendations = () => {
        const startIndex = currentPage * pageSize;
        const endIndex = startIndex + pageSize;
        return recommendations.slice(startIndex, endIndex);
    };

    const currentRecommendations = getCurrentPageRecommendations();

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 섹션 */}
            <div className="mb-8">
                <div className="flex items-center mb-4">
                    <div className="flex-shrink-0 w-10 h-10 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-4">
                        <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                        </svg>
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white sm:text-3xl">
                            AI 맞춤 추천
                        </h1>
                        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                            당신의 취향을 분석한 개인화 차량 추천
                        </p>
                    </div>
                </div>

                {/* 추천 정보 카드 */}
                <div className="bg-gradient-to-r from-purple-100 to-pink-100 dark:from-purple-900 dark:to-pink-900 dark:bg-opacity-20 rounded-lg p-6 mb-8">
                    <div className="flex items-start space-x-4">
                        <div className="flex-shrink-0">
                            <svg className="w-8 h-8 text-purple-600 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold text-purple-900 dark:text-purple-100 mb-2">
                                AI 추천 시스템 작동 원리
                            </h3>
                            <p className="text-purple-700 dark:text-purple-300 text-sm mb-3">
                                오토파인더의 AI는 당신이 즐겨찾기한 차량들을 분석하여 비슷한 취향의 차량을 찾아드립니다.
                            </p>
                            <ul className="text-purple-600 dark:text-purple-400 text-sm space-y-1">
                                <li>• 가격대, 연식, 브랜드 선호도 분석</li>
                                <li>• 주행거리, 연료타입, 지역 패턴 학습</li>
                                <li>• 유사도 점수 기반 정확한 매칭</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            {/* 로딩 상태 */}
            {isLoading && (
                <div className="py-12 flex flex-col items-center justify-center">
                    <svg className="animate-spin h-10 w-10 text-purple-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="text-sm text-gray-500 dark:text-gray-400">AI가 맞춤 추천을 생성하고 있습니다...</p>
                </div>
            )}

            {/* 오류 상태 */}
            {error && (
                <div className="rounded-md bg-red-50 dark:bg-red-900 dark:bg-opacity-20 p-4 mb-6">
                    <div className="flex">
                        <div className="flex-shrink-0">
                            <svg className="h-5 w-5 text-red-400 dark:text-red-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                            </svg>
                        </div>
                        <div className="ml-3">
                            <p className="text-sm text-red-700 dark:text-red-400">{error}</p>
                            <button
                                onClick={loadRecommendations}
                                className="mt-2 text-sm text-red-800 dark:text-red-300 underline hover:no-underline"
                            >
                                다시 시도
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* 추천 결과 */}
            {!isLoading && !error && (
                <>
                    {currentRecommendations.length > 0 ? (
                        <>
                            {/* 통계 정보 */}
                            <div className="flex items-center justify-between mb-6">
                                <div className="flex items-center space-x-4">
                                    <p className="text-sm text-gray-500 dark:text-gray-400">
                                        총 <span className="font-semibold text-gray-900 dark:text-white">{recommendations.length}</span>개의 맞춤 추천
                                    </p>
                                    {recommendations.length > 0 && (
                                        <div className="flex items-center space-x-2">
                                            <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                                            <span className="text-sm text-green-600 dark:text-green-400 font-medium">
                                                AI 추천 활성
                                            </span>
                                        </div>
                                    )}
                                </div>
                                <button
                                    onClick={loadRecommendations}
                                    className="text-sm text-purple-600 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium"
                                >
                                    새로고침
                                </button>
                            </div>

                            {/* 추천 차량 그리드 */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
                                {currentRecommendations.map((recommendation, index) => {
                                    const car = recommendation.car;
                                    return (
                                        <div key={car.id || index} className="relative">
                                            <CarCard
                                                car={car}
                                                isFavorite={favorites && favorites.has ? favorites.has(car.id) : false}
                                                onToggleFavorite={onToggleFavorite}
                                            />

                                            {/* AI 추천 배지 및 점수 */}
                                            <div className="absolute top-2 left-2 space-y-1">
                                                <div className="bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs px-2 py-1 rounded-full font-medium">
                                                    AI 추천
                                                </div>
                                                {recommendation.similarityScore && (
                                                    <div className="bg-black bg-opacity-70 text-white text-xs px-2 py-1 rounded-full">
                                                        {Math.round(recommendation.similarityScore * 100)}% 매칭
                                                    </div>
                                                )}
                                            </div>

                                            {/* 추천 이유 */}
                                            {recommendation.recommendationReason && (
                                                <div className="mt-3 p-3 bg-purple-50 dark:bg-purple-900 dark:bg-opacity-20 rounded-md">
                                                    <div className="flex items-start space-x-2">
                                                        <svg className="flex-shrink-0 w-4 h-4 text-purple-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                                        </svg>
                                                        <p className="text-xs text-purple-700 dark:text-purple-300">
                                                            {recommendation.recommendationReason}
                                                        </p>
                                                    </div>

                                                    {recommendation.similarityScore && (
                                                        <div className="flex items-center mt-2">
                                                            <span className="text-xs text-purple-600 dark:text-purple-400 mr-2">
                                                                매칭도:
                                                            </span>
                                                            <div className="flex-1 bg-purple-200 dark:bg-purple-700 rounded-full h-1.5">
                                                                <div
                                                                    className="bg-gradient-to-r from-purple-500 to-pink-500 h-1.5 rounded-full transition-all duration-500"
                                                                    style={{ width: `${Math.round(recommendation.similarityScore * 100)}%` }}
                                                                ></div>
                                                            </div>
                                                            <span className="text-xs text-purple-600 dark:text-purple-400 ml-2 font-medium">
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

                            {/* 페이지네이션 */}
                            {recommendations.length > pageSize && (
                                <Pagination
                                    currentPage={currentPage}
                                    totalPages={totalPages}
                                    onPageChange={handlePageChange}
                                />
                            )}
                        </>
                    ) : (
                        /* 추천 결과 없음 */
                        <div className="text-center py-12">
                            <div className="w-24 h-24 mx-auto mb-6 bg-gradient-to-br from-purple-100 to-pink-100 dark:from-purple-900 dark:to-pink-900 rounded-full flex items-center justify-center">
                                <svg className="w-12 h-12 text-purple-500 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                                </svg>
                            </div>
                            <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                                아직 추천할 차량이 없습니다
                            </h3>
                            <p className="text-gray-500 dark:text-gray-400 mb-6 max-w-md mx-auto">
                                차량을 즐겨찾기에 추가하면 AI가 당신의 취향을 학습하여
                                더 정확한 맞춤 추천을 제공할 수 있습니다.
                            </p>
                            <div className="space-x-4">
                                <button
                                    onClick={() => history.push('/cars')}
                                    className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all duration-200"
                                >
                                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                    </svg>
                                    차량 둘러보기
                                </button>
                                <button
                                    onClick={() => history.push('/favorites')}
                                    className="inline-flex items-center px-6 py-3 border border-purple-300 dark:border-purple-600 text-purple-700 dark:text-purple-300 rounded-lg hover:bg-purple-50 dark:hover:bg-purple-900 dark:hover:bg-opacity-20 transition-all duration-200"
                                >
                                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                                    </svg>
                                    내 즐겨찾기
                                </button>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default AIRecommendationsPage;