package com.example.autofinder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "car_comparisons", indexes = {
        @Index(name = "idx_user_comparison", columnList = "userId"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // null 가능 (비로그인 사용자)

    @Column(name = "car_ids", nullable = false, length = 500)
    private String carIds; // 쉼표로 구분된 차량 ID들 (예: "1,2,3")

    @Column(name = "comparison_name", length = 200)
    private String comparisonName;

    @Column(name = "comparison_data", columnDefinition = "TEXT")
    private String comparisonData; // JSON 형태로 비교 결과 저장 (선택사항)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 비교한 차량 개수 반환
     */
    public int getCarCount() {
        if (carIds == null || carIds.trim().isEmpty()) {
            return 0;
        }
        return carIds.split(",").length;
    }

    /**
     * 차량 ID 리스트 반환
     */
    public java.util.List<Long> getCarIdsList() {
        if (carIds == null || carIds.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return java.util.Arrays.stream(carIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 비교 타입 반환 (차량 개수 기반)
     */
    public String getComparisonType() {
        int count = getCarCount();
        if (count == 2) return "2대 비교";
        if (count == 3) return "3대 비교";
        return count + "대 비교";
    }

    @Override
    public String toString() {
        return String.format("CarComparison{id=%d, userId=%d, carCount=%d, name='%s', createdAt=%s}",
                id, userId, getCarCount(), comparisonName, createdAt);
    }
}