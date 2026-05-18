import React, { useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { fetchCarById, fetchCars } from "../api/services";
import { formatNumber } from "../utils/formatters";

const MAX_CARS = 3;

const COMPARE_ROWS = [
  { key: "model", label: "모델", type: "text" },
  { key: "price", label: "가격", type: "price", unit: "만원" },
  { key: "year", label: "연식", type: "text" },
  { key: "mileage", label: "주행거리", type: "number", unit: "km" },
  { key: "fuel", label: "연료", type: "text" },
  { key: "region", label: "지역", type: "text" },
  { key: "carType", label: "차종", type: "text" },
];

const formatCell = (car, item) => {
  const v = car[item.key];
  if (v == null || v === "정보 없음") return "—";
  switch (item.type) {
    case "price":
      return typeof v === "number" ? `${v.toLocaleString()} ${item.unit}` : "—";
    case "number":
      return typeof v === "number" || !isNaN(parseInt(v, 10))
        ? `${formatNumber(v)} ${item.unit}`
        : "—";
    default:
      return v.toString();
  }
};

const getVerdict = (cars, item, idx) => {
  if (item.type !== "price" && item.type !== "number") return null;
  const nums = cars
    .map((c) => c[item.key])
    .map((v) =>
      v == null || v === "정보 없음"
        ? null
        : typeof v === "number"
        ? v
        : parseInt(v, 10)
    )
    .filter((v) => v !== null && !isNaN(v));
  if (nums.length < 2) return null;
  const v = cars[idx][item.key];
  if (v == null || v === "정보 없음") return null;
  const n = typeof v === "number" ? v : parseInt(v, 10);
  if (isNaN(n)) return null;
  const min = Math.min(...nums);
  const max = Math.max(...nums);
  if (item.key === "price" || item.key === "mileage") {
    if (n === min) return "best";
    if (n === max) return "worst";
  }
  return null;
};

const ImageIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.25">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <circle cx="9" cy="9" r="1.5" />
    <path d="m21 15-5-5L5 21" />
  </svg>
);

