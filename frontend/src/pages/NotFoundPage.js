import React from "react";
import { Link } from "react-router-dom";

const NotFoundPage = () => {
  return (
    <main className="container-page py-24 sm:py-32 text-center">
      <p className="text-sm font-medium text-brand">404</p>
      <h1 className="mt-3 text-4xl sm:text-5xl font-semibold tracking-tight text-fg">
        페이지를 찾을 수 없습니다
      </h1>
      <p className="mt-4 text-base text-fg-muted max-w-md mx-auto">
        주소가 잘못되었거나 페이지가 이동/삭제되었을 수 있습니다.
      </p>
      <div className="mt-8 flex justify-center gap-2">
        <Link to="/" className="btn btn-primary">
          홈으로 돌아가기
        </Link>
        <Link to="/cars" className="btn btn-secondary">
          차량 검색
        </Link>
      </div>
    </main>
  );
};

export default NotFoundPage;
