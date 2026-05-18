import React, { useEffect, useState } from "react";
import { Link, NavLink, useHistory, useLocation } from "react-router-dom";

const NAV_PRIMARY = [
  { label: "홈", path: "/" },
  { label: "차량 검색", path: "/cars" },
  { label: "비교", path: "/compare" },
  { label: "시장 분석", path: "/analysis" },
];

const NAV_AUTHED = [
  { label: "즐겨찾기", path: "/favorites" },
  { label: "AI 추천", path: "/ai-recommendations" },
];

const Logo = () => (
  <span className="inline-flex items-center gap-2">
    <span className="grid h-7 w-7 place-items-center rounded-md bg-brand text-bg font-bold text-sm">
      A
    </span>
    <span className="text-base font-semibold tracking-tight text-fg">
      AutoFinder
    </span>
  </span>
);

const Header = ({ userId, username, setUserId, setUsername, setFavorites }) => {
  const history = useHistory();
  const location = useLocation();
  const token = localStorage.getItem("token") || sessionStorage.getItem("token");
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  const handleLogout = () => {
    ["token", "userId", "username"].forEach((k) => {
      localStorage.removeItem(k);
      sessionStorage.removeItem(k);
    });
    if (typeof setFavorites === "function") setFavorites(new Set());
    setUserId(null);
    setUsername(null);
    history.push("/");
  };

  const navItems = token ? [...NAV_PRIMARY, ...NAV_AUTHED] : NAV_PRIMARY;

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-bg/85 backdrop-blur-md">
      <div className="container-page">
        <div className="flex h-16 items-center justify-between">
          {/* left: logo + nav */}
          <div className="flex items-center gap-8">
            <Link to="/">
              <Logo />
            </Link>
            <nav className="hidden md:flex items-center gap-1">
              {navItems.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  exact={item.path === "/"}
                  activeClassName="!text-fg bg-bg-inset"
                  className="px-3 py-1.5 text-sm font-medium text-fg-muted hover:text-fg rounded-md transition-colors"
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>

          {/* right: auth */}
          <div className="flex items-center gap-2">
            {token ? (
              <div className="hidden md:flex items-center gap-3">
                <span className="text-sm text-fg-muted">
                  {username || "사용자"}님
                </span>
                <button onClick={handleLogout} className="btn btn-ghost btn-sm">
                  로그아웃
                </button>
              </div>
            ) : (
              <div className="hidden md:flex items-center gap-2">
                <Link to="/login" className="btn btn-ghost btn-sm">
                  로그인
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm">
                  회원가입
                </Link>
              </div>
            )}

            {/* mobile menu trigger */}
            <button
              type="button"
              onClick={() => setMenuOpen((v) => !v)}
              className="md:hidden inline-flex h-9 w-9 items-center justify-center rounded-md border border-line text-fg-muted hover:text-fg hover:bg-bg-inset"
              aria-label="메뉴"
              aria-expanded={menuOpen}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                {menuOpen ? (
                  <>
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </>
                ) : (
                  <>
                    <line x1="4" y1="7" x2="20" y2="7" />
                    <line x1="4" y1="12" x2="20" y2="12" />
                    <line x1="4" y1="17" x2="20" y2="17" />
                  </>
                )}
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* mobile sheet */}
      {menuOpen && (
        <div className="md:hidden border-t border-line bg-bg-raised">
          <div className="container-page py-4 space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                exact={item.path === "/"}
                activeClassName="!bg-bg-inset !text-fg"
                className="block px-3 py-2.5 text-sm font-medium text-fg-muted hover:text-fg hover:bg-bg-inset rounded-md"
              >
                {item.label}
              </NavLink>
            ))}
            <div className="pt-3 mt-3 border-t border-line">
              {token ? (
                <div className="flex items-center justify-between px-3">
                  <span className="text-sm text-fg-muted">
                    {username || "사용자"}님
                  </span>
                  <button onClick={handleLogout} className="btn btn-ghost btn-sm">
                    로그아웃
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Link to="/login" className="btn btn-secondary btn-sm">
                    로그인
                  </Link>
                  <Link to="/register" className="btn btn-primary btn-sm">
                    회원가입
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
};

export default Header;
