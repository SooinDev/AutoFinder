import React from "react";

const Pagination = ({ currentPage, totalPages, onPageChange }) => {
  if (totalPages <= 1) return null;

  const cur = currentPage + 1;
  const last = totalPages;
  const hasPrev = currentPage > 0;
  const hasNext = currentPage < totalPages - 1;

  const slots = [];
  slots.push(1);
  const window = [cur - 1, cur, cur + 1].filter((v) => v > 1 && v < last);
  if (window[0] > 2) slots.push("…");
  window.forEach((v) => slots.push(v));
  if (window.length && window[window.length - 1] < last - 1) slots.push("…");
  if (last > 1) slots.push(last);

  return (
    <nav
      aria-label="페이지네이션"
      className="mt-10 flex items-center justify-between gap-4"
    >
      <p className="text-sm text-fg-muted">
        <span className="text-fg font-medium">{cur}</span> / {last} 페이지
      </p>

      <div className="flex items-center gap-1">
        <button
          onClick={() => hasPrev && onPageChange(currentPage - 1)}
          disabled={!hasPrev}
          className="btn btn-secondary btn-sm"
        >
          이전
        </button>

        <ul className="hidden sm:flex items-center gap-1 mx-1">
          {slots.map((slot, i) =>
            slot === "…" ? (
              <li key={`e-${i}`} className="px-2 text-fg-faint text-sm">
                …
              </li>
            ) : (
              <li key={slot}>
                <button
                  onClick={() => onPageChange(slot - 1)}
                  aria-current={slot === cur ? "page" : undefined}
                  className={
                    "min-w-[2.25rem] h-9 px-2 text-sm font-medium rounded-md transition-colors " +
                    (slot === cur
                      ? "bg-brand text-bg"
                      : "text-fg-muted hover:text-fg hover:bg-bg-inset")
                  }
                >
                  {slot}
                </button>
              </li>
            )
          )}
        </ul>

        <button
          onClick={() => hasNext && onPageChange(currentPage + 1)}
          disabled={!hasNext}
          className="btn btn-secondary btn-sm"
        >
          다음
        </button>
      </div>
    </nav>
  );
};

export default Pagination;
