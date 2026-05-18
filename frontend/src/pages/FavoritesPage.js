import React, { useCallback, useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { fetchFavorites, toggleFavorite } from "../api/services";
import CarCard from "../components/car/CarCard";
import Pagination from "../components/common/Pagination";

const PAGE_SIZE = 21;

const FavoritesPage = ({ userId, setFavorites }) => {
  const history = useHistory();
  const [cars, setCars] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchFavorites(userId);
      setCars(data || []);
      setTotal(Math.ceil((data?.length || 0) / PAGE_SIZE) || 1);
    } catch (err) {
      console.error(err);
      setError("즐겨찾기 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (!userId) {
      history.push("/login");
      return;
    }
    load();
  }, [userId, history, load]);

  const slice = cars.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  const handleRemove = async (carId) => {
    try {
      await toggleFavorite(carId, userId, true);
      setFavorites?.((prev) => {
        const next = new Set(prev);
        next.delete(carId);
        return next;
      });
      const nextList = cars.filter((c) => c.id !== carId);
      setCars(nextList);
      setTotal(Math.ceil(nextList.length / PAGE_SIZE) || 1);
      if (nextList.slice(page * PAGE_SIZE).length === 0 && page > 0) {
        setPage(page - 1);
      }
    } catch (err) {
      console.error(err);
      alert("즐겨찾기 삭제에 실패했습니다.");
    }
  };

  return (
    <main className="container-page py-10 sm:py-14">
      <header className="mb-8">
        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
          즐겨찾기
        </h1>
        <p className="mt-2 text-sm text-fg-muted">
          저장한 차량은 {cars.length}대입니다.
        </p>
      </header>

      {error && (
        <div className="mb-6 card p-4 border-danger/30 bg-danger/5">
          <p className="text-sm text-danger">{error}</p>
        </div>
      )}

      {loading ? (
        <div className="py-16 flex flex-col items-center gap-3">
          <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-fg-muted">불러오는 중…</p>
        </div>
      ) : slice.length > 0 ? (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {slice.map((car) => (
              <CarCard
                key={car.id}
                car={car}
                isFavorite
                onToggleFavorite={handleRemove}
              />
            ))}
          </div>
          <Pagination
            currentPage={page}
            totalPages={total}
            onPageChange={(p) => {
              setPage(p);
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
          />
        </>
      ) : (
        <div className="card p-12 text-center">
          <p className="text-lg font-semibold text-fg">
            즐겨찾기한 차량이 없습니다
          </p>
          <p className="mt-2 text-sm text-fg-muted">
            관심 있는 차량을 즐겨찾기에 추가해 보세요. AI 추천도 더 정확해집니다.
          </p>
          <button
            onClick={() => history.push("/cars")}
            className="mt-6 btn btn-primary"
          >
            차량 검색하기
          </button>
        </div>
      )}
    </main>
  );
};

export default FavoritesPage;
