import React from "react";

const Icon = ({ d }) => (
  <svg
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.75"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    {d}
  </svg>
);

const FEATURES = [
  {
    title: "정밀 검색 필터",
    body: "모델, 가격, 연식, 주행거리, 연료, 지역 등 다양한 조건으로 원하는 차량을 빠르게 좁혀갈 수 있습니다.",
    icon: (
      <Icon
        d={
          <>
            <circle cx="11" cy="11" r="7" />
            <line x1="20" y1="20" x2="16.5" y2="16.5" />
          </>
        }
      />
    ),
  },
  {
    title: "즐겨찾기 기반 AI 추천",
    body: "즐겨찾기에 차량을 추가하면 머신러닝 모델이 취향을 학습해 비슷한 차량을 추천합니다.",
    icon: (
      <Icon
        d={
          <path d="M12 21s-7-4.5-7-11a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 6.5-7 11-7 11z" />
        }
      />
    ),
  },
  {
    title: "차량 비교 & 시장 분석",
    body: "관심 차량을 나란히 비교하고, 모델별 연식 가격 분포를 차트로 확인해 시세를 파악할 수 있습니다.",
    icon: (
      <Icon
        d={
          <>
            <line x1="4" y1="20" x2="4" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="20" y1="20" x2="20" y2="14" />
          </>
        }
      />
    ),
  },
];

const FeatureSection = () => {
  return (
    <section className="border-b border-line">
      <div className="container-page py-16 sm:py-20">
        <div className="max-w-2xl">
          <span className="section-eyebrow">주요 기능</span>
          <h2 className="mt-3 section-title">
            중고차 구매에 필요한 도구들
          </h2>
        </div>

        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-5">
          {FEATURES.map((f) => (
            <div key={f.title} className="card p-6 card-hover">
              <div className="grid h-10 w-10 place-items-center rounded-md bg-brand-subtle text-brand">
                {f.icon}
              </div>
              <h3 className="mt-4 text-base font-semibold text-fg">{f.title}</h3>
              <p className="mt-2 text-sm text-fg-muted leading-relaxed">
                {f.body}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeatureSection;
