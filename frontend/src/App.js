// src/App.js (업데이트된 버전)
import React, { useState, useEffect } from "react";
import { Switch, Route } from "react-router-dom";
import Header from "./components/common/Header";
import Footer from "./components/common/Footer";
import HomePage from "./pages/HomePage";
import CarListPage from "./pages/CarListPage";
import CarDetailPage from "./pages/CarDetailPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NotFoundPage from "./pages/NotFoundPage";
import ModelAnalysisPage from "./pages/ModelAnalysisPage";
import FavoritesPage from "./pages/FavoritesPage";
import AIRecommendationsPage from "./pages/AIRecommendationsPage";
import CarComparePage from "./pages/CarComparePage"; // 차량 비교 페이지 import
import { ThemeProvider } from "./context/ThemeContext";
import { toggleFavorite } from "./api/services";
import "./styles/global.css";

function App() {
    const [userId, setUserId] = useState(null);
    const [username, setUsername] = useState(null);
    const [favorites, setFavorites] = useState(new Set());

    useEffect(() => {
        const storedUserId = localStorage.getItem("userId") || sessionStorage.getItem("userId");
        const storedUsername = localStorage.getItem("username") || sessionStorage.getItem("username");

        if (storedUserId && storedUserId !== "null" && storedUserId !== "undefined") {
            setUserId(storedUserId);
        } else {
            setUserId(null);
        }

        if (storedUsername && storedUsername !== "null" && storedUsername !== "undefined") {
            setUsername(storedUsername);
        } else {
            setUsername(null);
        }
    }, []);

    // 즐겨찾기 토글 함수
    const handleToggleFavorite = async (carId) => {
        if (!userId) {
            alert("로그인이 필요합니다.");
            return;
        }

        try {
            await toggleFavorite(carId, userId, favorites.has(carId));

            // 즐겨찾기 상태 업데이트
            setFavorites(prev => {
                const newFavorites = new Set(prev);
                if (newFavorites.has(carId)) {
                    newFavorites.delete(carId);
                } else {
                    newFavorites.add(carId);
                }
                return newFavorites;
            });
        } catch (err) {
            console.error("즐겨찾기 업데이트 실패:", err);
            alert("즐겨찾기 처리 중 오류가 발생했습니다.");
        }
    };

    return (
        <ThemeProvider>
            <div className="flex flex-col min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
                <Header
                    userId={userId}
                    username={username}
                    setUserId={setUserId}
                    setUsername={setUsername}
                    setFavorites={setFavorites}
                />
                <main className="flex-grow">
                    <Switch>
                        <Route exact path="/">
                            <HomePage
                                userId={userId}
                                username={username}
                                favorites={favorites}
                                setFavorites={setFavorites}
                                onToggleFavorite={handleToggleFavorite}
                            />
                        </Route>
                        <Route exact path="/cars">
                            <CarListPage
                                userId={userId}
                                favorites={favorites}
                                setFavorites={setFavorites}
                            />
                        </Route>
                        <Route path="/cars/:id">
                            <CarDetailPage
                                userId={userId}
                                favorites={favorites}
                                setFavorites={setFavorites}
                            />
                        </Route>
                        <Route path="/login">
                            <LoginPage setUserId={setUserId} setUsername={setUsername}/>
                        </Route>
                        <Route path="/register">
                            <RegisterPage/>
                        </Route>
                        <Route path="/analysis/:model?">
                            <ModelAnalysisPage />
                        </Route>
                        <Route path="/favorites">
                            <FavoritesPage
                                userId={userId}
                                favorites={favorites}
                                setFavorites={setFavorites}
                            />
                        </Route>
                        {/* AI 추천 페이지 라우트 */}
                        <Route path="/ai-recommendations">
                            <AIRecommendationsPage
                                userId={userId}
                                favorites={favorites}
                                setFavorites={setFavorites}
                                onToggleFavorite={handleToggleFavorite}
                            />
                        </Route>
                        {/* 차량 비교 페이지 라우트 - EXACT 속성 제거 */}
                        <Route path="/compare">
                            <CarComparePage
                                userId={userId}
                                favorites={favorites}
                                onToggleFavorite={handleToggleFavorite}
                            />
                        </Route>
                        {/* 404 페이지는 가장 마지막에 */}
                        <Route path="*">
                            <NotFoundPage/>
                        </Route>
                    </Switch>
                </main>
                <Footer/>
            </div>
        </ThemeProvider>
    );
}

export default App;