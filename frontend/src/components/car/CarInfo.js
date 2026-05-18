import React from "react";
import { formatNumber } from "../../utils/formatters";

const ExtLink = () => (
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
    <path d="M14 4h6v6" />
    <path d="M10 14 21 3" />
    <path d="M20 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h5" />
  </svg>
);

const ImageIcon = () => (
  <svg
    width="40"
    height="40"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.25"
  >
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <circle cx="9" cy="9" r="1.5" />
    <path d="m21 15-5-5L5 21" />
  </svg>
);

const formatMileageText = (m) => {
  if (m == null || m === "정보 없음") return "—";
  const n = typeof m === "number" ? m : parseInt(m, 10);
  if (isNaN(n)) return "—";
  return `${formatNumber(n)} km`;
};

const CarInfo = ({ car }) => {
  // 실제 car 객체에 있는 필드만 표시. 없으면 행을 그리지 않음.
  const specs = [
    ["연식", car.year],
    ["주행거리", formatMileageText(car.mileage)],
    ["연료", car.fuel],
    ["지역", car.region],
    ["차종", car.carType],
  ].filter(([, v]) => v && v !== "—" && v !== "정보 없음");

  return (
    <article className="card overflow-hidden">
      {/* image */}
      <div className="relative aspect-[16/9] bg-bg-inset">
        {car.imageUrl ? (
          <img
            src={car.imageUrl}
            alt={car.model}
            className="absolute inset-0 h-full w-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 grid place-items-center text-fg-faint">
            <ImageIcon />
          </div>
        )}
      </div>

      {/* header */}
      <div className="p-6 sm:p-8 border-b border-line">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap gap-1.5 mb-3">
              {car.year && <span className="badge">{car.year}</span>}
              {car.fuel && <span className="badge">{car.fuel}</span>}
              {car.region && <span className="badge">{car.region}</span>}
              {car.carType && <span className="badge">{car.carType}</span>}
            </div>
            <h1 className="text-2xl sm:text-3xl font-semibold tracking-tight text-fg leading-snug">
              {car.model}
            </h1>
          </div>

          <div className="text-right">
            {car.price != null ? (
              <>
                <div className="text-3xl sm:text-4xl font-semibold tracking-tight text-fg">
                  {car.price.toLocaleString()}
                </div>
                <div className="mt-1 text-sm text-fg-muted">만원</div>
              </>
            ) : (
              <span className="text-sm text-fg-faint">가격 정보 없음</span>
            )}
          </div>
        </div>
      </div>

      {/* specs */}
      <dl className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 divide-x divide-y sm:divide-y-0 divide-line border-b border-line">
        {specs.map(([k, v]) => (
          <div key={k} className="p-5">
            <dt className="text-xs text-fg-subtle uppercase tracking-wider">
              {k}
            </dt>
            <dd className="mt-1.5 text-sm font-medium text-fg">{v}</dd>
          </div>
        ))}
      </dl>

      {/* action */}
      <div className="p-6 sm:p-8 flex flex-wrap items-center justify-between gap-4">
        <p className="text-sm text-fg-muted">
          원본 매물 페이지에서 상세 정보를 확인하실 수 있습니다.
        </p>
        {car.url && (
          <a
            href={car.url}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-primary"
          >
            원본 매물 보기
            <ExtLink />
          </a>
        )}
      </div>
    </article>
  );
};

export default CarInfo;
