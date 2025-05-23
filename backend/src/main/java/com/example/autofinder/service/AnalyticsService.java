package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final CarRepository carRepository;

    /**
     * 특정 모델의 연식별 가격 통계 조회
     * @param model 차량 모델명
     * @return 연식별 최저가, 평균가, 최고가 통계
     */
    public List<Map<String, Object>> getPriceStatsByYear(String model) {
        // 해당 모델의 모든 차량 조회
        List<Car> cars = carRepository.findByModelContaining(model);

        // 연식 표준화 함수 (예: "2301", "2211" → "2023", "2022")
        Function<String, String> standardizeYear = yearStr -> {
            if (yearStr == null || yearStr.isEmpty()) {
                return "미상";
            }

            // 숫자만 추출
            String yearDigits = yearStr.replaceAll("[^0-9]", "");

            // 숫자가 없는 경우
            if (yearDigits.isEmpty()) {
                return "미상";
            }

            // 앞 2자리 추출 (예: "23"01 → "23")
            String yearPrefix = yearDigits.length() >= 2 ? yearDigits.substring(0, 2) : yearDigits;

            // 연도 앞자리가 90 이상이면 1900년대, 그렇지 않으면 2000년대로 판단
            int yearNum = Integer.parseInt(yearPrefix);
            return (yearNum >= 90 ? "19" : "20") + yearPrefix;
        };

        // 이상치 제거 (가격이 9999만원인 차량 제외)
        cars = cars.stream()
                .filter(car -> car.getPrice() != 9999)
                .collect(Collectors.toList());

        // 표준화된 연식을 기준으로 그룹화하여 통계 계산
        Map<String, List<Car>> groupedByYear = cars.stream()
                .collect(Collectors.groupingBy(
                        car -> standardizeYear.apply(car.getYear())
                ));

        // TreeMap을 사용하여 연식 기준으로 정렬 (오래된 차량 → 최신 차량 순)
        TreeMap<String, List<Car>> sortedMap = new TreeMap<>();
        sortedMap.putAll(groupedByYear);

        List<Map<String, Object>> result = sortedMap.entrySet().stream()
                .map(entry -> {
                    String year = entry.getKey();
                    List<Car> carsInYear = entry.getValue();

                    // 최저가, 평균가, 최고가 계산
                    long minPrice = carsInYear.stream()
                            .mapToLong(Car::getPrice)
                            .min()
                            .orElse(0);

                    double avgPrice = carsInYear.stream()
                            .mapToLong(Car::getPrice)
                            .average()
                            .orElse(0);

                    long maxPrice = carsInYear.stream()
                            .mapToLong(Car::getPrice)
                            .max()
                            .orElse(0);

                    long count = carsInYear.size();

                    // 결과 맵 생성
                    Map<String, Object> yearStats = Map.of(
                            "year", year + "년식",
                            "minPrice", minPrice,
                            "avgPrice", Math.round(avgPrice),
                            "maxPrice", maxPrice,
                            "count", count,
                            "originalYear", entry.getKey() // 정렬용 원본 연도 값 추가
                    );

                    return yearStats;
                })
                .collect(Collectors.toList());

        // 오래된 차량 → 최신 차량 순으로 정렬
        return result;
    }
}