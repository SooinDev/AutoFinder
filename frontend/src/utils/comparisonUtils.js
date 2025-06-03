// src/utils/comparisonUtils.js

/**
 * 차량 비교 관련 유틸리티 함수들
 */

/**
 * 로컬 스토리지에서 비교 목록 가져오기
 * @returns {Array} 비교 중인 차량 ID 배열
 */
export const getComparisonList = () => {
    try {
        const stored = localStorage.getItem('carComparison');
        return stored ? JSON.parse(stored) : [];
    } catch (error) {
        console.error('비교 목록 로드 실패:', error);
        return [];
    }
};

/**
 * 로컬 스토리지에 비교 목록 저장
 * @param {Array} carIds - 차량 ID 배열
 */
export const saveComparisonList = (carIds) => {
    try {
        localStorage.setItem('carComparison', JSON.stringify(carIds));
    } catch (error) {
        console.error('비교 목록 저장 실패:', error);
    }
};

/**
 * 비교 목록에 차량 추가
 * @param {number} carId - 추가할 차량 ID
 * @param {number} maxItems - 최대 비교 가능 차량 수 (기본값: 3)
 * @returns {boolean} 추가 성공 여부
 */
export const addToComparison = (carId, maxItems = 3) => {
    const currentList = getComparisonList();

    // 이미 목록에 있는지 확인
    if (currentList.includes(carId)) {
        return false;
    }

    // 최대 개수 확인
    if (currentList.length >= maxItems) {
        return false;
    }

    // 추가 및 저장
    const newList = [...currentList, carId];
    saveComparisonList(newList);
    return true;
};

/**
 * 비교 목록에서 차량 제거
 * @param {number} carId - 제거할 차량 ID
 */
export const removeFromComparison = (carId) => {
    const currentList = getComparisonList();
    const newList = currentList.filter(id => id !== carId);
    saveComparisonList(newList);
};

/**
 * 비교 목록 초기화
 */
export const clearComparisonList = () => {
    localStorage.removeItem('carComparison');
};

/**
 * 차량이 비교 목록에 있는지 확인
 * @param {number} carId - 확인할 차량 ID
 * @returns {boolean} 목록에 있는지 여부
 */
export const isInComparison = (carId) => {
    const currentList = getComparisonList();
    return currentList.includes(carId);
};

/**
 * 비교 페이지 URL 생성
 * @param {Array} carIds - 차량 ID 배열
 * @returns {string} 비교 페이지 URL
 */
export const generateComparisonUrl = (carIds) => {
    if (!carIds || carIds.length === 0) {
        return '/compare';
    }
    return `/compare?cars=${carIds.join(',')}`;
};

/**
 * URL에서 비교 차량 ID 추출
 * @param {string} searchParams - URL search parameters
 * @returns {Array} 차량 ID 배열
 */
export const extractCarIdsFromUrl = (searchParams) => {
    const params = new URLSearchParams(searchParams);
    const carIds = params.get('cars');

    if (!carIds) {
        return [];
    }

    return carIds.split(',')
        .map(id => parseInt(id, 10))
        .filter(id => !isNaN(id));
};

/**
 * 비교 데이터에서 최고/최저값 찾기
 * @param {Array} cars - 차량 배열
 * @param {string} field - 비교할 필드명
 * @param {string} type - 비교 타입 ('min' | 'max')
 * @returns {number} 최고/최저값
 */
export const findComparisonExtreme = (cars, field, type = 'min') => {
    const values = cars
        .map(car => car[field])
        .filter(value => value !== null && value !== undefined && value !== '정보 없음')
        .map(value => typeof value === 'number' ? value : parseInt(value))
        .filter(value => !isNaN(value));

    if (values.length === 0) {
        return null;
    }

    return type === 'min' ? Math.min(...values) : Math.max(...values);
};

/**
 * 비교 결과 하이라이트 클래스 생성
 * @param {Array} cars - 차량 배열
 * @param {string} field - 비교 필드
 * @param {number} carIndex - 현재 차량 인덱스
 * @param {string} preferredDirection - 선호 방향 ('lower' | 'higher')
 * @returns {string} CSS 클래스명
 */
export const getComparisonHighlightClass = (cars, field, carIndex, preferredDirection = 'lower') => {
    const currentValue = cars[carIndex][field];

    if (currentValue === null || currentValue === undefined || currentValue === '정보 없음') {
        return '';
    }

    const numValue = typeof currentValue === 'number' ? currentValue : parseInt(currentValue);
    if (isNaN(numValue)) {
        return '';
    }

    const minValue = findComparisonExtreme(cars, field, 'min');
    const maxValue = findComparisonExtreme(cars, field, 'max');

    if (minValue === null || maxValue === null || minValue === maxValue) {
        return '';
    }

    if (preferredDirection === 'lower') {
        if (numValue === minValue) {
            return 'bg-green-50 dark:bg-green-900 dark:bg-opacity-20 text-green-800 dark:text-green-200';
        }
        if (numValue === maxValue) {
            return 'bg-red-50 dark:bg-red-900 dark:bg-opacity-20 text-red-800 dark:text-red-200';
        }
    } else {
        if (numValue === maxValue) {
            return 'bg-green-50 dark:bg-green-900 dark:bg-opacity-20 text-green-800 dark:text-green-200';
        }
        if (numValue === minValue) {
            return 'bg-red-50 dark:bg-red-900 dark:bg-opacity-20 text-red-800 dark:text-red-200';
        }
    }

    return '';
};