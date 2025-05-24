-- ========================================
-- AutoFinder 데이터베이스 스키마 (깔끔한 버전)
-- ========================================

-- 차량 테이블
CREATE TABLE cars (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      car_type VARCHAR(50) NOT NULL,
                      model VARCHAR(100) NOT NULL,
                      year VARCHAR(20) NOT NULL,
                      mileage VARCHAR(20) DEFAULT '정보 없음',
                      price BIGINT UNSIGNED NOT NULL DEFAULT 0,
                      fuel VARCHAR(20) NOT NULL,
                      region VARCHAR(50) NOT NULL,
                      url VARCHAR(255) UNIQUE,
                      image_url VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 테이블
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL
);

-- 즐겨찾기 테이블
CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           car_id BIGINT NOT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE
);

-- 사용자 선호도 테이블
CREATE TABLE user_preferences (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  min_price BIGINT,
                                  max_price BIGINT,
                                  min_year INT,
                                  max_year INT,
                                  max_mileage BIGINT,
                                  preferred_brands VARCHAR(255),
                                  preferred_fuel_types VARCHAR(255),
                                  preferred_regions VARCHAR(255),
                                  preference_score DOUBLE DEFAULT 0.0,
                                  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 사용자 행동 테이블
CREATE TABLE user_behaviors (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                car_id BIGINT NOT NULL,
                                action_type VARCHAR(50) NOT NULL,
                                value VARCHAR(255),
                                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                session_id VARCHAR(100),
                                user_agent VARCHAR(255),
                                ip_address VARCHAR(45),
                                referrer VARCHAR(500),
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 성능 최적화 인덱스
                                INDEX idx_user_timestamp (user_id, timestamp),
                                INDEX idx_car_timestamp (car_id, timestamp),
                                INDEX idx_action_type (action_type),
                                INDEX idx_timestamp (timestamp),
                                INDEX idx_session (session_id),
                                INDEX idx_user_car (user_id, car_id),
                                INDEX idx_user_action (user_id, action_type),

    -- 외래키 제약조건
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE
);

-- 행동 타입 제약조건 (데이터 일관성)
ALTER TABLE user_behaviors
    ADD CONSTRAINT chk_action_type
        CHECK (action_type IN (
                               'VIEW',          -- 페이지 조회
                               'CLICK',         -- 클릭
                               'DETAIL_VIEW',   -- 상세보기
                               'SEARCH',        -- 검색
                               'FILTER',        -- 필터 적용
                               'BOOKMARK',      -- 북마크
                               'SHARE',         -- 공유
                               'FAVORITE',      -- 즐겨찾기
                               'INQUIRY',       -- 문의
                               'CONTACT',       -- 연락하기
                               'DOWNLOAD',      -- 다운로드
                               'COMPARE'        -- 비교하기
            ));

-- ========================================
-- 분석용 뷰 (선택사항)
-- ========================================

-- 사용자 행동 요약 뷰
CREATE VIEW user_behavior_summary AS
SELECT
    user_id,
    COUNT(*) as total_actions,
    COUNT(DISTINCT car_id) as unique_cars_viewed,
    COUNT(DISTINCT action_type) as action_diversity,
    MAX(timestamp) as last_activity,
    MIN(timestamp) as first_activity,
    DATEDIFF(MAX(timestamp), MIN(timestamp)) as active_days,
    AVG(CASE
            WHEN action_type = 'VIEW' THEN 1
            WHEN action_type = 'CLICK' THEN 1.5
            WHEN action_type = 'DETAIL_VIEW' THEN 2
            WHEN action_type = 'BOOKMARK' THEN 3
            WHEN action_type = 'INQUIRY' THEN 4
            WHEN action_type = 'FAVORITE' THEN 5
            WHEN action_type = 'CONTACT' THEN 6
            ELSE 1
        END) as avg_engagement_score
FROM user_behaviors
GROUP BY user_id;

-- 차량 인기도 요약 뷰
CREATE VIEW car_popularity_summary AS
SELECT
    car_id,
    COUNT(*) as total_interactions,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(CASE WHEN action_type IN ('FAVORITE', 'INQUIRY', 'CONTACT') THEN 1 END) as high_intent_actions,
    SUM(CASE
            WHEN action_type = 'VIEW' THEN 1
            WHEN action_type = 'CLICK' THEN 1.5
            WHEN action_type = 'DETAIL_VIEW' THEN 2
            WHEN action_type = 'BOOKMARK' THEN 3
            WHEN action_type = 'INQUIRY' THEN 4
            WHEN action_type = 'FAVORITE' THEN 5
            WHEN action_type = 'CONTACT' THEN 6
            ELSE 1
        END) as popularity_score,
    MAX(timestamp) as last_interaction
FROM user_behaviors
GROUP BY car_id
ORDER BY popularity_score DESC;

-- 실시간 활동 모니터링 뷰
CREATE VIEW real_time_activity AS
SELECT
    action_type,
    COUNT(*) as action_count,
    COUNT(DISTINCT user_id) as unique_users,
    MAX(timestamp) as latest_action
FROM user_behaviors
WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY action_type
ORDER BY action_count DESC;

-- ========================================
-- 초기화 (선택사항)
-- ========================================

-- 기존 사용자 데이터 정리 (필요시만 사용)
-- TRUNCATE TABLE users;