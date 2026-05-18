import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchFavorites } from "../../api/services";

const UserDashboard = ({ userId, username }) => {
  const [favorites, setFavorites] = useState([]);
  const [recentSearches, setRecentSearches] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    (async () => {
      setLoading(true);
      try {
        const data = await fetchFavorites(userId);
        setFavorites(data || []);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    })();

    try {
      const s = JSON.parse(localStorage.getItem("recentSearches") || "[]");
      setRecentSearches(s.slice(0, 5));
    } catch {
      setRecentSearches([]);
    }
  }, [userId]);

  return (
    <section className="card overflow-hidden">
      <header className="p-6 border-b border-line">
        <h2 className="text-xl font-semibold tracking-tight text-fg">
          {username || "회원"}님의 대시보드
        </h2>
        <p className="mt-1 text-sm text-fg-muted">
          즐겨찾기와 최근 검색 기록을 확인하세요.
        </p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-line">
        {/* favorites */}
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-fg">
              즐겨찾기한 차량
            </h3>
            <span className="badge">{favorites.length}대</span>
          </div>

          {loading ? (
            <div className="space-y-2.5">
              {Array.from({ length: 3 }).map((_, i) => (
                <div
                  key={i}
                  className="h-14 bg-bg-inset rounded-md animate-pulse"
                />
              ))}
            </div>
          ) : favorites.length > 0 ? (
            <>
              <ul className="space-y-1">
                {favorites.slice(0, 3).map((car) => (
                  <li key={car.id}>
                    <Link
                      to={`/cars/${car.id}`}
                      className="flex items-center gap-3 p-2 -mx-2 rounded-md hover:bg-bg-inset transition-colors group"
                    >
                      <div className="h-12 w-16 bg-bg-inset rounded overflow-hidden flex-shrink-0">
                        {car.imageUrl && (
                          <img
                            src={car.imageUrl}
                            alt={car.model}
                            className="h-full w-full object-cover"
                          />
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-fg truncate group-hover:text-brand transition-colors">
                          {car.model}
                        </p>
                        <p className="mt-0.5 text-xs text-fg-muted">
                          {car.price != null
                            ? `${car.price.toLocaleString()} 만원`
                            : "가격 정보 없음"}
                          {car.year && ` · ${car.year}`}
                        </p>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
              <Link
                to="/favorites"
                className="mt-4 inline-flex link text-sm font-medium"
              >
                전체 보기 →
              </Link>
            </>
          ) : (
            <div className="py-6 text-center">
              <p className="text-sm text-fg-muted">
                아직 즐겨찾기한 차량이 없습니다.
              </p>
              <Link
                to="/cars"
                className="mt-3 inline-flex link text-sm font-medium"
              >
                차량 검색하러 가기 →
              </Link>
            </div>
          )}
        </div>

        {/* recent searches */}
        <div className="p-6">
          <h3 className="text-sm font-semibold text-fg mb-4">최근 검색</h3>

          {recentSearches.length > 0 ? (
            <ul className="space-y-2">
              {recentSearches.map((s, i) => (
                <li key={i}>
                  <Link
                    to={`/cars?${s.params || ""}`}
                    className="block p-3 -mx-2 rounded-md hover:bg-bg-inset transition-colors"
                  >
                    <div className="flex items-baseline justify-between gap-2">
                      <p className="text-sm font-medium text-fg truncate">
                        {s.label || "검색 기록"}
                      </p>
                      {s.date && (
                        <span className="text-xs text-fg-subtle whitespace-nowrap">
                          {s.date}
                        </span>
                      )}
                    </div>
                    {s.filters && (
                      <p className="mt-0.5 text-xs text-fg-muted truncate">
                        {[
                          s.filters.model,
                          s.filters.year && `${s.filters.year}년`,
                          s.filters.minPrice &&
                            s.filters.maxPrice &&
                            `${s.filters.minPrice}~${s.filters.maxPrice}만원`,
                        ]
                          .filter(Boolean)
                          .join(" · ")}
                      </p>
                    )}
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <div className="py-6 text-center">
              <p className="text-sm text-fg-muted">
                최근 검색 기록이 없습니다.
              </p>
              <Link
                to="/cars"
                className="mt-3 inline-flex link text-sm font-medium"
              >
                차량 검색하러 가기 →
              </Link>
            </div>
          )}
        </div>
      </div>
    </section>
  );
};

export default UserDashboard;
