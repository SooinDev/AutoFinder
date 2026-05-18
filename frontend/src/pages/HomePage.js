import React from "react";
import { Link } from "react-router-dom";
import HeroSection from "../components/home/HeroSection";
import FeatureSection from "../components/home/FeatureSection";
import CTASection from "../components/home/CTASection";
import CarListPage from "./CarListPage";
import UserDashboard from "../components/dashboard/UserDashboard";
import AIRecommendations from "../components/ai/AIRecommendations";
import UserPreferenceAnalysis from "../components/ai/UserPreferenceAnalysis";

const HomePage = ({ userId, username, favorites, setFavorites, onToggleFavorite }) => {
  return (
    <>
      <HeroSection userId={userId} username={username} />

      {/* 로그인한 사용자 전용 */}
      {userId && (
        <section className="border-b border-line">
          <div className="container-page py-12 sm:py-16 space-y-8">
            <UserDashboard userId={userId} username={username} />
            <AIRecommendations
              userId={userId}
              favorites={favorites}
              setFavorites={setFavorites}
              onToggleFavorite={onToggleFavorite}
            />
            <UserPreferenceAnalysis userId={userId} />
          </div>
        </section>
      )}

      <FeatureSection />

      {/* 최신 차량 */}
      <section className="border-b border-line">
        <div className="container-page py-16 sm:py-20">
          <div className="flex items-end justify-between mb-8">
            <div>
              <span className="section-eyebrow">최신 등록 차량</span>
              <h2 className="mt-3 section-title">새로 들어온 매물</h2>
            </div>
            <Link
              to="/cars"
              className="hidden sm:inline-flex link text-sm font-medium"
            >
              전체 보기 →
            </Link>
          </div>

          <CarListPage
            userId={userId}
            favorites={favorites}
            setFavorites={setFavorites}
            isHomePage
          />

          <div className="mt-8 sm:hidden text-center">
            <Link to="/cars" className="btn btn-secondary">
              전체 차량 보기
            </Link>
          </div>
        </div>
      </section>

      <CTASection userId={userId} username={username} />
    </>
  );
};

export default HomePage;
