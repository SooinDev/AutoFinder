package com.example.autofinder.repository;

import com.example.autofinder.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
//    // 차량명 검색
//    Page<Car> findByModelContaining(String model, Pageable pageable);
//
//    // 연료 타입 필터링
//    Page<Car> findByFuel(String fuel, Pageable pageable);
//
//    // 지역 필터링
//    Page<Car> findByRegion(String region, Pageable pageable);
//
//    // 가격 범위 검색 (min ≤ price ≤ max)
//    Page<Car> findByPriceBetween(Long minPrice, Long maxPrice, Pageable pageable);

    @Query("SELECT c FROM Car c WHERE " +
            "(:model IS NULL OR c.model LIKE %:model%) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:minMileage IS NULL OR c.mileage >= :minMileage) AND " +
            "(:maxMileage IS NULL OR c.mileage <= :maxMileage) AND " +
            "(:fuel IS NULL OR LOWER(c.fuel) = LOWER(:fuel)) AND " +
            "(:region IS NULL OR LOWER(c.region) = LOWER(:region)) AND " +
            "(:year IS NULL OR CAST(c.year AS string) LIKE %:year%)")
    Page<Car> findByFilters(
            @Param("model") String model,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("fuel") String fuel,
            @Param("region") String region,
            @Param("year") String year,
            Pageable pageable
    );

    // 모델명으로 차량 목록 조회 (부분 일치)
    List<Car> findByModelContaining(String model);

    // 기본 유사 차량 찾기 메소드
    @Query("SELECT c FROM Car c WHERE " +
            "c.id != :carId AND " +
            "(:model IS NULL OR c.model LIKE %:model%) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:year IS NULL OR c.year LIKE %:year%) " +
            "ORDER BY " +
            "(CASE WHEN c.model = :exactModel THEN 0 ELSE 1 END), " +
            "ABS(c.price - :targetPrice * 1.0)")
    Page<Car> findSimilarCars(
            @Param("carId") Long carId,
            @Param("model") String model,
            @Param("exactModel") String exactModel,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("year") String year,
            @Param("targetPrice") Long targetPrice,
            Pageable pageable
    );

    // 고급 유사도 계산을 위한 차량 후보 선택 메소드
    @Query("SELECT c FROM Car c WHERE " +
            "c.id != :carId AND " +
            "(:modelBase IS NULL OR c.model LIKE %:modelBase%) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice)")
    Page<Car> findSimilarCarsForScoring(
            @Param("carId") Long carId,
            @Param("modelBase") String modelBase,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );
}
