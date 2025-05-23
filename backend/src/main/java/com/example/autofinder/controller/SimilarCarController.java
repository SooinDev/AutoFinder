package com.example.autofinder.controller;

import com.example.autofinder.model.Car;
import com.example.autofinder.service.SimilarCarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class SimilarCarController {
    private final SimilarCarService similarCarService;

    /**
     * 특정 차량과 유사한 차량 목록을 조회하는 API
     *
     * @param carId 기준 차량 ID
     * @param limit 조회할 최대 차량 수 (기본값: 5)
     * @return 유사한 차량 목록
     */
    @GetMapping("/{carId}/similar")
    public ResponseEntity<Page<Car>> getSimilarCars(
            @PathVariable Long carId,
            @RequestParam(defaultValue = "5") int limit) {

        Page<Car> similarCars = similarCarService.findSimilarCars(carId, limit);

        // 결과가 없을 경우 204 No Content 반환
        if (similarCars.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(similarCars);
    }
}