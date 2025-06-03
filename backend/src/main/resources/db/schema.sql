-- ========================================
-- AutoFinder 데이터베이스 스키마 (최종 완성 버전)
-- 차량 비교 기능 포함
-- ========================================

-- 차량 테이블
CREATE TABLE cars (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      car_type VARCHAR(50) NOT NULL,
                      model VARCHAR(100) NOT NULL,
                      year VARCHAR(20) NOT NULL,
                      mileage BIGINT DEFAULT NULL COMMENT '주행거리 (km, NULL 허용)',
                      price BIGINT UNSIGNED NOT NULL DEFAULT 0,
                      fuel VARCHAR(20) NOT NULL,
                      region VARCHAR(50) NOT NULL,
                      url VARCHAR(255) UNIQUE,
                      image_url VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 검색 최적화 인덱스
                      INDEX idx_model (model),
                      INDEX idx_price (price),
                      INDEX idx_year (year),
                      INDEX idx_fuel (fuel),
                      INDEX idx_region (region),
                      INDEX idx_created_at (created_at),
                      INDEX idx_search_combo (model, price, year, fuel)
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

    -- 중복 방지를 위한 유니크 제약조건
                           UNIQUE KEY unique_user_car (user_id, car_id),

                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE,

    -- 성능 최적화 인덱스
                           INDEX idx_user_created (user_id, created_at),
                           INDEX idx_car_created (car_id, created_at)
);

-- 🆕 차량 비교 테이블 (새로 추가)
CREATE TABLE car_comparisons (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id BIGINT DEFAULT NULL COMMENT '사용자 ID (비로그인 사용자는 NULL)',
                                 car_ids VARCHAR(500) NOT NULL COMMENT '비교 차량 ID들 (쉼표 구분: 1,2,3)',
                                 comparison_name VARCHAR(200) DEFAULT NULL COMMENT '사용자 지정 비교 이름',
                                 comparison_data TEXT DEFAULT NULL COMMENT '비교 결과 JSON 데이터 (선택사항)',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',

    -- 인덱스 생성
                                 INDEX idx_user_comparison (user_id),
                                 INDEX idx_created_at (created_at),
                                 INDEX idx_car_ids (car_ids(100)),
                                 INDEX idx_user_created_at (user_id, created_at),

    -- 외래키 제약조건
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = '차량 비교 기록 테이블';

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

                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 사용자당 하나의 선호도만 허용
                                  UNIQUE KEY unique_user_preference (user_id)
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

-- 행동 타입 제약조건 (데이터 일관성) - 🆕 COMPARE 추가
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
-- 🆕 비교 기능을 위한 분석 뷰
-- ========================================

-- 비교 통계 뷰
CREATE VIEW comparison_statistics AS
SELECT
    DATE(created_at) as comparison_date,
    COUNT(*) as daily_comparisons,
    COUNT(DISTINCT user_id) as unique_users,
    AVG(CHAR_LENGTH(car_ids) - CHAR_LENGTH(REPLACE(car_ids, ',', '')) + 1) as avg_cars_per_comparison,
    COUNT(CASE WHEN comparison_name IS NOT NULL THEN 1 END) as saved_comparisons,
    ROUND(COUNT(CASE WHEN comparison_name IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as save_rate_percent
FROM car_comparisons
WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(created_at)
ORDER BY comparison_date DESC;

-- 인기 차량 비교 뷰
CREATE VIEW popular_comparison_cars AS
SELECT
    car_id,
    c.model,
    SUBSTRING_INDEX(c.model, ' ', 1) as brand,
    comparison_count,
    RANK() OVER (ORDER BY comparison_count DESC) as popularity_rank
FROM (
         SELECT
             CAST(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(cc.car_ids, ',', numbers.n), ',', -1)) AS UNSIGNED) as car_id,
             COUNT(*) as comparison_count
         FROM car_comparisons cc
                  CROSS JOIN (
             SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3
             UNION ALL SELECT 4 UNION ALL SELECT 5
         ) numbers
         WHERE CHAR_LENGTH(cc.car_ids) - CHAR_LENGTH(REPLACE(cc.car_ids, ',', '')) >= numbers.n - 1
           AND cc.created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
           AND TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(cc.car_ids, ',', numbers.n), ',', -1)) REGEXP '^[0-9]+$'
         GROUP BY car_id
         HAVING comparison_count >= 2
     ) popularity
         LEFT JOIN cars c ON popularity.car_id = c.id
WHERE c.id IS NOT NULL
ORDER BY comparison_count DESC;

-- ========================================
-- 기존 분석용 뷰 (개선됨)
-- ========================================

