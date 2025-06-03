import React from 'react';
import { Link, useHistory } from 'react-router-dom';

const CarCard = ({ car, isFavorite, onToggleFavorite, showCompareButton = true }) => {
    const history = useHistory();

    const handleCompareClick = (e) => {
        e.preventDefault();
        e.stopPropagation();

        // 현재 URL에서 비교 중인 차량 ID들을 가져오기
        const currentUrl = new URL(window.location.href);
        const existingCars = currentUrl.searchParams.get('cars');
        const carIds = existingCars ? existingCars.split(',') : [];

        // 이미 비교 목록에 있는지 확인
        if (carIds.includes(car.id.toString())) {
            alert('이미 비교 목록에 있는 차량입니다.');
            return;
        }

        // 최대 3대까지만 비교 가능
        if (carIds.length >= 3) {
            alert('최대 3대까지만 비교할 수 있습니다.');
            return;
        }

        // 새로운 차량 추가
        carIds.push(car.id.toString());

        // 비교 페이지로 이동
        history.push(`/compare?cars=${carIds.join(',')}`);
    };

    return (
        <div className="bg-white dark:bg-gray-800 overflow-hidden shadow rounded-lg transition-transform duration-300 hover:-translate-y-1 hover:shadow-lg relative">
            {car.imageUrl ? (
                <div className="w-full h-48 bg-gray-200 dark:bg-gray-700">
                    <img src={car.imageUrl} alt={car.model} className="w-full h-full object-cover" />
                </div>
            ) : (
                <div className="w-full h-48 bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-gray-400 dark:text-gray-500">
                    이미지 없음
                </div>
            )}

            {/* 즐겨찾기 및 비교 버튼 */}
            <div className="absolute top-2 right-2 flex space-x-1">
                <button
                    onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        onToggleFavorite(car.id);
                    }}
                    className="p-2 bg-white dark:bg-gray-700 rounded-full shadow-md hover:shadow-lg transition-shadow"
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-5 w-5"
                        fill={isFavorite ? "currentColor" : "none"}
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth={isFavorite ? "0" : "2"}
                        color={isFavorite ? "#EF4444" : "#6B7280"}
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                </button>

                {showCompareButton && (
                    <button
                        onClick={handleCompareClick}
                        className="p-2 bg-white dark:bg-gray-700 rounded-full shadow-md hover:shadow-lg transition-shadow"
                        title="비교 추가"
                    >
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-5 w-5 text-blue-600 dark:text-blue-400"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            strokeWidth={2}
                        >
                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 00-2 2z" />
                        </svg>
                    </button>
                )}
            </div>

            <div className="p-5">
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">{car.model}</h3>
                <div className="flex flex-wrap gap-2 mb-3">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200">
                        {car.year}년식
                    </span>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200">
                        {car.fuel}
                    </span>
                </div>

                <div className="flex justify-between items-center mb-1">
                    <span className="text-sm text-gray-500 dark:text-gray-400">주행거리:</span>
                    <span className="text-sm text-gray-700 dark:text-gray-300">{car.mileage !== "정보 없음" ? `${parseInt(car.mileage).toLocaleString()} km` : "정보 없음"}</span>
                </div>

                <div className="flex justify-between items-center mb-3">
                    <span className="text-sm text-gray-500 dark:text-gray-400">지역:</span>
                    <span className="text-sm text-gray-700 dark:text-gray-300">{car.region}</span>
                </div>

                <div className="border-t border-gray-200 dark:border-gray-700 pt-3 mt-3">
                    <div className="flex justify-between items-center mb-4">
                        <span className="text-xl font-bold text-teal-600 dark:text-teal-400">{car.price?.toLocaleString() ?? "정보 없음"} 만원</span>
                    </div>
                </div>

                <div className="space-y-2">
                    <Link
                        to={`/cars/${car.id}`}
                        className="block w-full bg-teal-600 text-white py-2 px-4 rounded-md text-sm font-medium text-center hover:bg-teal-700"
                    >
                        상세보기
                    </Link>

                    {showCompareButton && (
                        <button
                            onClick={handleCompareClick}
                            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md text-sm font-medium hover:bg-blue-700"
                        >
                            비교 추가
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CarCard;