import React, { useEffect, useState } from "react";
import { fetchUserPreferenceAnalysis } from "../../api/aiServices";

const Bar = ({ value, max }) => (
  <div className="h-1.5 w-full bg-bg-inset rounded-full overflow-hidden">
    <div
      className="h-full bg-brand transition-all duration-500"
      style={{ width: `${max ? (value / max) * 100 : 0}%` }}
    />
  </div>
);

const DistList = ({ items, max }) => (
  <ul className="space-y-3">
    {items.map(([k, v]) => (
      <li key={k}>
        <div className="flex items-baseline justify-between mb-1.5">
          <span className="text-sm text-fg truncate">{k}</span>
          <span className="text-xs text-fg-muted ml-3">{v}</span>
        </div>
        <Bar value={v} max={max} />
      </li>
    ))}
  </ul>
);

const StatPair = ({ label, value }) => (
  <div className="flex items-baseline justify-between">
    <dt className="text-xs text-fg-subtle">{label}</dt>
    <dd className="text-sm font-medium text-fg">{value}</dd>
  </div>
);

const Panel = ({ children }) => (
  <section className="card overflow-hidden">
    <header className="p-6 border-b border-line">
      <h3 className="text-base font-semibold text-fg">선호도 분석</h3>
      <p className="mt-1 text-sm text-fg-muted">
        즐겨찾기한 차량을 바탕으로 한 취향 요약입니다.
      </p>
    </header>
    <div className="p-6">{children}</div>
  </section>
);

const UserPreferenceAnalysis = ({ userId }) => {
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!userId) return;
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchUserPreferenceAnalysis();
      setAnalysis(data.analysis);
    } catch (err) {
      console.error(err);
      setError("선호도 분석을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (!userId) return null;

  if (loading) {
    return (
      <Panel>
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-4 bg-bg-inset rounded animate-pulse" />
          ))}
        </div>
      </Panel>
    );
  }

  if (error) {
    return (
      <Panel>
        <div className="py-6 text-center">
          <p className="text-sm text-fg-muted">{error}</p>
          <button onClick={load} className="mt-4 btn btn-secondary btn-sm">
            다시 시도
          </button>
        </div>
      </Panel>
    );
  }

  if (!analysis || analysis.message) {
    return (
      <Panel>
        <p className="text-sm text-fg-muted text-center py-4">
          {analysis?.message || "즐겨찾기한 차량이 있으면 선호도 분석이 가능합니다."}
        </p>
      </Panel>
    );
  }

  const formatPrice = (v) =>
    v ? `${Math.round(v).toLocaleString()} 만원` : "—";
  const formatMileage = (v) =>
    v ? `${Math.round(v).toLocaleString()} km` : "—";

  const topN = (obj, n) =>
    Object.entries(obj)
      .sort(([, a], [, b]) => b - a)
      .slice(0, n);

  return (
    <Panel>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {analysis.price_preferences && (
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-fg">가격</h4>
            <dl className="space-y-2">
              <StatPair
                label="평균"
                value={formatPrice(analysis.price_preferences.avg_price)}
              />
              <StatPair
                label="범위"
                value={analysis.price_preferences.price_range || "—"}
              />
            </dl>
          </div>
        )}

        {analysis.year_preferences && (
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-fg">연식</h4>
            <dl className="space-y-2">
              <StatPair
                label="평균"
                value={Math.round(analysis.year_preferences.avg_year) + "년"}
              />
              <StatPair
                label="범위"
                value={analysis.year_preferences.preferred_year_range || "—"}
              />
            </dl>
          </div>
        )}

        {analysis.mileage_preferences && (
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-fg">주행거리</h4>
            <dl className="space-y-2">
              <StatPair
                label="평균"
                value={formatMileage(analysis.mileage_preferences.avg_mileage)}
              />
              <StatPair
                label="범위"
                value={analysis.mileage_preferences.mileage_range || "—"}
              />
            </dl>
          </div>
        )}

        {analysis.fuel_preferences &&
          Object.keys(analysis.fuel_preferences).length > 0 && (
            <div className="space-y-3">
              <h4 className="text-sm font-semibold text-fg">연료</h4>
              <DistList
                items={topN(analysis.fuel_preferences, 4)}
                max={Math.max(...Object.values(analysis.fuel_preferences))}
              />
            </div>
          )}

        {analysis.region_preferences &&
          Object.keys(analysis.region_preferences).length > 0 && (
            <div className="space-y-3">
              <h4 className="text-sm font-semibold text-fg">지역</h4>
              <DistList
                items={topN(analysis.region_preferences, 4)}
                max={Math.max(...Object.values(analysis.region_preferences))}
              />
            </div>
          )}

        {analysis.brand_preferences &&
          Object.keys(analysis.brand_preferences).length > 0 && (
            <div className="space-y-3">
              <h4 className="text-sm font-semibold text-fg">브랜드</h4>
              <DistList
                items={topN(analysis.brand_preferences, 4)}
                max={Math.max(...Object.values(analysis.brand_preferences))}
              />
            </div>
          )}
      </div>
    </Panel>
  );
};

export default UserPreferenceAnalysis;
