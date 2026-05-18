import React from "react";
import { Link } from "react-router-dom";

const SITEMAP = [
  {
    h: "서비스",
    links: [
      { l: "차량 검색", to: "/cars" },
      { l: "차량 비교", to: "/compare" },
      { l: "시장 분석", to: "/analysis" },
    ],
  },
  {
    h: "계정",
    links: [
      { l: "로그인", to: "/login" },
      { l: "회원가입", to: "/register" },
      { l: "즐겨찾기", to: "/favorites" },
      { l: "AI 추천", to: "/ai-recommendations" },
    ],
  },
];

const Footer = () => {
  const year = new Date().getFullYear();
  return (
    <footer className="mt-24 border-t border-line bg-bg-raised">
      <div className="container-page py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
          {/* brand */}
          <div>
            <Link to="/" className="inline-flex items-center gap-2">
              <span className="grid h-7 w-7 place-items-center rounded-md bg-brand text-bg font-bold text-sm">
                A
              </span>
              <span className="text-base font-semibold tracking-tight text-fg">
                AutoFinder
              </span>
            </Link>
            <p className="mt-4 text-sm text-fg-muted leading-relaxed max-w-sm">
              사용자의 즐겨찾기를 학습하는 AI 기반 중고차 추천 플랫폼입니다.
            </p>
          </div>

          {/* sitemap */}
          {SITEMAP.map((col) => (
            <div key={col.h}>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-fg-subtle mb-4">
                {col.h}
              </h3>
              <ul className="space-y-2.5">
                {col.links.map((l) => (
                  <li key={l.l}>
                    <Link
                      to={l.to}
                      className="text-sm text-fg-muted hover:text-fg transition-colors"
                    >
                      {l.l}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-10 pt-6 border-t border-line">
          <p className="text-xs text-fg-subtle">
            © {year} AutoFinder. 학습용 프로젝트입니다.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