const CarComparePage = () => {
  const history = useHistory();
  const location = useLocation();
  const [cars, setCars] = useState([]);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const ids = params.get("cars");
    if (ids) {
      const arr = ids
        .split(",")
        .map((i) => parseInt(i, 10))
        .filter((n) => !isNaN(n));
      load(arr);
    } else {
      setCars([]);
    }
  }, [location.search]);

  const load = async (ids) => {
    setLoading(true);
    setError(null);
    try {
      const fetched = await Promise.all(
        ids.slice(0, MAX_CARS).map((id) => fetchCarById(id))
      );
      setCars(fetched.filter(Boolean));
    } catch (err) {
      console.error(err);
      setError("비교 차량을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const onSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) return;
    setSearching(true);
    try {
      const data = await fetchCars({ model: query }, 0, 12);
      setResults(data.content || []);
    } catch (err) {
      console.error(err);
      setError("검색에 실패했습니다.");
    } finally {
      setSearching(false);
    }
  };

  const updateUrl = (next) => {
    if (next.length > 0) {
      history.replace(`/compare?cars=${next.map((c) => c.id).join(",")}`);
    } else {
      history.replace("/compare");
    }
  };

  const add = (car) => {
    if (cars.length >= MAX_CARS) return alert(`최대 ${MAX_CARS}대까지 비교할 수 있습니다.`);
    if (cars.some((c) => c.id === car.id)) return alert("이미 비교 목록에 있는 차량입니다.");
    const next = [...cars, car];
    setCars(next);
    updateUrl(next);
  };

  const remove = (id) => {
    const next = cars.filter((c) => c.id !== id);
    setCars(next);
    updateUrl(next);
  };

  return (
    <main className="container-page py-10 sm:py-14">
      <header className="mb-8">
        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
          차량 비교
        </h1>
        <p className="mt-2 text-sm text-fg-muted">
          최대 {MAX_CARS}대까지 나란히 비교할 수 있습니다.
        </p>
      </header>

      {/* search */}
      <section className="card p-5 mb-8">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-semibold text-fg">비교할 차량 검색</h3>
          <span className="badge">{cars.length}/{MAX_CARS} 선택됨</span>
        </div>

        <form onSubmit={onSearch} className="flex gap-2 mb-4">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="예: 아반떼, 쏘나타"
            className="input flex-1"
          />
          <button
            type="submit"
            disabled={searching}
            className="btn btn-primary"
          >
            {searching ? "검색 중…" : "검색"}
          </button>
        </form>

        {results.length > 0 && (
          <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {results.map((c) => {
              const added = cars.some((x) => x.id === c.id);
              const full = cars.length >= MAX_CARS;
              return (
                <li
                  key={c.id}
                  className="flex items-center gap-3 p-3 border border-line rounded-md hover:border-line-strong transition-colors"
                >
                  <div className="h-14 w-20 bg-bg-inset rounded overflow-hidden flex-shrink-0">
                    {c.imageUrl ? (
                      <img
                        src={c.imageUrl}
                        alt={c.model}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="h-full w-full grid place-items-center text-fg-faint">
                        <ImageIcon />
                      </div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-fg truncate">{c.model}</p>
                    <p className="mt-0.5 text-xs text-fg-muted">
                      {[c.year, c.fuel].filter(Boolean).join(" · ")}
                    </p>
                  </div>
                  <button
                    onClick={() => add(c)}
                    disabled={added || full}
                    className={
                      "btn btn-sm flex-shrink-0 " +
                      (added ? "btn-ghost" : "btn-secondary")
                    }
                  >
                    {added ? "추가됨" : full ? "가득" : "추가"}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {error && (
        <div className="mb-6 card p-4 border-danger/30 bg-danger/5">
          <p className="text-sm text-danger">{error}</p>
        </div>
      )}

      {loading && (
        <div className="py-16 flex flex-col items-center gap-3">
          <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-fg-muted">불러오는 중…</p>
        </div>
      )}

      {!loading && cars.length === 0 && (
        <div className="card p-12 text-center">
          <p className="text-lg font-semibold text-fg">
            비교할 차량을 선택해 주세요
          </p>
          <p className="mt-2 text-sm text-fg-muted">
            위의 검색을 이용하거나, 차량 목록에서 비교 버튼을 눌러 추가하세요.
          </p>
          <button
            onClick={() => history.push("/cars")}
            className="mt-6 btn btn-primary"
          >
            차량 목록 보기
          </button>
        </div>
      )}

      {!loading && cars.length > 0 && (
        <section className="card overflow-hidden">
          {/* desktop table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-bg-inset border-b border-line">
                <tr>
                  <th className="px-5 py-4 text-left text-xs font-medium text-fg-subtle uppercase tracking-wider w-32">
                    항목
                  </th>
                  {cars.map((c) => (
                    <th key={c.id} className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-14 w-20 bg-bg rounded overflow-hidden flex-shrink-0">
                          {c.imageUrl && (
                            <img
                              src={c.imageUrl}
                              alt={c.model}
                              className="h-full w-full object-cover"
                            />
                          )}
                        </div>
                        <div className="min-w-0 flex-1 text-left">
                          <p className="text-sm font-semibold text-fg truncate">
                            {c.model}
                          </p>
                          <button
                            onClick={() => remove(c.id)}
                            className="mt-1 text-xs text-danger hover:underline"
                          >
                            제거
                          </button>
                        </div>
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {COMPARE_ROWS.filter((r) => r.key !== "model").map((item) => (
                  <tr key={item.key}>
                    <td className="px-5 py-4 text-sm font-medium text-fg-muted">
                      {item.label}
                    </td>
                    {cars.map((c, ci) => {
                      const verdict = getVerdict(cars, item, ci);
                      return (
                        <td
                          key={c.id}
                          className={
                            "px-5 py-4 text-sm " +
                            (verdict === "best"
                              ? "text-success font-semibold"
                              : verdict === "worst"
                              ? "text-fg-muted"
                              : "text-fg")
                          }
                        >
                          <span>{formatCell(c, item)}</span>
                          {verdict === "best" && (
                            <span className="ml-2 text-xs">최저</span>
                          )}
                          {verdict === "worst" && (
                            <span className="ml-2 text-xs text-fg-subtle">최고</span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
                <tr>
                  <td className="px-5 py-4 text-sm font-medium text-fg-muted">상세</td>
                  {cars.map((c) => (
                    <td key={c.id} className="px-5 py-4">
                      <button
                        onClick={() => history.push(`/cars/${c.id}`)}
                        className="btn btn-secondary btn-sm"
                      >
                        상세보기
                      </button>
                    </td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>

          {/* mobile cards */}
          <div className="md:hidden divide-y divide-line">
            {cars.map((c) => (
              <div key={c.id} className="p-5">
                <div className="flex items-start gap-3 mb-4">
                  <div className="h-14 w-20 bg-bg-inset rounded overflow-hidden flex-shrink-0">
                    {c.imageUrl && (
                      <img
                        src={c.imageUrl}
                        alt={c.model}
                        className="h-full w-full object-cover"
                      />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-base font-semibold text-fg truncate">
                      {c.model}
                    </p>
                  </div>
                  <button
                    onClick={() => remove(c.id)}
                    className="text-xs text-danger hover:underline flex-shrink-0"
                  >
                    제거
                  </button>
                </div>
                <dl className="space-y-2 mb-4">
                  {COMPARE_ROWS.filter((r) => r.key !== "model").map((item) => (
                    <div
                      key={item.key}
                      className="flex items-baseline justify-between"
                    >
                      <dt className="text-xs text-fg-muted">{item.label}</dt>
                      <dd className="text-sm font-medium text-fg">
                        {formatCell(c, item)}
                      </dd>
                    </div>
                  ))}
                </dl>
                <button
                  onClick={() => history.push(`/cars/${c.id}`)}
                  className="btn btn-secondary btn-sm w-full"
                >
                  상세보기
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {cars.length > 1 && (
        <p className="mt-4 text-xs text-fg-subtle">
          <span className="text-success font-medium">최저</span> · 가격이나 주행거리가 가장 낮은 값
          <span className="ml-3 text-fg-muted font-medium">최고</span> · 상대적으로 높은 값
        </p>
      )}
    </main>
  );
};

export default CarComparePage;
