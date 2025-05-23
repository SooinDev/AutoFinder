package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimilarCarService {
    private final CarRepository carRepository;

    /**
     * 특정 차량과 유사한 차량 목록을 조회합니다.
     *
     * @param carId 기준 차량 ID
     * @param limit 조회할 최대 차량 수
     * @return 유사한 차량 목록
     */
    public Page<Car> findSimilarCars(Long carId, int limit) {
        // 기준 차량 조회
        Car targetCar = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다: " + carId));

        // 유사 차량 검색을 위한 조건 설정
        String model = extractModelBase(targetCar.getModel());
        String exactModel = targetCar.getModel();

        // 가격 범위는 기준 차량 가격의 ±25%로 설정
        double priceRange = 0.25;
        Long minPrice = (long)(targetCar.getPrice() * (1 - priceRange));
        Long maxPrice = (long)(targetCar.getPrice() * (1 + priceRange));

        // 연식 추출 (2자리 연도만 사용)
        String year = extractYearPrefix(targetCar.getYear());

        // 페이징 설정
        Pageable pageable = PageRequest.of(0, limit);

        // 유사 차량 검색 쿼리 실행
        return carRepository.findSimilarCars(
                carId,
                model,
                exactModel,
                minPrice,
                maxPrice,
                year,
                targetCar.getPrice(),
                pageable
        );
    }

    /**
     * 가중치 기반 유사 차량 검색 (고급 버전)
     *
     * @param carId 기준 차량 ID
     * @param limit 최대 검색 개수
     * @return 유사한 차량 목록
     */
    public List<Car> findSimilarCarsWithWeightedScore(Long carId, int limit) {
        // 기준 차량 조회
        Car targetCar = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("차량을 찾을 수 없습니다: " + carId));

        // 필터링 조건 설정
        String modelBase = extractModelBase(targetCar.getModel());

        // 가격 범위 설정 (±30%)
        double priceRange = 0.3;
        Long minPrice = (long)(targetCar.getPrice() * (1 - priceRange));
        Long maxPrice = (long)(targetCar.getPrice() * (1 + priceRange));

        // 기본 필터링된 차량 목록 조회 (30개 정도를 기준으로 유사도 계산)
        Page<Car> filteredCars = carRepository.findSimilarCarsForScoring(
                carId,
                modelBase,
                minPrice,
                maxPrice,
                PageRequest.of(0, 30)
        );

        List<Car> candidateCars = new ArrayList<>(filteredCars.getContent());

        // 유사도 점수 계산 및 정렬
        List<ScoredCar> scoredCars = candidateCars.stream()
                .map(car -> new ScoredCar(car, calculateSimilarityScore(targetCar, car)))
                .sorted(Comparator.comparing(ScoredCar::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Car 객체만 추출하여 반환
        return scoredCars.stream()
                .map(ScoredCar::getCar)
                .collect(Collectors.toList());
    }

    /**
     * 두 차량 간의 유사도 점수 계산
     * 높을수록 더 유사함을 의미 (0-100 범위)
     */
    private double calculateSimilarityScore(Car target, Car other) {
        double score = 0;

        // 1. 모델 유사도 (가중치: 40%)
        double modelScore = calculateModelSimilarity(target.getModel(), other.getModel());
        score += modelScore * 0.4;

        // 2. 가격 유사도 (가중치: 30%)
        double priceScore = calculatePriceSimilarity(target.getPrice(), other.getPrice());
        score += priceScore * 0.3;

        // 3. 연식 유사도 (가중치: 20%)
        double yearScore = calculateYearSimilarity(target.getYear(), other.getYear());
        score += yearScore * 0.2;

        // 4. 주행거리 유사도 (가중치: 10%)
        double mileageScore = calculateMileageSimilarity(target.getMileage(), other.getMileage());
        score += mileageScore * 0.1;

        return score;
    }

    /**
     * 모델명 유사도 계산 (동일 모델: 100, 부분 일치: 60, 다른 모델: 0)
     */
    private double calculateModelSimilarity(String model1, String model2) {
        if (model1 == null || model2 == null) {
            return 0;
        }

        if (model1.equals(model2)) {
            return 100; // 완전 일치
        }

        // 기본 모델명 추출 (예: "현대 아반떼 1.6" -> "아반떼")
        String baseModel1 = extractModelBase(model1);
        String baseModel2 = extractModelBase(model2);

        if (baseModel1.equals(baseModel2)) {
            return 60; // 기본 모델명 일치
        }

        // 브랜드 일치 여부 확인
        String brand1 = extractBrand(model1);
        String brand2 = extractBrand(model2);

        if (!brand1.isEmpty() && brand1.equals(brand2)) {
            return 20; // 브랜드 일치
        }

        return 0;
    }

    /**
     * 가격 유사도 계산 (차이가 적을수록 높은 점수)
     */
    private double calculatePriceSimilarity(Long price1, Long price2) {
        if (price1 == null || price2 == null || price1 == 0) {
            return 0;
        }

        // 가격 차이의 백분율 계산
        double priceDiffPercentage = Math.abs((double) (price1 - price2) / price1) * 100;

        // 가격 차이가 50% 이상이면 0점, 0%면 100점
        return Math.max(0, 100 - (priceDiffPercentage * 2));
    }

    /**
     * 연식 유사도 계산 (차이가 적을수록 높은 점수)
     */
    private double calculateYearSimilarity(String yearStr1, String yearStr2) {
        if (yearStr1 == null || yearStr2 == null) {
            return 0;
        }

        // 연도만 추출 (예: "2018년식" -> 2018)
        int year1 = extractYear(yearStr1);
        int year2 = extractYear(yearStr2);

        if (year1 <= 0 || year2 <= 0) {
            return 0;
        }

        // 연식 차이 계산
        int yearDiff = Math.abs(year1 - year2);

        // 차이가 10년 이상이면 0점, 0년이면 100점
        return Math.max(0, 100 - (yearDiff * 10));
    }

    /**
     * 주행거리 유사도 계산 (차이가 적을수록 높은 점수)
     */
    private double calculateMileageSimilarity(Long mileage1, Long mileage2) {
        if (mileage1 == null || mileage2 == null) {
            return 0;
        }

        // 주행거리 차이의 백분율 계산 (기준: 10만km)
        double mileageDiffPercentage = Math.abs((double) (mileage1 - mileage2) / 100000) * 100;

        // 차이가 100% 이상이면 0점, 0%면 100점
        return Math.max(0, 100 - mileageDiffPercentage);
    }

    /**
     * 차량 모델명에서 기본 모델 추출 (예: "현대 아반떼 1.6" -> "아반떼")
     */
    private String extractModelBase(String fullModel) {
        if (fullModel == null || fullModel.isEmpty()) {
            return "";
        }

        // 주요 모델 키워드 목록
        String[] modelKeywords = {"아반떼", "쏘나타", "그랜저", "K3", "K5", "K7", "K8", "K9",
                "SM3", "SM5", "SM6", "SM7", "말리부", "스파크", "모닝"};

        for (String keyword : modelKeywords) {
            if (fullModel.contains(keyword)) {
                return keyword;
            }
        }

        // 공백으로 분리하여 두 번째 단어 추출 (예: "현대 아반떼" -> "아반떼")
        String[] parts = fullModel.split("\\s+");
        if (parts.length > 1) {
            return parts[1];
        }

        return fullModel;
    }

    /**
     * 차량 모델명에서 브랜드 추출 (예: "현대 아반떼" -> "현대")
     */
    private String extractBrand(String fullModel) {
        if (fullModel == null || fullModel.isEmpty()) {
            return "";
        }

        // 주요 브랜드 키워드
        String[] brandKeywords = {"현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디"};

        for (String brand : brandKeywords) {
            if (fullModel.toLowerCase().contains(brand.toLowerCase())) {
                return brand;
            }
        }

        // 첫 번째 단어를 브랜드로 간주 (예: "현대 아반떼" -> "현대")
        String[] parts = fullModel.split("\\s+");
        if (parts.length > 0) {
            return parts[0];
        }

        return "";
    }

    /**
     * 연식에서 연도 추출 (예: "2018년식" -> 2018)
     */
    private int extractYear(String yearString) {
        if (yearString == null || yearString.isEmpty()) {
            return 0;
        }

        // 숫자만 추출
        String yearDigits = yearString.replaceAll("[^0-9]", "");
        if (yearDigits.isEmpty()) {
            return 0;
        }

        try {
            // 2자리 연도인 경우 (예: "18년식")
            if (yearDigits.length() == 2) {
                int year = Integer.parseInt(yearDigits);
                // 20년대와 19년대 구분
                return year > 50 ? 1900 + year : 2000 + year;
            }
            // 4자리 연도인 경우 (예: "2018년식")
            else if (yearDigits.length() >= 4) {
                return Integer.parseInt(yearDigits.substring(0, 4));
            }
        } catch (NumberFormatException e) {
            return 0;
        }

        return 0;
    }

    /**
     * 연식에서 연도 앞 두 자리 추출 (예: "2018년식" -> "18")
     */
    private String extractYearPrefix(String yearString) {
        if (yearString == null || yearString.isEmpty()) {
            return null;
        }

        // 숫자만 추출
        String yearDigits = yearString.replaceAll("[^0-9]", "");

        // 연도의 앞 두 자리만 추출 (예: "2018" -> "20", "18년식" -> "18")
        if (yearDigits.length() >= 4) {
            return yearDigits.substring(2, 4); // 4자리 연도에서 뒤의 2자리
        } else if (yearDigits.length() >= 2) {
            return yearDigits.substring(0, 2); // 2자리 연도 그대로 사용
        }

        return yearDigits;
    }

    /**
     * 유사도 점수가 있는 차량 내부 클래스
     */
    private static class ScoredCar {
        private final Car car;
        private final double score;

        public ScoredCar(Car car, double score) {
            this.car = car;
            this.score = score;
        }

        public Car getCar() {
            return car;
        }

        public double getScore() {
            return score;
        }
    }
}