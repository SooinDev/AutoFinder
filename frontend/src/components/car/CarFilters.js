import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const FUELS = ["가솔린", "디젤", "LPG", "하이브리드", "전기"];

const CarFilters = ({ filters, setFilters, onSearch, onReset }) => {
  const location = useLocation();
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    const sp = new URLSearchParams(location.search);
    const next = { ...filters };
    let changed = false;
    Object.keys(filters).forEach((k) => {
      const v = sp.get(k);
      if (v !== null && next[k] !== v) {
        next[k] = v;
        changed = true;
      }
    });
    if (changed) setFilters(next);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.search]);

  const years = Array.from({ length: 25 }, (_, i) => {
    const y = new Date().getFullYear() - i;
    return y.toString();
  });

  const onChange = (e) => {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  };

  const onSubmit = (e) => {
    e.preventDefault();
    onSearch();
  };

  const activeCount = Object.values(filters).filter(Boolean).length;

  return (
    <form onSubmit={onSubmit} className="card p-5 mb-8">
      <div className="flex items-center justify-between mb-5">
        <h3 className="text-base font-semibold text-fg flex items-center gap-2">
          검색 필터
          {activeCount > 0 && (
            <span className="badge badge-brand">{activeCount}개 적용됨</span>
          )}
        </h3>
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="text-sm text-fg-muted hover:text-fg"
        >
          {expanded ? "간단히 보기" : "상세 필터 +"}
        </button>
      </div>

      {/* base row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label htmlFor="model" className="input-label">
            모델
          </label>
          <input
            id="model"
            name="model"
            type="text"
            placeholder="예: 아반떼, 쏘나타"
            value={filters.model || ""}
            onChange={onChange}
            className="input"
          />
        </div>
        <div>
          <label htmlFor="year" className="input-label">
            연식
          </label>
          <select
            id="year"
            name="year"
            value={filters.year || ""}
            onChange={onChange}
            className="input"
          >
            <option value="">전체</option>
            {years.map((y) => (
              <option key={y} value={y}>
                {y}년
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="fuel" className="input-label">
            연료
          </label>
          <select
            id="fuel"
            name="fuel"
            value={filters.fuel || ""}
            onChange={onChange}
            className="input"
          >
            <option value="">전체</option>
            {FUELS.map((f) => (
              <option key={f} value={f}>
                {f}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* advanced */}
      {expanded && (
        <div className="mt-5 pt-5 border-t border-line grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="input-label">가격 범위 (만원)</label>
            <div className="grid grid-cols-2 gap-2">
              <input
                name="minPrice"
                type="number"
                placeholder="최소"
                value={filters.minPrice || ""}
                onChange={onChange}
                className="input"
              />
              <input
                name="maxPrice"
                type="number"
                placeholder="최대"
                value={filters.maxPrice || ""}
                onChange={onChange}
                className="input"
              />
            </div>
          </div>

          <div>
            <label className="input-label">주행거리 (km)</label>
            <div className="grid grid-cols-2 gap-2">
              <input
                name="minMileage"
                type="number"
                placeholder="최소"
                value={filters.minMileage || ""}
                onChange={onChange}
                className="input"
              />
              <input
                name="maxMileage"
                type="number"
                placeholder="최대"
                value={filters.maxMileage || ""}
                onChange={onChange}
                className="input"
              />
            </div>
          </div>

          <div className="md:col-span-2">
            <label htmlFor="region" className="input-label">
              지역
            </label>
            <input
              id="region"
              name="region"
              type="text"
              placeholder="예: 서울, 경기"
              value={filters.region || ""}
              onChange={onChange}
              className="input"
            />
          </div>
        </div>
      )}

      <div className="mt-5 pt-5 border-t border-line flex items-center justify-end gap-2">
        <button type="button" onClick={onReset} className="btn btn-ghost">
          초기화
        </button>
        <button type="submit" className="btn btn-primary">
          검색
        </button>
      </div>
    </form>
  );
};

export default CarFilters;