-- 사용자 행동 요약 뷰 (비교 행동 포함)
CREATE VIEW user_behavior_summary AS
SELECT
    user_id,
    COUNT(*) as total_actions,
    COUNT(DISTINCT car_id) as unique_cars_viewed,
    COUNT(DISTINCT action_type) as action_diversity,
    MAX(timestamp) as last_activity,
    MIN(timestamp) as first_activity,
    DATEDIFF(MAX(timestamp), MIN(timestamp)) as active_days,
    COUNT(CASE WHEN action_type = 'COMPARE' THEN 1 END) as comparison_actions,
    AVG(CASE
            WHEN action_type = 'VIEW' THEN 1
            WHEN action_type = 'CLICK' THEN 1.5
            WHEN action_type = 'DETAIL_VIEW' THEN 2
            WHEN action_type = 'BOOKMARK' THEN 3
            WHEN action_type = 'INQUIRY' THEN 4
            WHEN action_type = 'FAVORITE' THEN 5
            WHEN action_type = 'CONTACT' THEN 6
            WHEN action_type = 'COMPARE' THEN 3.5  -- 🆕 비교 가중치
            ELSE 1
        END) as avg_engagement_score
FROM user_behaviors
GROUP BY user_id;

-- 차량 인기도 요약 뷰 (비교 점수 포함)
CREATE VIEW car_popularity_summary AS
SELECT
    car_id,
    COUNT(*) as total_interactions,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(CASE WHEN action_type IN ('FAVORITE', 'INQUIRY', 'CONTACT', 'COMPARE') THEN 1 END) as high_intent_actions,
    COUNT(CASE WHEN action_type = 'COMPARE' THEN 1 END) as comparison_count,
    SUM(CASE
            WHEN action_type = 'VIEW' THEN 1
            WHEN action_type = 'CLICK' THEN 1.5
            WHEN action_type = 'DETAIL_VIEW' THEN 2
            WHEN action_type = 'BOOKMARK' THEN 3
            WHEN action_type = 'INQUIRY' THEN 4
            WHEN action_type = 'FAVORITE' THEN 5
            WHEN action_type = 'CONTACT' THEN 6
            WHEN action_type = 'COMPARE' THEN 3.5  -- 🆕 비교 가중치
            ELSE 1
        END) as popularity_score,
    MAX(timestamp) as last_interaction
FROM user_behaviors
GROUP BY car_id
ORDER BY popularity_score DESC;

-- 실시간 활동 모니터링 뷰 (비교 활동 포함)
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

-- 🆕 사용자 비교 활동 뷰
CREATE VIEW user_comparison_activity AS
SELECT
    cc.user_id,
    u.username,
    COUNT(*) as total_comparisons,
    MIN(cc.created_at) as first_comparison,
    MAX(cc.created_at) as last_comparison,
    DATEDIFF(MAX(cc.created_at), MIN(cc.created_at)) as active_days,
    AVG(CHAR_LENGTH(cc.car_ids) - CHAR_LENGTH(REPLACE(cc.car_ids, ',', '')) + 1) as avg_cars_per_comparison,
    COUNT(CASE WHEN cc.comparison_name IS NOT NULL THEN 1 END) as saved_comparisons,
    CASE
        WHEN COUNT(*) = 1 THEN 'One-time'
        WHEN COUNT(*) BETWEEN 2 AND 5 THEN 'Casual'
        WHEN COUNT(*) BETWEEN 6 AND 15 THEN 'Regular'
        ELSE 'Power User'
        END as user_segment
FROM car_comparisons cc
         LEFT JOIN users u ON cc.user_id = u.id
WHERE cc.user_id IS NOT NULL
GROUP BY cc.user_id, u.username;

-- ========================================
-- 🆕 비교 기능을 위한 저장 프로시저
-- ========================================

DELIMITER //

-- 특정 차량과 자주 비교되는 차량들 조회
CREATE PROCEDURE GetFrequentlyComparedCars(
    IN target_car_id BIGINT,
    IN limit_count INT DEFAULT 10
)
BEGIN
    SELECT
        other_car_id,
        c.model as other_car_model,
        c.price as other_car_price,
        c.year as other_car_year,
        co_comparison_count,
        ROUND(co_comparison_count * 100.0 / total_comparisons.total, 2) as comparison_percentage
    FROM (
             SELECT
                 CAST(other_car.car_id AS UNSIGNED) as other_car_id,
                 COUNT(*) as co_comparison_count
             FROM (
                      SELECT
                          cc.id,
                          TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(cc.car_ids, ',', numbers.n), ',', -1)) as car_id
                      FROM car_comparisons cc
                               CROSS JOIN (
                          SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3
                          UNION ALL SELECT 4 UNION ALL SELECT 5
                      ) numbers
                      WHERE CHAR_LENGTH(cc.car_ids) - CHAR_LENGTH(REPLACE(cc.car_ids, ',', '')) >= numbers.n - 1
                        AND cc.car_ids LIKE CONCAT('%', target_car_id, '%')
                        AND cc.created_at >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
                  ) target_car
                      JOIN (
                 SELECT
                     cc.id,
                     TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(cc.car_ids, ',', numbers.n), ',', -1)) as car_id
                 FROM car_comparisons cc
                          CROSS JOIN (
                     SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3
                     UNION ALL SELECT 4 UNION ALL SELECT 5
                 ) numbers
                 WHERE CHAR_LENGTH(cc.car_ids) - CHAR_LENGTH(REPLACE(cc.car_ids, ',', '')) >= numbers.n - 1
             ) other_car ON target_car.id = other_car.id
             WHERE target_car.car_id = target_car_id
               AND other_car.car_id != target_car_id
               AND other_car.car_id REGEXP '^[0-9]+$'
             GROUP BY other_car.car_id
             ORDER BY co_comparison_count DESC
             LIMIT limit_count
         ) freq_compared
             CROSS JOIN (
        SELECT COUNT(*) as total
        FROM car_comparisons
        WHERE car_ids LIKE CONCAT('%', target_car_id, '%')
          AND created_at >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
    ) total_comparisons
             LEFT JOIN cars c ON freq_compared.other_car_id = c.id
    WHERE c.id IS NOT NULL
    ORDER BY co_comparison_count DESC;
