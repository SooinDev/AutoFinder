import React, { useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { login } from "../api/services";

const LoginPage = ({ setUserId, setUsername }) => {
  const [form, setForm] = useState({ username: "", password: "", rememberMe: false });
  const [msg, setMsg] = useState(null);
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(false);
  const history = useHistory();

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMsg(null);
    try {
      const data = await login(form);
      const { token, userId } = data;
      const store = form.rememberMe ? localStorage : sessionStorage;
      store.setItem("token", token);
      store.setItem("userId", userId);
      store.setItem("username", form.username);
      setUserId(userId);
      setUsername(form.username);
      setOk(true);
      setMsg("로그인 성공! 잠시 후 이동합니다.");
      setTimeout(() => history.push("/"), 800);
    } catch {
      setOk(false);
      setMsg("아이디 또는 비밀번호를 확인해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container-page py-16 sm:py-24">
      <div className="mx-auto max-w-md">
        <header className="text-center mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-fg">
            로그인
          </h1>
          <p className="mt-2 text-sm text-fg-muted">
            계정이 없으신가요?{" "}
            <Link to="/register" className="link font-medium">
              회원가입
            </Link>
          </p>
        </header>

        <div className="card p-8">
          {msg && (
            <div
              className={
                "mb-6 p-3.5 rounded-md text-sm border " +
                (ok
                  ? "bg-success/10 border-success/30 text-success"
                  : "bg-danger/10 border-danger/30 text-danger")
              }
            >
              {msg}
            </div>
          )}

          <form onSubmit={onSubmit} className="space-y-5">
            <div>
              <label htmlFor="username" className="input-label">
                아이디
              </label>
              <input
                id="username"
                name="username"
                type="text"
                value={form.username}
                onChange={onChange}
                required
                disabled={loading}
                className="input"
                placeholder="아이디를 입력하세요"
                autoComplete="username"
              />
            </div>

            <div>
              <label htmlFor="password" className="input-label">
                비밀번호
              </label>
              <input
                id="password"
                name="password"
                type="password"
                value={form.password}
                onChange={onChange}
                required
                disabled={loading}
                className="input"
                placeholder="••••••••"
                autoComplete="current-password"
              />
            </div>

            <label
              htmlFor="rememberMe"
              className="flex items-center gap-2.5 cursor-pointer select-none"
            >
              <input
                id="rememberMe"
                name="rememberMe"
                type="checkbox"
                checked={form.rememberMe}
                onChange={onChange}
                disabled={loading}
                className="h-4 w-4 rounded border-line bg-bg-inset text-brand focus:ring-brand"
              />
              <span className="text-sm text-fg-muted">로그인 상태 유지</span>
            </label>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg w-full"
            >
              {loading ? "로그인 중…" : "로그인"}
            </button>
          </form>
        </div>
      </div>
    </main>
  );
};

export default LoginPage;
