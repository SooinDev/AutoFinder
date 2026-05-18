import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchSimilarCars } from "../../api/services";

const ImageIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.25">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <circle cx="9" cy="9" r="1.5" />
    <path d="m21 15-5-5L5 21" />
  </svg>
);

const SimilarCarsCarousel = ({ carId }) => {
  const [cars, setCars] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);

  useEffect(() => {
    let alive = true;
    (async () => {
      if (!carId) return;
      setLoading(true);
      setErr(null);
      try {
        const data = await fetchSimilarCars(carId, 8);
        if (alive) setCars(data.content || []);
      } catch (e) {
        if (alive) setErr(true);
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [carId]);

  if (loading) {
    return (
      <section className="mt-10">
        <h2 className="text-xl font-semibold tracking-tight text-fg mb-5">
          비슷한 차량
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="card overflow-hidden animate-pulse"
            >
              <div className="aspect-[4/3] bg-bg-inset" />
              <div className="p-4 space-y-2">
                <div className="h-4 bg-bg-inset rounded w-3/4" />
                <div className="h-4 bg-bg-inset rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (err || cars.length === 0) return null;

  return (
    <section className="mt-10">
      <h2 className="text-xl font-semibold tracking-tight text-fg mb-5">
        비슷한 차량
      </h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        {cars.slice(0, 8).map((c) => (
          <Link
            key={c.id}
            to={`/cars/${c.id}`}
            className="card card-hover overflow-hidden group"
          >
            <div className="relative aspect-[4/3] bg-bg-inset">
              {c.imageUrl ? (
                <img
                  src={c.imageUrl}
                  alt={c.model}
                  loading="lazy"
                  className="absolute inset-0 h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
                />
              ) : (
                <div className="absolute inset-0 grid place-items-center text-fg-faint">
                  <ImageIcon />
                </div>
              )}
            </div>
            <div className="p-4">
              <h3 className="text-sm font-semibold text-fg line-clamp-1">
                {c.model}
              </h3>
              <p className="mt-1 text-xs text-fg-muted">
                {[c.year, c.fuel].filter(Boolean).join(" · ")}
              </p>
              {c.price != null && (
                <p className="mt-2 text-base font-semibold text-fg">
                  {c.price.toLocaleString()}{" "}
                  <span className="text-xs text-fg-muted font-normal">만원</span>
                </p>
              )}
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
};

export default SimilarCarsCarousel;
