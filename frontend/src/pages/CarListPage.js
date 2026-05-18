import React, { useReducer, useEffect, useCallback } from "react";
import { fetchCars, fetchFavorites, toggleFavorite } from "../api/services";
import CarFilters from "../components/car/CarFilters";
import CarCard from "../components/car/CarCard";
import Pagination from "../components/common/Pagination";
import { useHistory, useLocation } from "react-router-dom";

const initialState = {
  cars: [],
  favoriteCars: [],
  totalPages: 1,
  currentPage: 0,
  isLoading: false,
  error: null,
  activeTab: "all",
  filters: {
    model: "",
    minPrice: "",
    maxPrice: "",
    minMileage: "",
    maxMileage: "",
    fuel: "",
    region: "",
    year: "",
  },
  initialLoad: true,
};

function carListReducer(state, action) {
  switch (action.type) {
    case "INITIALIZE_FROM_URL":
      return {
        ...state,
        currentPage: action.page || 0,
        activeTab: action.tab || "all",
        filters: action.filters || state.filters,
        error:
          action.tab === "favorite" && !action.userId
            ? "즐겨찾기를 보려면 로그인이 필요합니다."
            : null,
      };
    case "SET_CARS":
      return {
        ...state,
        cars: action.payload.content || [],
        totalPages: action.payload.totalPages || 1,
        isLoading: false,
        initialLoad: false,
      };
    case "LOAD_FAVORITES":
      return { ...state, favoriteCars: action.payload || [] };
    case "SET_PAGE":
      return { ...state, currentPage: action.payload };
    case "SET_FILTERS":
      return { ...state, filters: action.payload, currentPage: 0 };
    case "RESET_FILTERS":
      return {
        ...state,
        filters: { ...initialState.filters },
        currentPage: 0,
      };
    case "SET_TAB":
      return {
        ...state,
        activeTab: action.payload,
        currentPage: 0,
        error:
          action.payload === "favorite" && !action.userId
            ? "즐겨찾기를 보려면 로그인이 필요합니다."
            : null,
      };
    case "LOADING_START":
      return { ...state, isLoading: true, error: null };
    case "LOADING_ERROR":
      return { ...state, isLoading: false, error: action.payload };
    default:
      return state;
  }
}

const Spinner = ({ label = "불러오는 중…" }) => (
  <div className="py-16 flex flex-col items-center gap-3">
    <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
    <p className="text-sm text-fg-muted">{label}</p>
  </div>
);

const EmptyState = ({ title, hint, action }) => (
  <div className="card p-12 text-center">
    <p className="text-lg font-semibold text-fg">{title}</p>
    {hint && <p className="mt-2 text-sm text-fg-muted">{hint}</p>}
    {action && <div className="mt-6">{action}</div>}
  </div>
);

