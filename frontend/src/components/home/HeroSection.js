import React from "react";
import { Link } from "react-router-dom";

const HeroSection = ({ userId, username }) => {
  const isLoggedIn = !!userId;

  return (
    <section className="border-b border-line">
      <div className="container-page py-16 sm:py-24">
        <div className="max-w-3xl">
          <span className="section-eyebrow">AutoFinder</span>
          <h1 className="mt-3 text-4xl sm:text-5xl lg:text-6xl font-semibold tracking-tight text-fg leading-[1.1]">
            {isLoggedIn ? (
              <>
                {username || "회원"}님,
                <br />
                다시 오신 걸 환영합니다.
              </>
            ) : (
              <>
                중고차 검색,
                <br />
                조금 더 똑똑하게.
              </>
            )}
          </h1>
          <p className="mt-6 text-base sm:text-lg text-fg-muted max-w-2xl leading-relaxed">
            {isLoggedIn
              ? "저장한 차량을 기반으로 추천이 실시간으로 갱신됩니다. 새로 등록된 매물을 확인해 보세요."
              : "다양한 조건으로 차량을 검색하고, 즐겨찾기에 추가하면 AI가 취향을 학습해 맞춤 추천을 제공합니다."}
          </p>

          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/cars" className="btn btn-primary btn-lg">
              차량 검색하기
            </Link>
            {isLoggedIn ? (
              <Link to="/favorites" className="btn btn-secondary btn-lg">
                내 즐겨찾기
              </Link>
            ) : (
              <Link to="/register" className="btn btn-secondary btn-lg">
                회원가입
              </Link>
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