END //

-- 비교 트렌드 분석
CREATE PROCEDURE GetComparisonTrends(
    IN days_back INT DEFAULT 30
)
BEGIN
    SELECT
        comparison_date,
        daily_comparisons,
        unique_users,
        avg_cars_per_comparison,
        save_rate_percent,
        LAG(daily_comparisons) OVER (ORDER BY comparison_date) as prev_day_comparisons,
        ROUND(
                CASE
                    WHEN LAG(daily_comparisons) OVER (ORDER BY comparison_date) > 0 THEN
                        (daily_comparisons - LAG(daily_comparisons) OVER (ORDER BY comparison_date)) * 100.0 /
                        LAG(daily_comparisons) OVER (ORDER BY comparison_date)
                    ELSE 0
                    END, 2
        ) as daily_growth_percent
    FROM comparison_statistics
    WHERE comparison_date >= DATE_SUB(CURDATE(), INTERVAL days_back DAY)
    ORDER BY comparison_date DESC;
END //

DELIMITER ;

-- ========================================
-- 🆕 추가 최적화 인덱스
-- ========================================

-- 차량 테이블 추가 인덱스
CREATE INDEX idx_cars_price_year ON cars(price, year);
CREATE INDEX idx_cars_model_price ON cars(model, price);

-- 비교 테이블 추가 인덱스
CREATE INDEX idx_comparisons_user_created ON car_comparisons(user_id, created_at DESC);

-- 행동 테이블 추가 인덱스
CREATE INDEX idx_behaviors_action_timestamp ON user_behaviors(action_type, timestamp);

-- ========================================
-- 🆕 데이터 정리 및 유지보수
-- ========================================

-- 90일 이상 된 익명 사용자 비교 기록 정리 이벤트 (매주 실행)
SET GLOBAL event_scheduler = ON;

DELIMITER //
CREATE EVENT IF NOT EXISTS cleanup_old_anonymous_comparisons
    ON SCHEDULE EVERY 1 WEEK
        STARTS CURRENT_TIMESTAMP
    DO
    BEGIN
        DELETE FROM car_comparisons
        WHERE user_id IS NULL
          AND created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

        -- 오래된 행동 데이터도 정리 (6개월 이상)
        DELETE FROM user_behaviors
        WHERE timestamp < DATE_SUB(NOW(), INTERVAL 180 DAY);
    END //
DELIMITER ;

-- ========================================
-- 초기 설정 및 테스트 데이터
-- ========================================

-- 기본 관리자 사용자 생성 (비밀번호는 실제 사용시 변경 필요)
-- INSERT INTO users (username, password, role) VALUES
-- ('admin', '$2a$10$example_encrypted_password', 'ADMIN');

-- 테스트용 차량 데이터 (실제 운영시 제거)
/*
INSERT INTO cars (car_type, model, year, mileage, price, fuel, region, image_url) VALUES
('국산차', '현대 아반떼', '2022년식', 25000, 2500, '가솔린', '서울', '/images/avante.jpg'),
('국산차', '기아 K5', '2021년식', 35000, 3200, '가솔린', '경기', '/images/k5.jpg'),
('수입차', 'BMW 3시리즈', '2020년식', 45000, 4500, '가솔린', '서울', '/images/bmw3.jpg');
*/

-- ========================================
-- 성능 모니터링 쿼리 (참고용)
-- ========================================

-- 테이블 크기 확인
/*
SELECT
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'DB Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'autofinder'
ORDER BY (data_length + index_length) DESC;
*/

-- 인덱스 사용률 확인
/*
SELECT
    t.table_name,
    s.index_name,
    s.cardinality,
    s.sub_part,
    s.nullable
FROM information_schema.tables t
LEFT JOIN information_schema.statistics s ON t.table_name = s.table_name
WHERE t.table_schema = 'autofinder'
AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name, s.seq_in_index;
*/