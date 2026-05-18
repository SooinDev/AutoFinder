import React, { useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { fetchCarById, toggleFavorite } from "../api/services";
import CarInfo from "../components/car/CarInfo";
import PriceAnalysisChart from "../components/analytics/PriceAnalysisChart";
import SimilarCarsCarousel from "../components/car/SimilarCarsCarousel";

const Heart = ({ filled }) => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill={filled ? "currentColor" : "none"}
    stroke="currentColor"
    strokeWidth="1.75"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
  </svg>
);

const CarDetailPage = ({ userId, favorites, setFavorites }) => {
  const { id } = useParams();
  const history = useHistory();
  const [car, setCar] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isFavorite, setIsFavorite] = useState(false);

  useEffect(() => {
    let alive = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchCarById(id);
        if (!alive) return;
        if (data) {
          setCar(data);
          if (favorites?.has?.(parseInt(id, 10))) setIsFavorite(true);
        } else {
          setError("차량 정보를 찾을 수 없습니다.");
        }
      } catch (err) {
        console.error(err);
        if (alive) setError("차량 정보를 불러오는 중 오류가 발생했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [id, favorites]);

  const handleBack = () => history.goBack();

  const handleToggleFavorite = async () => {
    if (!userId) {
      alert("로그인이 필요합니다.");
      history.push("/login");
      return;
    }
    try {
      await toggleFavorite(id, userId, isFavorite);
      setIsFavorite((v) => !v);
      setFavorites?.((prev) => {
        const next = new Set(prev);
        if (isFavorite) next.delete(parseInt(id, 10));
        else next.add(parseInt(id, 10));
        return next;
      });
    } catch (err) {
      console.error(err);
      alert("즐겨찾기 처리 중 오류가 발생했습니다.");
    }
  };

  return (
    <main className="container-page py-10 sm:py-14">
      <div className="mb-6 flex items-center justify-between">
        <button
          onClick={handleBack}
          className="btn btn-ghost btn-sm -ml-3"
        >
          ← 목록으로
        </button>

        {userId && car && (
          <button
            onClick={handleToggleFavorite}
            className={
              "btn btn-sm " + (isFavorite ? "btn-danger" : "btn-secondary")
            }
          >
            <Heart filled={isFavorite} />
            {isFavorite ? "즐겨찾기 해제" : "즐겨찾기"}
          </button>
        )}
      </div>

      {loading ? (
        <div className="py-24 flex flex-col items-center gap-3">
          <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-fg-muted">차량 정보를 불러오는 중…</p>
        </div>
      ) : error ? (
        <div className="card p-12 text-center border-danger/30">
          <p className="text-lg font-semibold text-danger">{error}</p>
          <button onClick={handleBack} className="mt-6 btn btn-secondary">
            돌아가기
          </button>
        </div>
      ) : !car ? null : (
        <>
          <CarInfo car={car} />

          <section className="mt-10">
            <h2 className="text-xl font-semibold tracking-tight text-fg mb-2">
              {car.model} 시세 분석
            </h2>
            <p className="text-sm text-fg-muted mb-5">
              같은 모델의 연식별 가격 분포입니다.
            </p>
            <PriceAnalysisChart modelName={car.model} />
          </section>

          <SimilarCarsCarousel carId={car.id} />
        </>
      )}
    </main>
  );
};

export default CarDetailPage;
