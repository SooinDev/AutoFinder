// src/components/ai/UserPreferenceAnalysis.js
import React, { useState, useEffect } from 'react';
import { fetchUserPreferenceAnalysis } from '../../api/aiServices';

const UserPreferenceAnalysis = ({ userId }) => {
    const [analysis, setAnalysis] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (userId) {
            loadAnalysis();
        }
    }, [userId]);

    const loadAnalysis = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const data = await fetchUserPreferenceAnalysis();
            setAnalysis(data.analysis);
        } catch (err) {
            console.error("선호도 분석 로드 실패:", err);
            setError("선호도 분석을 불러오는 중 오류가 발생했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    if (!userId) {
        return null;
    }

    if (isLoading) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="animate-pulse">
                    <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded mb-4"></div>
                    <div className="space-y-3">
                        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
                        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-2/3"></div>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="text-center py-8">
                    <div className="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 mb-4">{error}</p>
                    <button
                        onClick={loadAnalysis}
                        className="inline-flex items-center px-4 py-2 bg-teal-600 text-white rounded-md hover:bg-teal-700 transition-colors"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    if (!analysis || analysis.message) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
                <div className="flex items-center mb-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-blue-500 to-teal-500 rounded-lg flex items-center justify-center mr-3">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 00-2 2z" />
                        </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">선호도 분석</h3>
                </div>

                <div className="text-center py-8">
                    <div className="w-16 h-16 mx-auto mb-4 bg-blue-100 dark:bg-blue-900 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                        </svg>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400">
                        {analysis?.message || "즐겨찾기한 차량이 있으면 선호도 분석이 가능합니다."}
                    </p>
                </div>
            </div>
        );
    }

    const formatPrice = (price) => {
        return price ? `${Math.round(price).toLocaleString()}만원` : '정보 없음';
    };

    const formatMileage = (mileage) => {
        return mileage ? `${Math.round(mileage).toLocaleString()}km` : '정보 없음';
    };

    return (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 transition-colors duration-300">
            <div className="flex items-center mb-6">
                <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-blue-500 to-teal-500 rounded-lg flex items-center justify-center mr-3">
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 00-2 2z" />
                    </svg>
                </div>
                <div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">선호도 분석</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                        즐겨찾기한 차량을 바탕으로 한 취향 분석
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* 가격 선호도 */}
                {analysis.price_preferences && (
                    <div className="bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-900 dark:to-emerald-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                            </svg>
                            가격 선호도
                        </h4>
                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">평균 선호 가격:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {formatPrice(analysis.price_preferences.avg_price)}
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">가격 범위:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {analysis.price_preferences.price_range}
                                </span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 연식 선호도 */}
                {analysis.year_preferences && (
                    <div className="bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900 dark:to-indigo-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            연식 선호도
                        </h4>
                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">평균 연식:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {Math.round(analysis.year_preferences.avg_year)}년
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">연식 범위:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {analysis.year_preferences.preferred_year_range}
                                </span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 주행거리 선호도 */}
                {analysis.mileage_preferences && (
                    <div className="bg-gradient-to-br from-purple-50 to-pink-50 dark:from-purple-900 dark:to-pink-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                            주행거리 선호도
                        </h4>
                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">평균 주행거리:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {formatMileage(analysis.mileage_preferences.avg_mileage)}
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">주행거리 범위:</span>
                                <span className="font-medium text-gray-900 dark:text-white">
                                    {analysis.mileage_preferences.mileage_range}
                                </span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 연료 타입 선호도 */}
                {analysis.fuel_preferences && Object.keys(analysis.fuel_preferences).length > 0 && (
                    <div className="bg-gradient-to-br from-orange-50 to-red-50 dark:from-orange-900 dark:to-red-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                            </svg>
                            연료 선호도
                        </h4>
                        <div className="space-y-2">
                            {Object.entries(analysis.fuel_preferences)
                                .sort(([,a], [,b]) => b - a)
                                .slice(0, 3)
                                .map(([fuel, count]) => (
                                    <div key={fuel} className="flex justify-between items-center">
                                        <span className="text-sm text-gray-600 dark:text-gray-400">{fuel}</span>
                                        <div className="flex items-center">
                                            <div className="w-16 bg-orange-200 dark:bg-orange-700 rounded-full h-2 mr-2">
                                                <div
                                                    className="bg-orange-500 h-2 rounded-full"
                                                    style={{ width: `${(count / Math.max(...Object.values(analysis.fuel_preferences))) * 100}%` }}
                                                ></div>
                                            </div>
                                            <span className="text-sm font-medium text-gray-900 dark:text-white">{count}</span>
                                        </div>
                                    </div>
                                ))}
                        </div>
                    </div>
                )}

                {/* 지역 선호도 */}
                {analysis.region_preferences && Object.keys(analysis.region_preferences).length > 0 && (
                    <div className="bg-gradient-to-br from-teal-50 to-cyan-50 dark:from-teal-900 dark:to-cyan-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-teal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                            </svg>
                            지역 선호도
                        </h4>
                        <div className="space-y-2">
                            {Object.entries(analysis.region_preferences)
                                .sort(([,a], [,b]) => b - a)
                                .slice(0, 3)
                                .map(([region, count]) => (
                                    <div key={region} className="flex justify-between items-center">
                                        <span className="text-sm text-gray-600 dark:text-gray-400">{region}</span>
                                        <div className="flex items-center">
                                            <div className="w-16 bg-teal-200 dark:bg-teal-700 rounded-full h-2 mr-2">
                                                <div
                                                    className="bg-teal-500 h-2 rounded-full"
                                                    style={{ width: `${(count / Math.max(...Object.values(analysis.region_preferences))) * 100}%` }}
                                                ></div>
                                            </div>
                                            <span className="text-sm font-medium text-gray-900 dark:text-white">{count}</span>
                                        </div>
                                    </div>
                                ))}
                        </div>
                    </div>
                )}

                {/* 브랜드 선호도 */}
                {analysis.brand_preferences && Object.keys(analysis.brand_preferences).length > 0 && (
                    <div className="bg-gradient-to-br from-yellow-50 to-amber-50 dark:from-yellow-900 dark:to-amber-900 dark:bg-opacity-20 p-4 rounded-lg">
                        <h4 className="font-medium text-gray-900 dark:text-white mb-3 flex items-center">
                            <svg className="w-5 h-5 mr-2 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                            </svg>
                            선호 브랜드
                        </h4>
                        <div className="space-y-2">
                            {Object.entries(analysis.brand_preferences)
                                .sort(([,a], [,b]) => b - a)
                                .slice(0, 3)
                                .map(([brand, count]) => (
                                    <div key={brand} className="flex justify-between items-center">
                                        <span className="text-sm text-gray-600 dark:text-gray-400 truncate mr-2">{brand}</span>
                                        <div className="flex items-center">
                                            <div className="w-16 bg-yellow-200 dark:bg-yellow-700 rounded-full h-2 mr-2">
                                                <div
                                                    className="bg-yellow-500 h-2 rounded-full"
                                                    style={{ width: `${(count / Math.max(...Object.values(analysis.brand_preferences))) * 100}%` }}
                                                ></div>
                                            </div>
                                            <span className="text-sm font-medium text-gray-900 dark:text-white">{count}</span>
                                        </div>
                                    </div>
                                ))}
                        </div>
                    </div>
                )}
            </div>

            <div className="mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                        이 분석은 즐겨찾기한 차량을 바탕으로 생성되었습니다.
                    </p>
                    <button
                        onClick={loadAnalysis}
                        className="text-sm text-teal-600 dark:text-teal-400 hover:text-teal-800 dark:hover:text-teal-300 font-medium"
                    >
                        새로고침
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UserPreferenceAnalysis;