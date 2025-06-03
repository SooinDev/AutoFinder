package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.CarComparison;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.CarComparisonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarComparisonService {

    private final CarRepository carRepository;
    private final CarComparisonRepository carComparisonRepository;
    private final AIRecommendationService aiRecommendationService;
    private final UserBehaviorService userBehaviorService;

    /**
     * 기본 차량 비교 기능
     */
    public Map<String, Object> compareCars(List<Long> carIds) {
        // 차량 데이터 조회
        List<Car> cars = carRepository.findAllById(carIds);

        if (cars.size() != carIds.size()) {
            List<Long> foundIds = cars.stream().map(Car::getId).collect(Collectors.toList());
            List<Long> missingIds = carIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("다음 차량을 찾을 수 없습니다: " + missingIds);
        }

        Map<String, Object> comparison = new HashMap<>();

        // 기본 차량 정보
        comparison.put("cars", cars.stream().map(this::mapCarToComparison).collect(Collectors.toList()));

        // 가격 비교 분석
        comparison.put("priceAnalysis", analyzePrices(cars));

        // 연식 비교 분석
        comparison.put("yearAnalysis", analyzeYears(cars));

        // 주행거리 비교 분석
        comparison.put("mileageAnalysis", analyzeMileage(cars));

        // 연료 타입 비교
        comparison.put("fuelAnalysis", analyzeFuelTypes(cars));

        // 지역 분석
        comparison.put("regionAnalysis", analyzeRegions(cars));

        // 종합 점수 계산
        comparison.put("overallScoring", calculateOverallScoring(cars));

        // 장단점 분석
        comparison.put("prosAndCons", analyzeProsAndCons(cars));

        // 추천 결론
        comparison.put("recommendation", generateRecommendation(cars));

        comparison.put("comparedAt", LocalDateTime.now());
        comparison.put("comparisonId", UUID.randomUUID().toString());

        log.info("차량 비교 완료: {} 대", cars.size());
        return comparison;
    }

    /**
     * 상세 차량 비교 (고급 분석 포함)
     */
    public Map<String, Object> compareDetailedCars(List<Long> carIds) {
        Map<String, Object> basicComparison = compareCars(carIds);
        List<Car> cars = carRepository.findAllById(carIds);

        // 상세 분석 추가
        Map<String, Object> detailedAnalysis = new HashMap<>();

        // 가성비 분석
        detailedAnalysis.put("valueAnalysis", analyzeValueForMoney(cars));

        // 감가상각 분석
        detailedAnalysis.put("depreciationAnalysis", analyzeDepreciation(cars));

        // 시장 인기도 분석
        detailedAnalysis.put("popularityAnalysis", analyzePopularity(cars));

        // 브랜드 신뢰도 분석
        detailedAnalysis.put("brandAnalysis", analyzeBrands(cars));

        // 유지비 예상 분석
        detailedAnalysis.put("maintenanceCostAnalysis", analyzeMaintenanceCosts(cars));

        // 투자 관점 분석
        detailedAnalysis.put("investmentAnalysis", analyzeInvestmentPotential(cars));

        // 기본 비교에 상세 분석 추가
        basicComparison.put("detailedAnalysis", detailedAnalysis);
        basicComparison.put("analysisType", "detailed");

        return basicComparison;
    }

    /**
     * 사용자 비교 기록 조회
     */
    public List<Map<String, Object>> getComparisonHistory(Long userId) {
        List<CarComparison> history = carComparisonRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return history.stream().map(comparison -> {
            Map<String, Object> historyItem = new HashMap<>();
            historyItem.put("id", comparison.getId());
            historyItem.put("comparisonName", comparison.getComparisonName());
            historyItem.put("carIds", Arrays.asList(comparison.getCarIds().split(",")));
            historyItem.put("createdAt", comparison.getCreatedAt());

            // 비교한 차량들의 기본 정보 추가
            List<Long> carIds = Arrays.stream(comparison.getCarIds().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            List<Car> cars = carRepository.findAllById(carIds);
            historyItem.put("cars", cars.stream().map(this::mapCarToBasicInfo).collect(Collectors.toList()));

            return historyItem;
        }).collect(Collectors.toList());
    }

    /**
     * 비교 결과 저장
     */
    public Long saveComparison(Long userId, List<Long> carIds, String comparisonName) {
        CarComparison comparison = new CarComparison();
        comparison.setUserId(userId);
        comparison.setCarIds(carIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        comparison.setComparisonName(comparisonName != null ? comparisonName : generateDefaultComparisonName(carIds));
        comparison.setCreatedAt(LocalDateTime.now());

        CarComparison saved = carComparisonRepository.save(comparison);

        // 사용자 행동 기록 (각 차량에 대해)
        if (userId != null && userBehaviorService != null) {
            for (Long carId : carIds) {
                try {
                    userBehaviorService.recordUserAction(userId, carId, "COMPARE", "car_comparison");
                } catch (Exception e) {
                    log.warn("비교 행동 기록 실패 - 사용자: {}, 차량: {}", userId, carId, e);
                }
            }
        }

        log.info("차량 비교 결과 저장 완료 - ID: {}, 사용자: {}", saved.getId(), userId);
        return saved.getId();
    }

    /**
     * 추천 차량과 비교
     */
    public Map<String, Object> compareWithRecommendations(Long userId, Long targetCarId, int recommendationCount) {
        // 타겟 차량 조회
        Car targetCar = carRepository.findById(targetCarId)
                .orElseThrow(() -> new IllegalArgumentException("타겟 차량을 찾을 수 없습니다: " + targetCarId));

        // 추천 차량 조회
        List<AIRecommendationService.RecommendedCar> recommendations =
                aiRecommendationService.getRecommendationsForUser(userId, recommendationCount + 5);

        // 타겟 차량과 다른 추천 차량들 선택
        List<Car> recommendedCars = recommendations.stream()
                .filter(rec -> !rec.getCar().getId().equals(targetCarId))
                .limit(recommendationCount)
                .map(AIRecommendationService.RecommendedCar::getCar)
                .collect(Collectors.toList());

        if (recommendedCars.isEmpty()) {
            throw new IllegalArgumentException("비교할 추천 차량을 찾을 수 없습니다.");
        }

        // 타겟 차량 + 추천 차량들로 비교 리스트 생성
        List<Long> compareCarIds = new ArrayList<>();
        compareCarIds.add(targetCarId);
        compareCarIds.addAll(recommendedCars.stream().map(Car::getId).collect(Collectors.toList()));

        // 비교 수행
        Map<String, Object> comparison = compareCars(compareCarIds);

        // 추천 정보 추가
        comparison.put("targetCarId", targetCarId);

        List<Map<String, Object>> recommendationInfo = recommendations.stream()
                .filter(rec -> recommendedCars.stream().anyMatch(car -> car.getId().equals(rec.getCar().getId())))
                .map(rec -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("carId", rec.getCar().getId());
                    info.put("similarityScore", rec.getSimilarityScore());
                    info.put("reason", rec.getRecommendationReason());
                    return info;
                })
                .collect(Collectors.toList());

        comparison.put("recommendationInfo", recommendationInfo);

        return comparison;
    }

    /**
     * 비교 통계 조회
     */
    public Map<String, Object> getComparisonStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 전체 비교 횟수
        long totalComparisons = carComparisonRepository.count();
        stats.put("totalComparisons", totalComparisons);

        // 최근 7일 비교 횟수
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentComparisons = carComparisonRepository.countByCreatedAtAfter(sevenDaysAgo);
        stats.put("recentComparisons", recentComparisons);

        // 가장 많이 비교된 차량들
        List<Object[]> mostComparedCars = carComparisonRepository.findMostComparedCars();
        stats.put("mostComparedCars", mostComparedCars);

        // 평균 비교 차량 수
        Double avgCarsPerComparison = carComparisonRepository.getAverageCarCountPerComparison();
        stats.put("averageCarsPerComparison", avgCarsPerComparison);

        return stats;
    }

    // === 분석 메서드들 ===

    private Map<String, Object> analyzePrices(List<Car> cars) {
        List<Long> prices = cars.stream()
                .map(Car::getPrice)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> analysis = new HashMap<>();

        if (!prices.isEmpty()) {
            analysis.put("lowest", prices.get(0));
            analysis.put("highest", prices.get(prices.size() - 1));
            analysis.put("average", prices.stream().mapToLong(Long::longValue).average().orElse(0));
            analysis.put("priceRange", prices.get(prices.size() - 1) - prices.get(0));

            // 가격 차이 백분율
            if (prices.get(0) > 0) {
                double priceDifferencePercent = ((double)(prices.get(prices.size() - 1) - prices.get(0)) / prices.get(0)) * 100;
                analysis.put("priceDifferencePercent", Math.round(priceDifferencePercent * 100.0) / 100.0);
            }

            // 각 차량의 가격 순위
            List<Map<String, Object>> priceRanking = new ArrayList<>();
            for (Car car : cars) {
                if (car.getPrice() != null) {
                    int rank = prices.indexOf(car.getPrice()) + 1;
                    Map<String, Object> rankInfo = new HashMap<>();
                    rankInfo.put("carId", car.getId());
                    rankInfo.put("model", car.getModel());
                    rankInfo.put("price", car.getPrice());
                    rankInfo.put("rank", rank);
                    rankInfo.put("isLowest", rank == 1);
                    rankInfo.put("isHighest", rank == prices.size());
                    priceRanking.add(rankInfo);
                }
            }
            analysis.put("ranking", priceRanking);
        }

        return analysis;
    }

    private Map<String, Object> analyzeYears(List<Car> cars) {
        List<Integer> years = cars.stream()
                .map(car -> extractYear(car.getYear()))
                .filter(year -> year > 0)
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> analysis = new HashMap<>();

        if (!years.isEmpty()) {
            analysis.put("oldest", years.get(0));
            analysis.put("newest", years.get(years.size() - 1));
            analysis.put("averageYear", years.stream().mapToInt(Integer::intValue).average().orElse(0));
            analysis.put("yearRange", years.get(years.size() - 1) - years.get(0));

            // 연식별 차량 수
            Map<Integer, Long> yearCounts = years.stream()
                    .collect(Collectors.groupingBy(year -> year, Collectors.counting()));
            analysis.put("yearDistribution", yearCounts);
        }

        return analysis;
    }

    private Map<String, Object> analyzeMileage(List<Car> cars) {
        List<Long> mileages = cars.stream()
                .map(Car::getMileage)
                .filter(Objects::nonNull)
                .filter(mileage -> mileage > 0)
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> analysis = new HashMap<>();

        if (!mileages.isEmpty()) {
            analysis.put("lowest", mileages.get(0));
            analysis.put("highest", mileages.get(mileages.size() - 1));
            analysis.put("average", mileages.stream().mapToLong(Long::longValue).average().orElse(0));
            analysis.put("mileageRange", mileages.get(mileages.size() - 1) - mileages.get(0));

            // 주행거리 등급 분류
            List<Map<String, Object>> mileageGrades = cars.stream()
                    .filter(car -> car.getMileage() != null && car.getMileage() > 0)
                    .map(car -> {
                        String grade = getMileageGrade(car.getMileage());
                        Map<String, Object> gradeInfo = new HashMap<>();
                        gradeInfo.put("carId", car.getId());
                        gradeInfo.put("model", car.getModel());
                        gradeInfo.put("mileage", car.getMileage());
                        gradeInfo.put("grade", grade);
                        return gradeInfo;
                    })
                    .collect(Collectors.toList());
            analysis.put("mileageGrades", mileageGrades);
        }

        return analysis;
    }

    private Map<String, Object> analyzeFuelTypes(List<Car> cars) {
        Map<String, Long> fuelCounts = cars.stream()
                .map(Car::getFuel)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(fuel -> fuel, Collectors.counting()));

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("distribution", fuelCounts);
        analysis.put("uniqueFuelTypes", fuelCounts.size());

        // 가장 일반적인 연료 타입
        fuelCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> analysis.put("mostCommon", entry.getKey()));

        return analysis;
    }

    private Map<String, Object> analyzeRegions(List<Car> cars) {
        Map<String, Long> regionCounts = cars.stream()
                .map(Car::getRegion)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(region -> region, Collectors.counting()));

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("distribution", regionCounts);
        analysis.put("uniqueRegions", regionCounts.size());

        return analysis;
    }

    private Map<String, Object> calculateOverallScoring(List<Car> cars) {
        Map<String, Object> scoring = new HashMap<>();

        List<Map<String, Object>> carScores = cars.stream().map(car -> {
            double score = 0.0;
            int factors = 0;

            // 가격 점수 (낮을수록 좋음, 역계산)
            if (car.getPrice() != null && car.getPrice() > 0) {
                double priceScore = Math.max(0, 10 - (car.getPrice() / 1000.0)); // 1000만원당 1점 감점
                score += Math.max(0, Math.min(10, priceScore));
                factors++;
            }

            // 연식 점수 (최신일수록 좋음)
            int year = extractYear(car.getYear());
            if (year > 0) {
                double yearScore = Math.max(0, Math.min(10, (year - 2010) * 0.7)); // 2010년 이후로 계산
                score += yearScore;
                factors++;
            }

            // 주행거리 점수 (적을수록 좋음)
            if (car.getMileage() != null && car.getMileage() >= 0) {
                double mileageScore = Math.max(0, 10 - (car.getMileage() / 20000.0)); // 2만km당 1점 감점
                score += Math.max(0, Math.min(10, mileageScore));
                factors++;
            }

            double finalScore = factors > 0 ? score / factors : 0.0;

            Map<String, Object> scoreInfo = new HashMap<>();
            scoreInfo.put("carId", car.getId());
            scoreInfo.put("model", car.getModel());
            scoreInfo.put("score", Math.round(finalScore * 100.0) / 100.0);
            scoreInfo.put("grade", getScoreGrade(finalScore));
            return scoreInfo;
        }).collect(Collectors.toList());

        // 점수순 정렬
        carScores.sort((a, b) -> Double.compare(
                (Double) b.get("score"),
                (Double) a.get("score")
        ));

        scoring.put("carScores", carScores);
        scoring.put("winner", carScores.isEmpty() ? null : carScores.get(0));

        return scoring;
    }

    private Map<String, Object> analyzeProsAndCons(List<Car> cars) {
        Map<String, Object> analysis = new HashMap<>();

        List<Map<String, Object>> prosAndCons = cars.stream().map(car -> {
            List<String> pros = new ArrayList<>();
            List<String> cons = new ArrayList<>();

            // 가격 분석
            if (car.getPrice() != null) {
                if (car.getPrice() < 2000) {
                    pros.add("경제적인 가격");
                } else if (car.getPrice() > 5000) {
                    cons.add("높은 가격");
                }
            }

            // 연식 분석
            int year = extractYear(car.getYear());
            if (year > 0) {
                if (year >= 2020) {
                    pros.add("최신 연식");
                } else if (year < 2015) {
                    cons.add("오래된 연식");
                }
            }

            // 주행거리 분석
            if (car.getMileage() != null) {
                if (car.getMileage() < 50000) {
                    pros.add("낮은 주행거리");
                } else if (car.getMileage() > 150000) {
                    cons.add("높은 주행거리");
                }
            }

            // 연료 타입 분석
            if ("하이브리드".equals(car.getFuel()) || "전기".equals(car.getFuel())) {
                pros.add("친환경 연료");
            }

            Map<String, Object> prosConsInfo = new HashMap<>();
            prosConsInfo.put("carId", car.getId());
            prosConsInfo.put("model", car.getModel());
            prosConsInfo.put("pros", pros);
            prosConsInfo.put("cons", cons);
            return prosConsInfo;
        }).collect(Collectors.toList());

        analysis.put("carsAnalysis", prosAndCons);
        return analysis;
    }

    private Map<String, Object> generateRecommendation(List<Car> cars) {
        Map<String, Object> recommendation = new HashMap<>();

        // 가격 기준 추천
        Car cheapestCar = cars.stream()
                .filter(car -> car.getPrice() != null)
                .min(Comparator.comparing(Car::getPrice))
                .orElse(null);

        // 연식 기준 추천
        Car newestCar = cars.stream()
                .max(Comparator.comparing(car -> extractYear(car.getYear())))
                .orElse(null);

        // 주행거리 기준 추천
        Car lowestMileageCar = cars.stream()
                .filter(car -> car.getMileage() != null)
                .min(Comparator.comparing(Car::getMileage))
                .orElse(null);

        if (cheapestCar != null) {
            Map<String, Object> budgetChoice = new HashMap<>();
            budgetChoice.put("car", mapCarToBasicInfo(cheapestCar));
            budgetChoice.put("reason", "가장 경제적인 선택");
            recommendation.put("budgetChoice", budgetChoice);
        } else {
            recommendation.put("budgetChoice", null);
        }

        if (newestCar != null) {
            Map<String, Object> latestChoice = new HashMap<>();
            latestChoice.put("car", mapCarToBasicInfo(newestCar));
            latestChoice.put("reason", "가장 최신 연식");
            recommendation.put("latestChoice", latestChoice);
        } else {
            recommendation.put("latestChoice", null);
        }

        if (lowestMileageCar != null) {
            Map<String, Object> qualityChoice = new HashMap<>();
            qualityChoice.put("car", mapCarToBasicInfo(lowestMileageCar));
            qualityChoice.put("reason", "가장 낮은 주행거리");
            recommendation.put("qualityChoice", qualityChoice);
        } else {
            recommendation.put("qualityChoice", null);
        }

        // 종합 추천 (점수 기반)
        Map<String, Object> overallScoring = calculateOverallScoring(cars);
        @SuppressWarnings("unchecked")
        Map<String, Object> winner = (Map<String, Object>) overallScoring.get("winner");

        if (winner != null) {
            Map<String, Object> overallChoice = new HashMap<>();
            overallChoice.put("carId", winner.get("carId"));
            overallChoice.put("model", winner.get("model"));
            overallChoice.put("score", winner.get("score"));
            overallChoice.put("reason", "종합 점수가 가장 높음");
            recommendation.put("overallChoice", overallChoice);
        } else {
            recommendation.put("overallChoice", null);
        }

        return recommendation;
    }

    // === 상세 분석 메서드들 ===

    private Map<String, Object> analyzeValueForMoney(List<Car> cars) {
        List<Map<String, Object>> valueAnalysis = cars.stream()
                .filter(car -> car.getPrice() != null && car.getPrice() > 0)
                .map(car -> {
                    double valueScore = calculateValueScore(car);
                    Map<String, Object> valueInfo = new HashMap<>();
                    valueInfo.put("carId", car.getId());
                    valueInfo.put("model", car.getModel());
                    valueInfo.put("price", car.getPrice());
                    valueInfo.put("valueScore", Math.round(valueScore * 100.0) / 100.0);
                    valueInfo.put("valueGrade", getValueGrade(valueScore));
                    return valueInfo;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("valueScore"), (Double) a.get("valueScore")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("analysis", valueAnalysis);
        result.put("bestValue", valueAnalysis.isEmpty() ? null : valueAnalysis.get(0));
        return result;
    }

    private Map<String, Object> analyzeDepreciation(List<Car> cars) {
        List<Map<String, Object>> depreciationAnalysis = cars.stream().map(car -> {
            int year = extractYear(car.getYear());
            int age = Math.max(0, 2024 - year);

            double depreciationRate = calculateDepreciationRate(car);
            String depreciationLevel = getDepreciationLevel(depreciationRate);

            Map<String, Object> depreciationInfo = new HashMap<>();
            depreciationInfo.put("carId", car.getId());
            depreciationInfo.put("model", car.getModel());
            depreciationInfo.put("age", age);
            depreciationInfo.put("depreciationRate", Math.round(depreciationRate * 100.0) / 100.0);
            depreciationInfo.put("depreciationLevel", depreciationLevel);
            depreciationInfo.put("futureValue", estimateFutureValue(car, 3)); // 3년 후 예상 가치
            return depreciationInfo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("depreciationAnalysis", depreciationAnalysis);
        return result;
    }

    private Map<String, Object> analyzePopularity(List<Car> cars) {
        List<Map<String, Object>> popularityAnalysis = cars.stream().map(car -> {
            int popularityScore = calculatePopularityScore(car);
            Map<String, Object> popularityInfo = new HashMap<>();
            popularityInfo.put("carId", car.getId());
            popularityInfo.put("model", car.getModel());
            popularityInfo.put("popularityScore", popularityScore);
            popularityInfo.put("popularityLevel", getPopularityLevel(popularityScore));
            return popularityInfo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("popularityAnalysis", popularityAnalysis);
        return result;
    }

    private Map<String, Object> analyzeBrands(List<Car> cars) {
        Map<String, List<Car>> brandGroups = cars.stream()
                .collect(Collectors.groupingBy(car -> extractBrand(car.getModel())));

        Map<String, Object> brandAnalysis = new HashMap<>();

        brandGroups.forEach((brand, brandCars) -> {
            Map<String, Object> brandInfo = new HashMap<>();
            brandInfo.put("carCount", brandCars.size());
            brandInfo.put("reliability", getBrandReliability(brand));
            brandInfo.put("luxuryLevel", getBrandLuxuryLevel(brand));
            brandInfo.put("maintenanceCost", getBrandMaintenanceCost(brand));
            brandAnalysis.put(brand, brandInfo);
        });

        return brandAnalysis;
    }

    private Map<String, Object> analyzeMaintenanceCosts(List<Car> cars) {
        List<Map<String, Object>> maintenanceAnalysis = cars.stream().map(car -> {
            String brand = extractBrand(car.getModel());
            int year = extractYear(car.getYear());
            Long mileage = car.getMileage();

            int annualMaintenanceCost = estimateMaintenanceCost(brand, year, mileage);

            Map<String, Object> maintenanceInfo = new HashMap<>();
            maintenanceInfo.put("carId", car.getId());
            maintenanceInfo.put("model", car.getModel());
            maintenanceInfo.put("estimatedAnnualCost", annualMaintenanceCost);
            maintenanceInfo.put("costLevel", getMaintenanceCostLevel(annualMaintenanceCost));
            return maintenanceInfo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("maintenanceAnalysis", maintenanceAnalysis);
        return result;
    }

    private Map<String, Object> analyzeInvestmentPotential(List<Car> cars) {
        List<Map<String, Object>> investmentAnalysis = cars.stream().map(car -> {
            double investmentScore = calculateInvestmentScore(car);
            String investmentGrade = getInvestmentGrade(investmentScore);

            Map<String, Object> investmentInfo = new HashMap<>();
            investmentInfo.put("carId", car.getId());
            investmentInfo.put("model", car.getModel());
            investmentInfo.put("investmentScore", Math.round(investmentScore * 100.0) / 100.0);
            investmentInfo.put("investmentGrade", investmentGrade);
            investmentInfo.put("recommendation", getInvestmentRecommendation(investmentScore));
            return investmentInfo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("investmentAnalysis", investmentAnalysis);
        return result;
    }

    // === 유틸리티 메서드들 ===

    private Map<String, Object> mapCarToComparison(Car car) {
        Map<String, Object> carMap = new HashMap<>();
        carMap.put("id", car.getId());
        carMap.put("model", car.getModel());
        carMap.put("year", car.getYear());
        carMap.put("price", car.getPrice());
        carMap.put("mileage", car.getMileage());
        carMap.put("fuel", car.getFuel());
        carMap.put("region", car.getRegion());
        carMap.put("carType", car.getCarType());
        carMap.put("imageUrl", car.getImageUrl());
        carMap.put("url", car.getUrl());
        carMap.put("brand", extractBrand(car.getModel()));
        carMap.put("yearNumeric", extractYear(car.getYear()));
        carMap.put("age", 2024 - extractYear(car.getYear()));
        return carMap;
    }

    private Map<String, Object> mapCarToBasicInfo(Car car) {
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("id", car.getId());
        basicInfo.put("model", car.getModel());
        basicInfo.put("year", car.getYear());
        basicInfo.put("price", car.getPrice() != null ? car.getPrice() : 0);
        basicInfo.put("imageUrl", car.getImageUrl() != null ? car.getImageUrl() : "");
        return basicInfo;
    }

    private int extractYear(String yearStr) {
        if (yearStr == null || yearStr.trim().isEmpty()) {
            return 2020;
        }

        try {
            String digits = yearStr.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) {
                return Integer.parseInt(digits.substring(0, 4));
            } else if (digits.length() == 2) {
                int year = Integer.parseInt(digits);
                return year > 50 ? 1900 + year : 2000 + year;
            }
        } catch (NumberFormatException e) {
            log.debug("연식 파싱 실패: {}", yearStr);
        }

        return 2020;
    }

    private String extractBrand(String model) {
        if (model == null) return "기타";

        String[] brands = {"현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디", "볼보", "폭스바겐"};
        for (String brand : brands) {
            if (model.contains(brand)) {
                return brand;
            }
        }

        String[] parts = model.split("\\s+");
        return parts.length > 0 ? parts[0] : "기타";
    }

    private String getMileageGrade(Long mileage) {
        if (mileage == null) return "정보없음";
        if (mileage < 30000) return "우수";
        if (mileage < 80000) return "양호";
        if (mileage < 150000) return "보통";
        return "주의";
    }

    private String getScoreGrade(double score) {
        if (score >= 8.0) return "A";
        if (score >= 6.0) return "B";
        if (score >= 4.0) return "C";
        return "D";
    }

    private double calculateValueScore(Car car) {
        double score = 5.0; // 기본 점수

        // 가격 대비 연식 점수
        if (car.getPrice() != null && car.getPrice() > 0) {
            int year = extractYear(car.getYear());
            int age = 2024 - year;

            // 연식이 최신일수록, 가격이 저렴할수록 높은 점수
            double yearScore = Math.max(0, 10 - age * 0.5);
            double priceScore = Math.max(0, 10 - car.getPrice() / 500.0);

            score = (yearScore + priceScore) / 2;
        }

        return Math.max(0, Math.min(10, score));
    }

    private String getValueGrade(double score) {
        if (score >= 8.0) return "최고가성비";
        if (score >= 6.0) return "우수가성비";
        if (score >= 4.0) return "보통가성비";
        return "가성비 미흡";
    }

    private double calculateDepreciationRate(Car car) {
        int year = extractYear(car.getYear());
        int age = Math.max(0, 2024 - year);

        // 일반적인 차량 감가상각률 (연간 15-20%)
        double baseRate = 0.17; // 17%

        // 브랜드별 감가상각률 조정
        String brand = extractBrand(car.getModel());
        switch (brand) {
            case "BMW":
            case "벤츠":
            case "아우디":
                baseRate = 0.20; // 수입차는 감가상각이 빠름
                break;
            case "현대":
            case "기아":
                baseRate = 0.15; // 국산차는 상대적으로 안정적
                break;
        }

        return baseRate * age;
    }

    private String getDepreciationLevel(double rate) {
        if (rate < 0.3) return "낮음";
        if (rate < 0.6) return "보통";
        return "높음";
    }

    private long estimateFutureValue(Car car, int yearsLater) {
        if (car.getPrice() == null) return 0;

        double depreciationRate = calculateDepreciationRate(car);
        double futureDepreciation = Math.min(0.8, depreciationRate + (yearsLater * 0.15));

        return Math.round(car.getPrice() * (1 - futureDepreciation));
    }

    private int calculatePopularityScore(Car car) {
        String brand = extractBrand(car.getModel());
        int score = 5;

        // 브랜드별 인기도
        switch (brand) {
            case "현대":
            case "기아":
                score = 8;
                break;
            case "BMW":
            case "벤츠":
                score = 9;
                break;
            case "제네시스":
                score = 7;
                break;
            default:
                score = 5;
        }

        // 연식 보정
        int year = extractYear(car.getYear());
        if (year >= 2020) score += 1;
        else if (year < 2015) score -= 2;

        return Math.max(1, Math.min(10, score));
    }

    private String getPopularityLevel(int score) {
        if (score >= 8) return "높음";
        if (score >= 6) return "보통";
        return "낮음";
    }

    private String getBrandReliability(String brand) {
        switch (brand) {
            case "현대":
            case "기아":
                return "높음";
            case "BMW":
            case "벤츠":
            case "아우디":
                return "보통";
            default:
                return "보통";
        }
    }

    private String getBrandLuxuryLevel(String brand) {
        switch (brand) {
            case "BMW":
            case "벤츠":
            case "아우디":
            case "제네시스":
                return "프리미엄";
            case "현대":
            case "기아":
                return "일반";
            default:
                return "일반";
        }
    }

    private String getBrandMaintenanceCost(String brand) {
        switch (brand) {
            case "BMW":
            case "벤츠":
            case "아우디":
                return "높음";
            case "현대":
            case "기아":
                return "보통";
            default:
                return "보통";
        }
    }

    private int estimateMaintenanceCost(String brand, int year, Long mileage) {
        int baseCost = 80; // 기본 연간 유지비 (만원)

        // 브랜드별 유지비
        switch (brand) {
            case "BMW":
            case "벤츠":
            case "아우디":
                baseCost = 200;
                break;
            case "현대":
            case "기아":
                baseCost = 100;
                break;
            case "제네시스":
                baseCost = 150;
                break;
        }

        // 연식 보정 (오래될수록 유지비 증가)
        int age = 2024 - year;
        baseCost += age * 10;

        // 주행거리 보정
        if (mileage != null && mileage > 100000) {
            baseCost += 50;
        }

        return Math.max(50, baseCost);
    }

    private String getMaintenanceCostLevel(int cost) {
        if (cost < 100) return "저렴";
        if (cost < 180) return "보통";
        return "비싸다";
    }

    private double calculateInvestmentScore(Car car) {
        double score = 5.0;

        // 브랜드 가치 유지력
        String brand = extractBrand(car.getModel());
        switch (brand) {
            case "현대":
            case "기아":
                score += 2;
                break;
            case "BMW":
            case "벤츠":
                score += 1; // 비싸지만 감가상각도 빠름
                break;
            case "제네시스":
                score += 1.5;
                break;
        }

        // 연식 점수
        int year = extractYear(car.getYear());
        if (year >= 2020) score += 2;
        else if (year >= 2018) score += 1;
        else if (year < 2015) score -= 2;

        // 주행거리 점수
        if (car.getMileage() != null) {
            if (car.getMileage() < 50000) score += 1;
            else if (car.getMileage() > 150000) score -= 2;
        }

        return Math.max(0, Math.min(10, score));
    }

    private String getInvestmentGrade(double score) {
        if (score >= 8.0) return "우수";
        if (score >= 6.0) return "양호";
        if (score >= 4.0) return "보통";
        return "주의";
    }

    private String getInvestmentRecommendation(double score) {
        if (score >= 8.0) return "투자 가치가 높은 차량입니다";
        if (score >= 6.0) return "안정적인 투자 대상입니다";
        if (score >= 4.0) return "신중한 검토가 필요합니다";
        return "투자 관점에서는 권장하지 않습니다";
    }

    private String generateDefaultComparisonName(List<Long> carIds) {
        return "차량 비교 " + carIds.size() + "대 - " +
                LocalDateTime.now().toString().substring(0, 16);
    }
}