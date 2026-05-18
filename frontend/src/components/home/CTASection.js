import React from "react";
import { Link } from "react-router-dom";

const CTASection = ({ userId, username }) => {
  const isLoggedIn = !!userId;
  return (
    <section className="border-b border-line">
      <div className="container-page py-16 sm:py-20">
        <div className="card p-8 sm:p-12 text-center">
          <h2 className="text-2xl sm:text-3xl font-semibold tracking-tight text-fg">
            {isLoggedIn
              ? `${username || "회원"}님께 맞는 차량을 찾아보세요`
              : "지금 시작해 보세요"}
          </h2>
          <p className="mt-3 text-sm sm:text-base text-fg-muted max-w-xl mx-auto">
            {isLoggedIn
              ? "다양한 필터로 차량을 검색하고, 마음에 드는 차량은 즐겨찾기에 저장하면 AI 추천이 정확해집니다."
              : "회원가입 후 즐겨찾기 기능을 이용하면 AI가 취향을 학습해 맞춤 추천을 제공합니다."}
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link to="/cars" className="btn btn-primary btn-lg">
              차량 검색하기
            </Link>
            {!isLoggedIn && (
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

export default CTASection;
