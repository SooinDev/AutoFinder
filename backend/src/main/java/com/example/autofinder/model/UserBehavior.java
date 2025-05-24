package com.example.autofinder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_behaviors", indexes = {
        @Index(name = "idx_user_timestamp", columnList = "userId, timestamp"),
        @Index(name = "idx_car_timestamp", columnList = "carId, timestamp"),
        @Index(name = "idx_action_type", columnList = "actionType"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long carId;

    @Column(nullable = false, length = 50)
    private String actionType;

    @Column(length = 255)
    private String value;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 100)
    private String sessionId;  // 세션 추적용

    @Column(length = 255)
    private String userAgent;  // 브라우저 정보

    @Column(length = 45)
    private String ipAddress;  // IP 주소

    @Column(length = 500)
    private String referrer;   // 유입 경로

    // 생성 시 자동으로 현재 시간 설정
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // 편의 메서드들
    public boolean isRecentAction() {
        return timestamp.isAfter(LocalDateTime.now().minusDays(1));
    }

    public boolean isHighValueAction() {
        return "FAVORITE".equals(actionType) ||
                "INQUIRY".equals(actionType) ||
                "CONTACT".equals(actionType);
    }

    @Override
    public String toString() {
        return String.format("UserBehavior{id=%d, userId=%d, carId=%d, actionType='%s', timestamp=%s}",
                id, userId, carId, actionType, timestamp);
    }
}