import React, { useCallback, useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { fetchAIRecommendations } from "../api/aiServices";
import CarCard from "../components/car/CarCard";
import Pagination from "../components/common/Pagination";

const PAGE_SIZE = 12;

const AIRecommendationsPage = ({ userId, favorites, onToggleFavorite }) => {
  const history = useHistory();
  const [recs, setRecs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAIRecommendations(50);
      const list = data.recommendations || [];
      setRecs(list);
      setTotalPages(Math.max(1, Math.ceil(list.length / PAGE_SIZE)));
    } catch (err) {
      console.error(err);
      setError("AI 추천을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!userId) {
      history.push("/login");
      return;
    }
    load();
  }, [userId, history, load]);

  const slice = recs.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  return (
    <main className="container-page py-10 sm:py-14">
      <header className="mb-8">
        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
          AI 맞춤 추천
        </h1>
        <p className="mt-2 text-sm text-fg-muted">
          즐겨찾기한 차량을 기반으로 비슷한 차량을 추천합니다.
        </p>
      </header>

      {error && (
        <div className="mb-6 card p-4 border-danger/30 bg-danger/5 flex items-center justify-between gap-3">
          <p className="text-sm text-danger">{error}</p>
          <button onClick={load} className="btn btn-secondary btn-sm">
            다시 시도
          </button>
        </div>
      )}

      {loading && (
        <div className="py-16 flex flex-col items-center gap-3">
          <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-fg-muted">추천을 생성하는 중…</p>
        </div>
      )}

      {!loading && !error && slice.length > 0 && (
        <>
          <div className="mb-5 flex items-center justify-between">
            <p className="text-sm text-fg-muted">
              총 <span className="text-fg font-medium">{recs.length}</span>개의 추천
            </p>
            <button
              onClick={load}
              className="btn btn-ghost btn-sm"
            >
              새로고침
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            {slice.map((r) => {
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
                      <span className="badge badge-brand">{match}% 매칭</span>
                    </div>
                  )}
                  {r.recommendationReason && (
                    <p className="mt-2 px-1 text-xs text-fg-muted leading-relaxed line-clamp-2">
                      {r.recommendationReason}
                    </p>
                  )}
                </div>
              );
            })}
          </div>

          {recs.length > PAGE_SIZE && (
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={(p) => {
                setPage(p);
                window.scrollTo({ top: 0, behavior: "smooth" });
              }}
            />
          )}
        </>
      )}

      {!loading && !error && slice.length === 0 && (
        <div className="card p-12 text-center">
          <p className="text-lg font-semibold text-fg">
            아직 추천할 차량이 없습니다
          </p>
          <p className="mt-2 text-sm text-fg-muted max-w-md mx-auto">
            차량을 즐겨찾기에 추가하면 AI가 취향을 학습해 정확한 추천을 제공합니다.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-2">
            <button
              onClick={() => history.push("/cars")}
              className="btn btn-primary"
            >
              차량 둘러보기
            </button>
            <button
              onClick={() => history.push("/favorites")}
              className="btn btn-secondary"
            >
              내 즐겨찾기
            </button>
          </div>
        </div>
      )}
    </main>
  );
};

export default AIRecommendationsPage;
