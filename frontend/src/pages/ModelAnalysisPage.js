import React, { useState } from "react";
import { useParams } from "react-router-dom";
import PriceAnalysisChart from "../components/analytics/PriceAnalysisChart";

const ModelAnalysisPage = () => {
  const { model } = useParams();
  const [draft, setDraft] = useState(model || "");
  const [current, setCurrent] = useState(model || "");

  const submit = (e) => {
    e.preventDefault();
    setCurrent(draft.trim());
  };

  return (
    <main className="container-page py-10 sm:py-14">
      <header className="mb-8">
        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
          시장 분석
        </h1>
        <p className="mt-2 text-sm text-fg-muted">
          모델별 연식 가격 분포를 확인하세요.
        </p>
      </header>

      <form onSubmit={submit} className="card p-5 mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
          <div className="md:col-span-3">
            <label htmlFor="m" className="input-label">
              차량 모델
            </label>
            <input
              id="m"
              type="text"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="예: 아반떼, 쏘나타, 그랜저"
              className="input"
            />
          </div>
          <button type="submit" className="btn btn-primary">
            분석하기
          </button>
        </div>
      </form>

      {current ? (
        <PriceAnalysisChart modelName={current} />
      ) : (
        <div className="card p-12 text-center">
          <p className="text-sm text-fg-muted">
            위에 모델명을 입력하면 가격 분포를 보여드립니다.
          </p>
        </div>
      )}

      {/* 안내 */}
      <section className="mt-10 grid grid-cols-1 md:grid-cols-2 gap-5">
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-fg mb-3">
            그래프 보는 법
          </h3>
          <ul className="space-y-2 text-sm text-fg-muted">
            <li>• 선은 연식별 평균가입니다.</li>
            <li>• 음영 영역은 최저~최고 가격 범위입니다.</li>
            <li>• 모델명을 더 구체적으로 입력하면 정확도가 올라갑니다.</li>
            <li>• 통계 신뢰도를 위해 9,999만원 매물은 제외됩니다.</li>
          </ul>
        </div>
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-fg mb-3">
            연식 표기 정리
          </h3>
          <p className="text-sm text-fg-muted mb-3">
            크롤링 시 다양한 표기가 들어오므로 다음과 같이 정규화합니다.
          </p>
          <ul className="space-y-1.5 text-sm">
            <li className="flex items-baseline justify-between border-b border-line pb-1.5">
              <span className="text-fg-muted">2301, 2303</span>
              <span className="text-fg font-medium">→ 2023</span>
            </li>
            <li className="flex items-baseline justify-between border-b border-line pb-1.5">
              <span className="text-fg-muted">2211, 2209</span>
              <span className="text-fg font-medium">→ 2022</span>
            </li>
            <li className="flex items-baseline justify-between border-b border-line pb-1.5">
              <span className="text-fg-muted">9901, 9905</span>
              <span className="text-fg font-medium">→ 1999</span>
            </li>
            <li className="flex items-baseline justify-between">
              <span className="text-fg-muted">8812</span>
              <span className="text-fg font-medium">→ 1988</span>
            </li>
          </ul>
        </div>
      </section>
    </main>
  );
};

export default ModelAnalysisPage;
