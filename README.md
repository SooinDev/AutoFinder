# AutoFinder 🚗

> **AI 기반 개인화 중고차 추천 플랫폼**  
> 사용자의 취향을 학습하여 맞춤형 차량을 추천하는 스마트 중고차 거래 플랫폼

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.0+-blue)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-orange)](https://www.mysql.com/)
[![Python](https://img.shields.io/badge/Python-3.8+-yellow)](https://www.python.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.0+-06B6D4)](https://tailwindcss.com/)

## 🌟 주요 특징

### 🤖 AI 맞춤 추천 시스템
- **개인화된 차량 추천**: 사용자의 즐겨찾기 패턴을 분석하여 취향에 맞는 차량 추천
- **선호도 분석**: 가격대, 브랜드, 연료타입, 지역 등 다차원 선호도 분석
- **실시간 학습**: 사용자 행동 데이터를 통한 지속적인 추천 정확도 향상

### 📊 실시간 시장 분석
- **가격 트렌드 분석**: 차종별, 연식별 시장 가격 추이 시각화
- **데이터 기반 의사결정**: 구매 전 시장 동향 파악 지원
- **이상치 필터링**: 비정상적인 가격 데이터 자동 제거

### 🎨 현대적 사용자 경험
- **다크/라이트 모드**: 사용자 환경에 맞는 테마 자동 전환
- **반응형 디자인**: 모바일, 태블릿, 데스크톱 완벽 지원
- **직관적 UI/UX**: Tailwind CSS 기반 세련된 인터페이스

### ⚡ 실시간 데이터 수집
- **자동화된 크롤링**: Selenium 기반 엔카 데이터 실시간 수집
- **데이터 정제**: 중복 제거 및 표준화된 데이터 관리
- **스케줄링**: 매일 새벽 자동 데이터 업데이트

## 🛠 기술 스택

### Backend
- **Spring Boot 3.0+**: RESTful API 서버
- **Spring Security + JWT**: 인증/인가 시스템
- **Spring Data JPA**: 데이터 접근 계층
- **MySQL 8.0+**: 관계형 데이터베이스

### Frontend
- **React 18**: 사용자 인터페이스
- **React Router**: 클라이언트 사이드 라우팅
- **Tailwind CSS**: 유틸리티 우선 CSS 프레임워크
- **Recharts**: 데이터 시각화

### AI/ML
- **Python Flask**: AI 추천 API 서버
- **scikit-learn**: 머신러닝 알고리즘
- **TF-IDF + Cosine Similarity**: 차량 유사도 계산

### Data Collection
- **Selenium**: 웹 크롤링
- **PyMySQL**: Python-MySQL 연동
- **ChromeDriver**: 브라우저 자동화

## 🚀 시작하기

### 사전 요구사항
```bash
- Java 17+
- Node.js 16+
- Python 3.8+
- MySQL 8.0+
- Chrome Browser
```

### 1. 백엔드 설정
```bash
# 프로젝트 클론
git clone https://github.com/your-username/autofinder.git
cd autofinder

# MySQL 데이터베이스 생성
mysql -u root -p
CREATE DATABASE autofinder CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# application.properties 설정
spring.datasource.url=jdbc:mysql://localhost:3306/autofinder
spring.datasource.username=your_username
spring.datasource.password=your_password

# Spring Boot 실행
./gradlew bootRun
```

### 2. 프론트엔드 설정
```bash
cd frontend
npm install
npm start
```

### 3. AI 서비스 설정
```bash
cd ai-service
pip install -r requirements.txt
python app.py
```

### 4. 크롤링 서비스 설정
```bash
cd crawler
pip install selenium pymysql chromedriver-autoinstaller
python crawler.py
```

## 📁 프로젝트 구조

```
autofinder/
├── src/main/java/               # Spring Boot 백엔드
│   ├── controller/              # REST API 컨트롤러
│   ├── service/                 # 비즈니스 로직
│   ├── repository/              # 데이터 접근 계층
│   ├── model/                   # JPA 엔티티
│   ├── config/                  # 설정 파일
│   └── security/                # 보안 설정
├── frontend/                    # React 프론트엔드
│   ├── src/
│   │   ├── components/          # React 컴포넌트
│   │   ├── pages/               # 페이지 컴포넌트
│   │   ├── api/                 # API 호출 로직
│   │   ├── context/             # React Context
│   │   └── styles/              # CSS 스타일
├── ai-service/                  # Python AI 서비스
│   ├── app.py                   # Flask 애플리케이션
│   └── requirements.txt         # Python 의존성
├── crawler/                     # 데이터 크롤링
│   ├── crawler.py               # 크롤링 스크립트
│   └── requirements.txt         # Python 의존성
└── docs/                        # 프로젝트 문서
```

## 🔧 주요 기능

### 🔍 차량 검색 및 필터링
- 모델명, 가격대, 연식, 주행거리, 연료타입, 지역별 검색
- 실시간 검색 결과 및 페이지네이션
- URL 기반 검색 상태 저장

### ❤️ 즐겨찾기 시스템
- 관심 차량 저장 및 관리
- 즐겨찾기 기반 AI 추천 학습
- 사용자별 맞춤 대시보드

### 📈 시장 분석 도구
- 차종별 연식-가격 상관관계 분석
- 인터랙티브 차트 및 테이블
- 시장 트렌드 시각화

### 🔐 사용자 인증
- JWT 기반 안전한 인증
- 로그인 유지 기능
- 역할 기반 접근 제어

## 🎯 AI 추천 알고리즘

### 1. 특성 추출 (Feature Extraction)
```python
def extract_features(cars_data):
    # 텍스트 특성: TF-IDF (모델명, 연료타입, 지역)
    text_features = tfidf_vectorizer.fit_transform(df['text_features'])
    
    # 수치 특성: 정규화 (연식, 가격, 주행거리)
    numeric_features = scaler.fit_transform(df[['year', 'price', 'mileage']])
    
    return np.hstack([text_features.toarray(), numeric_features])
```

### 2. 사용자 선호도 벡터 계산
```python
def calculate_user_preference_vector(favorite_car_ids):
    favorite_features = car_features_matrix[favorite_indices]
    return np.mean(favorite_features, axis=0)  # 평균 벡터
```

### 3. 유사도 기반 추천
```python
def recommend_cars(user_preference_vector, top_k=10):
    similarities = cosine_similarity(
        user_preference_vector.reshape(1, -1), 
        car_features_matrix
    )[0]
    return top_k_recommendations
```

## 📊 데이터베이스 스키마

### 주요 테이블
```sql
-- 차량 정보
CREATE TABLE cars (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model VARCHAR(100) NOT NULL,
    year VARCHAR(20) NOT NULL,
    price BIGINT NOT NULL,
    mileage BIGINT,
    fuel VARCHAR(20) NOT NULL,
    region VARCHAR(50) NOT NULL,
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 정보
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- 즐겨찾기
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (car_id) REFERENCES cars(id)
);
```

## 🌐 API 문서

### 인증 API
```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### 차량 API
```http
GET    /api/cars                 # 차량 목록 조회 (필터링 지원)
GET    /api/cars/{id}            # 차량 상세 조회
GET    /api/cars/{id}/similar    # 유사 차량 조회
```

### 즐겨찾기 API
```http
GET    /api/favorites            # 즐겨찾기 목록
POST   /api/favorites/{carId}    # 즐겨찾기 추가
DELETE /api/favorites/{carId}    # 즐겨찾기 제거
```

### AI 추천 API
```http
GET /api/ai/recommend            # AI 맞춤 추천
GET /api/ai/analysis            # 사용자 선호도 분석
GET /api/ai/status              # AI 서비스 상태
```

### 분석 API
```http
GET /api/analytics/price-by-year/{model}  # 모델별 가격 분석
```

## 🔍 사용 예시

### 1. 회원가입 및 로그인
```javascript
// 회원가입
const response = await register({
    username: "user123",
    password: "password123"
});

// 로그인
const loginData = await login({
    username: "user123", 
    password: "password123",
    rememberMe: true
});
```

### 2. 차량 검색
```javascript
// 조건별 차량 검색
const cars = await fetchCars({
    model: "아반떼",
    minPrice: 1000,
    maxPrice: 3000,
    fuel: "가솔린",
    region: "서울"
}, page, size);
```

### 3. AI 추천 받기
```javascript
// 맞춤 추천 조회
const recommendations = await fetchAIRecommendations(10);

// 추천 결과 예시
{
    "recommendations": [
        {
            "car": { "id": 123, "model": "아반떼", ... },
            "similarity_score": 0.85,
            "recommendation_reason": "선호 패턴과 매우 유사 • 경제적인 가격"
        }
    ]
}
```

### 환경별 설정
```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DATABASE_URL}
  jpa:
    hibernate:
      ddl-auto: validate
```

## 🧪 테스트

### 백엔드 테스트
```bash
./gradlew test
./gradlew jacocoTestReport  # 코드 커버리지
```

### 프론트엔드 테스트
```bash
npm test
npm run test:coverage
```

## 👥 개발자

**SooinDev** - [@SooinDev](https://github.com/SooinDev) - alwayswithsound@gmail.com

프로젝트 링크: [https://github.com/SooinDev/AutoFinder](https://github.com/SooinDev/AutoFinder)

## 🙏 감사의 말

- [Spring Boot](https://spring.io/projects/spring-boot) - 백엔드 프레임워크
- [React](https://reactjs.org/) - 프론트엔드 라이브러리  
- [Tailwind CSS](https://tailwindcss.com/) - CSS 프레임워크
- [Recharts](https://recharts.org/) - 차트 라이브러리
- [Encar](https://www.encar.com/) - 데이터 소스

---

<div align="center">

**⭐ 이 프로젝트가 도움이 되셨다면 Star를 눌러주세요! ⭐**

</div>
