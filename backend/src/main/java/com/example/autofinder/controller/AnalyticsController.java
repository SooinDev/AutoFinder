package com.example.autofinder.controller;

import com.example.autofinder.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    /**
     * 특정 모델의 연식별 가격 통계 조회 API
     * @param model 차량 모델명
     * @return 연식별 가격 통계 데이터 (최저가, 평균가, 최고가)
     */
    @GetMapping("/price-by-year/{model}")
    public ResponseEntity<List<Map<String, Object>>> getPriceStatsByYear(@PathVariable String model) {
        List<Map<String, Object>> priceStats = analyticsService.getPriceStatsByYear(model);
        return ResponseEntity.ok(priceStats);
    }
}