const CarListPage = ({ userId, favorites, setFavorites, isHomePage }) => {
  const history = useHistory();
  const location = useLocation();
  const [state, dispatch] = useReducer(carListReducer, initialState);

  const {
    cars,
    favoriteCars,
    totalPages,
    currentPage,
    isLoading,
    error,
    activeTab,
    filters,
    initialLoad,
  } = state;

  useEffect(() => {
    const q = new URLSearchParams(location.search);
    const page = q.get("page");
    const tab = q.get("tab");
    const parsedFilters = { ...initialState.filters };
    Object.keys(parsedFilters).forEach((k) => {
      const v = q.get(k);
      if (v !== null) parsedFilters[k] = v;
    });
    dispatch({
      type: "INITIALIZE_FROM_URL",
      page: page ? parseInt(page, 10) - 1 : 0,
      tab: tab === "favorite" ? "favorite" : "all",
      filters: parsedFilters,
      userId,
    });
  }, [location.search, userId]);

  const updateUrl = useCallback(() => {
    const params = new URLSearchParams();
    params.set("page", currentPage + 1);
    if (activeTab === "favorite") params.set("tab", "favorite");
    Object.entries(filters).forEach(([k, v]) => {
      if (v) params.set(k, v);
    });
    history.replace(`${location.pathname}?${params.toString()}`);
  }, [history, location.pathname, currentPage, activeTab, filters]);

  const loadCars = useCallback(async () => {
    if (isHomePage || activeTab === "favorite") return;
    dispatch({ type: "LOADING_START" });
    try {
      const data = await fetchCars(filters, currentPage, 21);
      dispatch({ type: "SET_CARS", payload: data });
      if (currentPage >= data.totalPages && data.totalPages > 0) {
        dispatch({ type: "SET_PAGE", payload: 0 });
      }
    } catch (err) {
      console.error(err);
      dispatch({
        type: "LOADING_ERROR",
        payload: "차량 목록을 불러오지 못했습니다.",
      });
    }
  }, [isHomePage, activeTab, filters, currentPage]);

  const loadHomeFeed = useCallback(async () => {
    dispatch({ type: "LOADING_START" });
    try {
      const data = await fetchCars({}, 0, 6);
      dispatch({ type: "SET_CARS", payload: data });
    } catch (err) {
      console.error(err);
      dispatch({
        type: "LOADING_ERROR",
        payload: "차량 목록을 불러오지 못했습니다.",
      });
    }
  }, []);

  const loadFavorites = useCallback(async () => {
    if (!userId) {
      setFavorites(new Set());
      dispatch({ type: "LOAD_FAVORITES", payload: [] });
      return;
    }
    try {
      const data = await fetchFavorites(userId);
      setFavorites(new Set(data.map((c) => c.id)));
      dispatch({ type: "LOAD_FAVORITES", payload: data || [] });
    } catch (err) {
      console.error(err);
    }
  }, [userId, setFavorites]);

  useEffect(() => {
    if (!initialLoad && !isHomePage) updateUrl();
  }, [currentPage, activeTab, filters, initialLoad, updateUrl, isHomePage]);

  useEffect(() => {
    if (isHomePage) {
      loadHomeFeed();
      return;
    }
    if (!initialLoad && activeTab === "all") loadCars();
  }, [isHomePage, initialLoad, activeTab, currentPage, filters, loadCars, loadHomeFeed]);

  useEffect(() => {
    if (initialLoad && !isHomePage) loadCars();
  }, [initialLoad, isHomePage, loadCars]);

  useEffect(() => {
    loadFavorites();
  }, [userId, loadFavorites]);

  const handlePageChange = (p) => {
    dispatch({ type: "SET_PAGE", payload: p });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleTabChange = (tab) => {
    if (tab === "favorite" && !userId) {
      alert("로그인이 필요합니다.");
      return;
    }
    dispatch({ type: "SET_TAB", payload: tab, userId });
  };

  const handleSearch = () => loadCars();
  const handleFiltersChange = (f) => dispatch({ type: "SET_FILTERS", payload: f });
  const handleResetFilters = () => {
    dispatch({ type: "RESET_FILTERS" });
    loadCars();
  };

  const handleToggleFavorite = async (carId) => {
    if (!userId) return alert("로그인이 필요합니다.");
    try {
      await toggleFavorite(carId, userId, favorites.has(carId));
      setFavorites((prev) => {
        const next = new Set(prev);
        if (next.has(carId)) next.delete(carId);
        else next.add(carId);
        return next;
      });
      loadFavorites();
    } catch (err) {
      console.error(err);
      alert("즐겨찾기 처리 중 오류가 발생했습니다.");
    }
  };

  const getFavoritePageData = () => {
    const pageSize = 21;
    const start = currentPage * pageSize;
    return {
      items: favoriteCars.slice(start, start + pageSize),
      totalPages: Math.ceil(favoriteCars.length / pageSize) || 1,
    };
  };

  // ---------------- Home inline ----------------
  if (isHomePage) {
    if (isLoading) return <Spinner />;
    if (cars.length === 0)
      return (
        <EmptyState
          title="등록된 차량이 없습니다"
          hint="크롤러를 실행해 매물 데이터를 채워 보세요."
        />
      );
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {cars.slice(0, 6).map((car) => (
          <CarCard
            key={car.id}
            car={car}
            isFavorite={favorites.has(car.id)}
            onToggleFavorite={handleToggleFavorite}
          />
        ))}
      </div>
    );
  }

  // ---------------- Full page ----------------
  let displayCars = cars;
  let displayTotalPages = totalPages;
  if (activeTab === "favorite") {
    const fp = getFavoritePageData();
    displayCars = fp.items;
    displayTotalPages = fp.totalPages;
  }

  return (
    <main className="container-page py-10 sm:py-14">
      <header className="mb-8">
        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
          {activeTab === "favorite" ? "즐겨찾기" : "차량 검색"}
        </h1>
        <p className="mt-2 text-sm text-fg-muted">
          {activeTab === "favorite"
            ? "관심 등록한 차량들을 모아 봤습니다."
            : "조건을 입력해 원하는 차량을 찾아 보세요."}
        </p>
      </header>

      <CarFilters
        filters={filters}
        setFilters={handleFiltersChange}
        onSearch={handleSearch}
        onReset={handleResetFilters}
      />

      {/* tabs */}
      <div className="mb-6 flex items-center justify-between border-b border-line">
        <nav className="flex items-center gap-1 -mb-px">
          {[
            { key: "all", label: "전체 차량" },
            { key: "favorite", label: "즐겨찾기" },
          ].map((t) => (
            <button
              key={t.key}
              onClick={() => handleTabChange(t.key)}
              className={
                "px-4 py-2.5 text-sm font-medium border-b-2 transition-colors " +
                (activeTab === t.key
                  ? "border-brand text-fg"
                  : "border-transparent text-fg-muted hover:text-fg")
              }
            >
              {t.label}
            </button>
          ))}
        </nav>
        <p className="text-sm text-fg-subtle">
          {displayCars.length}대 표시 중
        </p>
      </div>

      {error && (
        <div className="mb-6 card p-4 border-danger/30 bg-danger/5">
          <p className="text-sm text-danger">{error}</p>
        </div>
      )}

      {isLoading ? (
        <Spinner />
      ) : displayCars.length > 0 ? (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {displayCars.map((car) => (
              <CarCard
                key={car.id}
                car={car}
                isFavorite={favorites.has(car.id)}
                onToggleFavorite={handleToggleFavorite}
              />
            ))}
          </div>

          <Pagination
            currentPage={currentPage}
            totalPages={displayTotalPages}
            onPageChange={handlePageChange}
          />
        </>
      ) : (
        <EmptyState
          title={
            activeTab === "favorite"
              ? "즐겨찾기한 차량이 없습니다"
              : "검색 결과가 없습니다"
          }
          hint={
            activeTab === "favorite"
              ? "마음에 드는 차량을 즐겨찾기에 추가해 보세요."
              : "다른 조건으로 다시 검색해 보세요."
          }
          action={
            activeTab === "favorite" ? (
              <button
                onClick={() => handleTabChange("all")}
                className="btn btn-primary"
              >
                차량 둘러보기
              </button>
            ) : (
              <button onClick={handleResetFilters} className="btn btn-secondary">
                필터 초기화
              </button>
            )
          }
        />
      )}
    </main>
  );
};

export default CarListPage;
