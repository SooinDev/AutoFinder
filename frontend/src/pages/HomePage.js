// src/pages/HomePage.js (업데이트된 버전)
import React from 'react';
import HeroSection from '../components/home/HeroSection';
import FeatureSection from '../components/home/FeatureSection';
import CarListPage from './CarListPage';
import TestimonialSection from '../components/home/TestimonialSection';
import StatsSection from '../components/home/StatsSection';
import CTASection from '../components/home/CTASection';
import UserDashboard from '../components/dashboard/UserDashboard';
import AIRecommendations from '../components/ai/AIRecommendations';
import UserPreferenceAnalysis from '../components/ai/UserPreferenceAnalysis';

const HomePage = ({ userId, username, favorites, setFavorites, onToggleFavorite }) => {
    return (
        <>
            <HeroSection userId={userId} username={username} />

            {/* 로그인한 사용자에게만 대시보드와 AI 기능 표시 */}
            {userId && (
                <div className="py-12 bg-gray-50 dark:bg-gray-900">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
                        {/* 사용자 대시보드 */}
                        <UserDashboard userId={userId} username={username} />

                        {/* AI 추천 시스템 */}
                        <AIRecommendations
                            userId={userId}
                            favorites={favorites}
                            setFavorites={setFavorites}
                            onToggleFavorite={onToggleFavorite}
                        />

                        {/* 사용자 선호도 분석 */}
                        <UserPreferenceAnalysis userId={userId} />
                    </div>
                </div>
            )}

            <FeatureSection />

            <div className="py-16 bg-gray-50 dark:bg-gray-900">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="text-center mb-12">
                        <h2 className="text-base text-teal-600 dark:text-teal-400 font-semibold tracking-wide uppercase">차량 둘러보기</h2>
                        <p className="mt-2 text-3xl font-extrabold text-gray-900 dark:text-white sm:text-4xl">
                            최신 등록 차량
                        </p>
                        <p className="mt-4 max-w-2xl text-xl text-gray-500 dark:text-gray-400 mx-auto">
                            오토파인더에 새롭게 등록된 차량들을 확인해보세요.
                        </p>
                    </div>
                    <CarListPage
                        userId={userId}
                        favorites={favorites}
                        setFavorites={setFavorites}
                        isHomePage={true}
                    />
                </div>
            </div>

            <TestimonialSection />
            <StatsSection />
            <CTASection userId={userId} username={username} />
        </>
    );
};

export default HomePage;