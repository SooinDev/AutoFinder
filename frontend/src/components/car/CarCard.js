import React from "react";
import { Link, useHistory } from "react-router-dom";

const HeartIcon = ({ filled }) => (
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

const CompareIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.75"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <line x1="7" y1="20" x2="7" y2="10" />
    <line x1="12" y1="20" x2="12" y2="4" />
    <line x1="17" y1="20" x2="17" y2="14" />
  </svg>
);

const ImageIcon = () => (
  <svg
    width="32"
    height="32"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.25"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <circle cx="9" cy="9" r="1.5" />
    <path d="m21 15-5-5L5 21" />
  </svg>
);

const formatMileage = (m) => {
  if (m == null || m === "정보 없음") return null;
  const n = parseInt(m, 10);
  if (isNaN(n)) return null;
  return `${n.toLocaleString()} km`;
};

const CarCard = ({ car, isFavorite, onToggleFavorite, showCompareButton = true }) => {
  const history = useHistory();

  const handleCompare = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const url = new URL(window.location.href);
    const existing = url.searchParams.get("cars");
    const ids = existing ? existing.split(",") : [];
    if (ids.includes(car.id.toString())) {
      alert("이미 비교 목록에 있는 차량입니다.");
      return;
    }
    if (ids.length >= 3) {
      alert("최대 3대까지 비교할 수 있습니다.");
      return;
    }
    ids.push(car.id.toString());
    history.push(`/compare?cars=${ids.join(",")}`);
  };

  const handleFavorite = (e) => {
    e.preventDefault();
    e.stopPropagation();
    onToggleFavorite(car.id);
  };

  const mileage = formatMileage(car.mileage);

  return (
    <Link
      to={`/cars/${car.id}`}
      className="group card card-hover overflow-hidden flex flex-col"
    >
      {/* image */}
      <div className="relative aspect-[4/3] bg-bg-inset overflow-hidden">
        {car.imageUrl ? (
          <img
            src={car.imageUrl}
            alt={car.model}
            loading="lazy"
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : (
          <div className="absolute inset-0 grid place-items-center text-fg-faint">
            <ImageIcon />
          </div>
        )}

        {/* favorite button */}
        <button
          onClick={handleFavorite}
          aria-label={isFavorite ? "즐겨찾기 해제" : "즐겨찾기 추가"}
          className={
            "absolute top-3 right-3 grid h-9 w-9 place-items-center rounded-full backdrop-blur transition-colors " +
            (isFavorite
              ? "bg-danger text-white"
              : "bg-bg/70 text-fg hover:bg-bg")
          }
        >
          <HeartIcon filled={isFavorite} />
        </button>
      </div>

      {/* body */}
      <div className="p-4 flex flex-col flex-1">
        <h3 className="text-base font-semibold text-fg leading-snug line-clamp-2">
          {car.model}
        </h3>

        <div className="mt-2 flex flex-wrap gap-1.5">
          {car.year && <span className="badge">{car.year}</span>}
          {car.fuel && <span className="badge">{car.fuel}</span>}
          {car.region && <span className="badge">{car.region}</span>}
        </div>

        {mileage && (
          <p className="mt-3 text-sm text-fg-muted">{mileage}</p>
        )}

        <div className="mt-auto pt-4 flex items-end justify-between gap-3">
          <div>
            {car.price != null ? (
              <>
                <span className="text-xl font-semibold text-fg">
                  {car.price.toLocaleString()}
                </span>
                <span className="ml-1 text-sm text-fg-muted">만원</span>
              </>
            ) : (
              <span className="text-sm text-fg-faint">가격 정보 없음</span>
            )}
          </div>

          {showCompareButton && (
            <button
              onClick={handleCompare}
              className="inline-flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-fg-muted hover:text-fg hover:bg-bg-inset rounded-md transition-colors"
              title="비교 목록에 추가"
            >
              <CompareIcon />
              비교
            </button>
          )}
        </div>
      </div>
    </Link>
  );
};

export default CarCard;
