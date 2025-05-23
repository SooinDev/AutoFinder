CREATE TABLE cars (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 차량 ID (자동 증가)
                      car_type VARCHAR(50) NOT NULL,         -- 차량 종류 (국산차/수입차)
                      model VARCHAR(100) NOT NULL,           -- 차량 모델명
                      year VARCHAR(20) NOT NULL,             -- 연식 (예: "22/01식")
                      mileage VARCHAR(20) DEFAULT '정보 없음', -- 주행거리 (없으면 "정보 없음" 저장)
                      price BIGINT UNSIGNED NOT NULL DEFAULT 0,  -- 차량 가격 (기본값 0)
                      fuel VARCHAR(20) NOT NULL,             -- 연료 종류 (예: 가솔린, 디젤)
                      region VARCHAR(50) NOT NULL,           -- 지역 (예: 서울, 경기 등)
                      url VARCHAR(255) UNIQUE,               -- 차량 상세 페이지 URL (중복 방지)
                      image_url VARCHAR(255),                -- 차량 사진 URL 추가
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 데이터 삽입 시간 기록
);

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL
);

CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           car_id BIGINT NOT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE
);

TRUNCATE TABLE users;