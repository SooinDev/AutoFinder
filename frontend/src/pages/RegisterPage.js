import React, { useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { register } from "../api/services";

const RegisterPage = () => {
  const [form, setForm] = useState({ username: "", password: "", confirmPassword: "" });
  const [errs, setErrs] = useState({});
  const [msg, setMsg] = useState(null);
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(false);
  const history = useHistory();

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
    if (errs[name]) setErrs((p) => ({ ...p, [name]: null }));
  };

  const validate = () => {
    const n = {};
    if (!form.username) n.username = "아이디를 입력해 주세요.";
    else if (form.username.length < 3) n.username = "아이디는 3자 이상이어야 합니다.";
    if (!form.password) n.password = "비밀번호를 입력해 주세요.";
    else if (form.password.length < 6) n.password = "비밀번호는 6자 이상이어야 합니다.";
    if (form.password !== form.confirmPassword)
      n.confirmPassword = "비밀번호가 일치하지 않습니다.";
    setErrs(n);
    return Object.keys(n).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    setMsg(null);
    try {
      await register({ username: form.username, password: form.password });
      setOk(true);
      setMsg("회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.");
      setTimeout(() => history.push("/login"), 1400);
    } catch (err) {
      setOk(false);
      setMsg(err.response?.data || "회원가입에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container-page py-16 sm:py-24">
      <div className="mx-auto max-w-md">
        <header className="text-center mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-fg">
            회원가입
          </h1>
          <p className="mt-2 text-sm text-fg-muted">
            이미 계정이 있으신가요?{" "}
            <Link to="/login" className="link font-medium">
              로그인
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
                disabled={loading}
                required
                className="input"
                placeholder="3자 이상"
                autoComplete="username"
              />
              {errs.username && (
                <p className="mt-1.5 text-xs text-danger">{errs.username}</p>
              )}
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
                disabled={loading}
                required
                className="input"
                placeholder="6자 이상"
                autoComplete="new-password"
              />
              {errs.password && (
                <p className="mt-1.5 text-xs text-danger">{errs.password}</p>
              )}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="input-label">
                비밀번호 확인
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                value={form.confirmPassword}
                onChange={onChange}
                disabled={loading}
                required
                className="input"
                placeholder="비밀번호를 다시 입력"
                autoComplete="new-password"
              />
              {errs.confirmPassword && (
                <p className="mt-1.5 text-xs text-danger">{errs.confirmPassword}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg w-full"
            >
              {loading ? "처리 중…" : "회원가입"}
            </button>
          </form>
        </div>
      </div>
    </main>
  );
};

export default RegisterPage;
