import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchAIRecommendations, fetchAIServiceStatus } from "../../api/aiServices";
import CarCard from "../car/CarCard";

const SparkleIcon = () => (
  <svg
    width="18"
    height="18"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.75"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1" />
  </svg>
);

const Panel = ({ title, subtitle, action, children }) => (
  <section className="card overflow-hidden">
    <header className="p-6 border-b border-line flex items-start justify-between gap-4">
      <div>
        <div className="flex items-center gap-2.5">
          <span className="grid h-8 w-8 place-items-center rounded-md bg-brand-subtle text-brand">
            <SparkleIcon />
          </span>
          <h3 className="text-base font-semibold text-fg">{title}</h3>
        </div>
        {subtitle && <p className="mt-1 ml-10 text-sm text-fg-muted">{subtitle}</p>}
      </div>
      {action}
    </header>
    <div className="p-6">{children}</div>
  </section>
);

const AIRecommendations = ({ userId, favorites, onToggleFavorite }) => {
  const [recs, setRecs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState(null);

  useEffect(() => {
    if (!userId) return;
    (async () => {
      try {
        const s = await fetchAIServiceStatus();
        setStatus(s);
      } catch {
        setStatus({ aiServiceAvailable: false });
      }
    })();
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const load = async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAIRecommendations(8);
      setRecs(data.recommendations || []);
    } catch (err) {
      console.error(err);
      setError("AI 추천을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (!userId) return null;

  if (loading) {
    return (
      <Panel title="AI 맞춤 추천" subtitle="즐겨찾기 기반 개인화 추천">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card overflow-hidden animate-pulse">
              <div className="aspect-[4/3] bg-bg-inset" />
              <div className="p-4 space-y-2">
                <div className="h-4 bg-bg-inset rounded w-3/4" />
                <div className="h-4 bg-bg-inset rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </Panel>
    );
  }

  if (error || (status && !status.aiServiceAvailable)) {
    return (
      <Panel title="AI 맞춤 추천" subtitle="즐겨찾기 기반 개인화 추천">
        <div className="py-8 text-center">
          <p className="text-sm text-fg-muted">
            {error || "AI 서비스를 사용할 수 없습니다."}
          </p>
          <button onClick={load} className="mt-4 btn btn-secondary btn-sm">
            다시 시도
          </button>
        </div>
      </Panel>
    );
  }

  if (recs.length === 0) {
    return (
      <Panel title="AI 맞춤 추천" subtitle="즐겨찾기 기반 개인화 추천">
        <div className="py-8 text-center">
          <p className="text-sm text-fg">아직 추천할 차량이 없습니다.</p>
          <p className="mt-1 text-sm text-fg-muted">
            차량을 즐겨찾기에 추가하면 AI가 학습을 시작합니다.
          </p>
          <Link to="/cars" className="mt-4 inline-flex btn btn-primary btn-sm">
            차량 둘러보기
          </Link>
        </div>
      </Panel>
    );
  }

  return (
    <Panel
      title="AI 맞춤 추천"
      subtitle="즐겨찾기를 기반으로 분석한 추천 차량입니다."
      action={
        <Link
          to="/ai-recommendations"
          className="hidden sm:inline-flex link text-sm font-medium"
        >
          전체 보기 →
        </Link>
      }
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {recs.slice(0, 4).map((r) => {
          const car = r.car;
          const match = r.similarityScore
            ? Math.round(r.similarityScore * 100)
            : null;
          return (
            <div key={car.id} className="relative">
              <CarCard
                car={car}
                isFavorite={favorites?.has?.(car.id) || false}
                onToggleFavorite={onToggleFavorite}
                showCompareButton={false}
              />
              {match !== null && (
                <div className="absolute top-3 left-3">
                  <span className="badge badge-brand">
                    {match}% 매칭
                  </span>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </Panel>
  );
};

export default AIRecommendations;
