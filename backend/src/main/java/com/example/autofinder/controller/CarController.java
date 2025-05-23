package com.example.autofinder.controller;

import com.example.autofinder.model.Car;
import com.example.autofinder.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    // 차량 검색 + 필터링 API (페이징 포함)
    @GetMapping
    public ResponseEntity<?> searchCars(
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minMileage,
            @RequestParam(required = false) Integer maxMileage,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "0") int page,   // 현재 페이지 (기본: 0)
            @RequestParam(defaultValue = "21") int size   // 한 페이지당 차량 개수 (기본: 20)
    ) {
        try {
            // URL 디코딩 (한글 및 특수문자 처리)
            model = Optional.ofNullable(model)
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .orElse(null);
            fuel = Optional.ofNullable(fuel)
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .orElse(null);
            region = Optional.ofNullable(region)
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .orElse(null);
            year = Optional.ofNullable(year)
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .map(this::convertYearFormat)
                    .orElse(null);

            // 기본 정렬: 최신 등록된 차량 순 (createdAt 기준 내림차순)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 차량 검색 서비스 호출
            Page<Car> cars = carService.searchCars(model, minPrice, maxPrice, minMileage, maxMileage, fuel, region, year, pageable);

            // 검색 결과가 없을 경우 204 No Content 반환
            if (cars.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("검색 결과가 없습니다.");
            }

            return ResponseEntity.ok(cars);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 연식 변환 메서드
     * @param year 사용자 입력 연식 (예: "2023", "23", "1999", "99")
     * @return 데이터베이스 검색용 연식 형식 (예: "23/", "99/")
     */
    private String convertYearFormat(String year) {
        try {
            // 빈 값 체크
            if (year == null || year.trim().isEmpty()) {
                return null;
            }

            // 숫자만 추출
            String yearDigits = year.replaceAll("[^0-9]", "");
            if (yearDigits.isEmpty()) {
                return null;
            }

            // 2자리 숫자 형식 체크
            if (yearDigits.length() == 2) {
                // 이미 2자리 형식인 경우 (예: "23") -> "23/" 형식으로 변환
                return yearDigits + "/";
            } else if (yearDigits.length() == 4) {
                // 4자리 연도인 경우 (예: "2023" 또는 "1999") -> "23/" 또는 "99/" 형식으로 변환
                return yearDigits.substring(2) + "/";
            } else if (yearDigits.length() >= 4) {
                // 4자리 이상인 경우 (예: "2301년식") -> 앞 2자리 추출하여 변환 ("23/")
                return yearDigits.substring(0, 2) + "/";
            }

            // 그 외의 경우 원본 값 유지
            return year;
        } catch (Exception e) {
            System.err.println("연식 변환 오류: " + e.getMessage()); // 오류 발생 시 로그 출력
            return year; // 변환 실패 시 원본 값 유지
        }
    }

    // 전체 차량 목록 조회 API (페이징 지원)
    @GetMapping("/all")
    public ResponseEntity<Page<Car>> getAllCars(Pageable pageable) {
        Page<Car> cars = carService.getAllCars(pageable);
        return ResponseEntity.ok(cars);
    }

    // 특정 차량 조회 (ID 기반)
    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        return carService.getCarById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 차량 추가 (CREATE)
    @PostMapping
    public ResponseEntity<Car> addCar(@RequestBody Car car) {
        Car savedCar = carService.addCar(car);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCar);
    }

    // 차량 정보 수정 (UPDATE)
    @PutMapping("/{id}")
    public ResponseEntity<Car> updateCar(@PathVariable Long id, @RequestBody Car updatedCar) {
        try {
            Car car = carService.updateCar(id, updatedCar);
            return ResponseEntity.ok(car);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 차량 삭제 (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        try {
            carService.deleteCar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}