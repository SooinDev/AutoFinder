// src/pages/CarComparePage.js
import React, { useState, useEffect } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { fetchCarById, fetchCars } from '../api/services';
import CarCard from '../components/car/CarCard';
import { formatPrice, formatNumber } from '../utils/formatters';

const CarComparePage = ({ userId, favorites, onToggleFavorite }) => {
    const [selectedCars, setSelectedCars] = useState([]);
    const [searchResults, setSearchResults] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isSearching, setIsSearching] = useState(false);
    const [error, setError] = useState(null);
    const history = useHistory();
    const location = useLocation();
    const maxComparisonCars = 3;

    // URL 파라미터에서 차량 ID 가져오기
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const carIds = params.get('cars');

        if (carIds) {
            const idArray = carIds.split(',').map(id => parseInt(id, 10)).filter(id => !isNaN(id));
            loadCarsFromIds(idArray);
        }
    }, [location.search]);

    // ID 배열로부터 차량 정보 로드
    const loadCarsFromIds = async (carIds) => {
        setIsLoading(true);
        setError(null);

        try {
            const carPromises = carIds.slice(0, maxComparisonCars).map(id => fetchCarById(id));
            const cars = await Promise.all(carPromises);
            const validCars = cars.filter(car => car !== null);
            setSelectedCars(validCars);
        } catch (err) {
            console.error("차량 정보 로드 실패:", err);
            setError("차량 정보를 불러오는 중 오류가 발생했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    // 차량 검색
    const handleSearch = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim()) return;

        setIsSearching(true);
        try {
            const data = await fetchCars({ model: searchQuery }, 0, 12);
            setSearchResults(data.content || []);
        } catch (err) {
            console.error("검색 실패:", err);
            setError("검색 중 오류가 발생했습니다.");
        } finally {
            setIsSearching(false);
        }
    };

    // 차량 비교에 추가
    const addToComparison = (car) => {
        if (selectedCars.length >= maxComparisonCars) {
            alert(`최대 ${maxComparisonCars}대까지만 비교할 수 있습니다.`);
            return;
        }

        if (selectedCars.some(c => c.id === car.id)) {
            alert('이미 비교 목록에 있는 차량입니다.');
            return;
        }

        const newSelectedCars = [...selectedCars, car];
        setSelectedCars(newSelectedCars);
        updateUrl(newSelectedCars);
    };

    // 비교 목록에서 제거
    const removeFromComparison = (carId) => {
        const newSelectedCars = selectedCars.filter(car => car.id !== carId);
        setSelectedCars(newSelectedCars);
        updateUrl(newSelectedCars);
    };

    // URL 업데이트
    const updateUrl = (cars) => {
        if (cars.length > 0) {
            const carIds = cars.map(car => car.id).join(',');
            history.replace(`/compare?cars=${carIds}`);
        } else {
            history.replace('/compare');
        }
    };

    // 비교 항목 정의
    const comparisonItems = [
        { key: 'model', label: '모델명', type: 'text' },
        { key: 'price', label: '가격', type: 'price', unit: '만원' },
        { key: 'year', label: '연식', type: 'text' },
        { key: 'mileage', label: '주행거리', type: 'number', unit: 'km' },
        { key: 'fuel', label: '연료타입', type: 'text' },
        { key: 'region', label: '지역', type: 'text' }
    ];

    // 비교 테이블 값 포맷팅
    const formatComparisonValue = (car, item) => {
        const value = car[item.key];

        if (value === null || value === undefined || value === '정보 없음') {
            return '정보 없음';
        }

        switch (item.type) {
            case 'price':
                return typeof value === 'number' ? `${formatPrice(value)} ${item.unit}` : '정보 없음';
            case 'number':
                return typeof value === 'number' || !isNaN(parseInt(value))
                    ? `${formatNumber(value)} ${item.unit}`
                    : '정보 없음';
            default:
                return value.toString();
        }
    };

    // 최고/최저값 하이라이트를 위한 비교 로직
    const getComparisonHighlight = (cars, item, carIndex) => {
        if (item.type !== 'price' && item.type !== 'number') return '';

        const values = cars.map(car => {
            const value = car[item.key];
            if (value === null || value === undefined || value === '정보 없음') return null;
            return typeof value === 'number' ? value : parseInt(value);
        }).filter(v => v !== null && !isNaN(v));

        if (values.length < 2) return '';

        const currentValue = cars[carIndex][item.key];
        if (currentValue === null || currentValue === undefined || currentValue === '정보 없음') return '';

        const numValue = typeof currentValue === 'number' ? currentValue : parseInt(currentValue);
        if (isNaN(numValue)) return '';

        const minValue = Math.min(...values);
        const maxValue = Math.max(...values);

        // 가격이나 주행거리는 낮을수록 좋음
        if (item.key === 'price' || item.key === 'mileage') {
            if (numValue === minValue) return 'bg-green-50 dark:bg-green-900 dark:bg-opacity-20 text-green-800 dark:text-green-200';
            if (numValue === maxValue) return 'bg-red-50 dark:bg-red-900 dark:bg-opacity-20 text-red-800 dark:text-red-200';
        }

        return '';
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white sm:text-3xl mb-2">
                    차량 비교
                </h1>
                <p className="text-gray-500 dark:text-gray-400">
                    최대 {maxComparisonCars}대까지 차량을 선택하여 나란히 비교해보세요.
                </p>
            </div>

            {/* 차량 검색 */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 mb-8 transition-colors duration-300">
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                    비교할 차량 검색
                </h3>
                <form onSubmit={handleSearch} className="mb-4">
                    <div className="flex gap-4">
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="차량 모델명을 입력하세요 (예: 아반떼, 쏘나타)"
                            className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:ring-teal-500 focus:border-teal-500 dark:bg-gray-700 dark:text-white"
                        />
                        <button
                            type="submit"
                            disabled={isSearching}
                            className="px-6 py-2 bg-teal-600 text-white rounded-md hover:bg-teal-700 disabled:opacity-50"
                        >
                            {isSearching ? '검색 중...' : '검색'}
                        </button>
                    </div>
                </form>

                {/* 검색 결과 */}
                {searchResults.length > 0 && (
                    <div>
                        <h4 className="text-md font-medium text-gray-700 dark:text-gray-300 mb-3">
                            검색 결과 ({searchResults.length}건)
                        </h4>
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                            {searchResults.map(car => (
                                <div key={car.id} className="relative">
                                    <CarCard
                                        car={car}
                                        isFavorite={favorites && favorites.has ? favorites.has(car.id) : false}
                                        onToggleFavorite={onToggleFavorite}
                                    />
                                    <button
                                        onClick={() => addToComparison(car)}
                                        disabled={selectedCars.some(c => c.id === car.id) || selectedCars.length >= maxComparisonCars}
                                        className="absolute top-2 right-2 bg-blue-600 text-white text-xs px-2 py-1 rounded-full hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {selectedCars.some(c => c.id === car.id) ? '추가됨' : '비교 추가'}
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* 오류 메시지 */}
            {error && (
                <div className="bg-red-50 dark:bg-red-900 dark:bg-opacity-20 p-4 rounded-md mb-6">
                    <p className="text-red-700 dark:text-red-400">{error}</p>
                </div>
            )}

            {/* 로딩 상태 */}
            {isLoading && (
                <div className="py-12 flex flex-col items-center justify-center">
                    <svg className="animate-spin h-10 w-10 text-teal-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="text-sm text-gray-500 dark:text-gray-400">차량 정보를 불러오는 중...</p>
                </div>
            )}

            {/* 비교 결과 */}
            {!isLoading && (
                <>
                    {selectedCars.length === 0 ? (
                        <div className="text-center py-12 bg-gray-50 dark:bg-gray-800 rounded-lg">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto text-gray-400 dark:text-gray-500 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 00-2 2z" />
                            </svg>
                            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                                비교할 차량을 선택해주세요
                            </h3>
                            <p className="text-gray-500 dark:text-gray-400 mb-4">
                                위의 검색 기능을 이용하여 비교하고 싶은 차량을 찾아보세요.
                            </p>
                            <button
                                onClick={() => history.push('/cars')}
                                className="inline-flex items-center px-4 py-2 bg-teal-600 text-white rounded-md hover:bg-teal-700"
                            >
                                차량 목록 보기
                            </button>
                        </div>
                    ) : (
                        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm overflow-hidden transition-colors duration-300">
                            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                                <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                                    차량 비교 ({selectedCars.length}/{maxComparisonCars})
                                </h3>
                            </div>

                            {/* 모바일 버전 - 카드 형태 */}
                            <div className="block lg:hidden">
                                {selectedCars.map((car, index) => (
                                    <div key={car.id} className="border-b border-gray-200 dark:border-gray-700 last:border-b-0">
                                        <div className="p-4">
                                            <div className="flex justify-between items-start mb-4">
                                                <h4 className="text-lg font-medium text-gray-900 dark:text-white">
                                                    {car.model}
                                                </h4>
                                                <button
                                                    onClick={() => removeFromComparison(car.id)}
                                                    className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                                                >
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                    </svg>
                                                </button>
                                            </div>

                                            <div className="space-y-3">
                                                {comparisonItems.map(item => (
                                                    <div key={item.key} className="flex justify-between">
                                                        <span className="text-sm text-gray-500 dark:text-gray-400">
                                                            {item.label}
                                                        </span>
                                                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                                                            {formatComparisonValue(car, item)}
                                                        </span>
                                                    </div>
                                                ))}
                                                <div className="pt-2">
                                                    <button
                                                        onClick={() => history.push(`/cars/${car.id}`)}
                                                        className="w-full text-center px-4 py-2 bg-teal-600 text-white rounded-md text-sm hover:bg-teal-700"
                                                    >
                                                        상세보기
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* 데스크톱 버전 - 테이블 형태 */}
                            <div className="hidden lg:block overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                                    <thead className="bg-gray-50 dark:bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                            비교 항목
                                        </th>
                                        {selectedCars.map((car, index) => (
                                            <th key={car.id} className="px-6 py-3 text-center text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                                                <div className="flex flex-col items-center">
                                                    <div className="w-24 h-16 bg-gray-200 dark:bg-gray-600 rounded mb-2 overflow-hidden">
                                                        {car.imageUrl ? (
                                                            <img src={car.imageUrl} alt={car.model} className="w-full h-full object-cover" />
                                                        ) : (
                                                            <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">
                                                                이미지 없음
                                                            </div>
                                                        )}
                                                    </div>
                                                    <span className="text-gray-900 dark:text-white font-medium truncate max-w-24">
                                                            차량 {index + 1}
                                                        </span>
                                                    <button
                                                        onClick={() => removeFromComparison(car.id)}
                                                        className="mt-1 text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                                                    >
                                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                        </svg>
                                                    </button>
                                                </div>
                                            </th>
                                        ))}
                                    </tr>
                                    </thead>
                                    <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                                    {comparisonItems.map(item => (
                                        <tr key={item.key}>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                                                {item.label}
                                            </td>
                                            {selectedCars.map((car, carIndex) => (
                                                <td key={car.id} className={`px-6 py-4 whitespace-nowrap text-sm text-center font-medium ${getComparisonHighlight(selectedCars, item, carIndex)}`}>
                                                    {formatComparisonValue(car, item)}
                                                </td>
                                            ))}
                                        </tr>
                                    ))}
                                    <tr>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                                            상세보기
                                        </td>
                                        {selectedCars.map(car => (
                                            <td key={car.id} className="px-6 py-4 whitespace-nowrap text-center">
                                                <button
                                                    onClick={() => history.push(`/cars/${car.id}`)}
                                                    className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-teal-600 hover:bg-teal-700"
                                                >
                                                    보기
                                                </button>
                                            </td>
                                        ))}
                                    </tr>
                                    </tbody>
                                </table>
                            </div>

                            {/* 비교 결과 요약 */}
                            {selectedCars.length > 1 && (
                                <div className="px-6 py-4 bg-gray-50 dark:bg-gray-700">
                                    <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
                                        💡 비교 요약
                                    </h4>
                                    <div className="text-xs text-gray-600 dark:text-gray-400 space-y-1">
                                        <p>• <span className="text-green-600 dark:text-green-400">녹색</span>: 해당 항목에서 가장 좋은 값 (낮은 가격, 적은 주행거리)</p>
                                        <p>• <span className="text-red-600 dark:text-red-400">빨간색</span>: 해당 항목에서 상대적으로 높은 값</p>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default CarComparePage;