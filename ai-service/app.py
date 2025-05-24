from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
import logging
from datetime import datetime
import pickle
import os
import traceback

app = Flask(__name__)
app.config['JSON_ENSURE_ASCII'] = False

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SimpleCarRecommendationSystem:
    def __init__(self):
        self.cars_df = None
        self.favorites_df = None
        self.user_behaviors = {}
        self.model_trained = False

        # 모델 저장 디렉토리 생성
        os.makedirs('./models', exist_ok=True)

    def train_model(self, cars_data, favorites_data, user_behaviors=None):
        """간단한 추천 모델 학습"""
        try:
            logger.info(f"모델 학습 시작: 차량 {len(cars_data)}개, 즐겨찾기 {len(favorites_data)}개")

            # 데이터 검증
            if not cars_data:
                raise ValueError("차량 데이터가 없습니다.")

            if not favorites_data:
                logger.warning("즐겨찾기 데이터가 없습니다. 기본 모델로 학습합니다.")
                favorites_data = []

            # 데이터프레임 생성
            self.cars_df = pd.DataFrame(cars_data)

            if favorites_data:
                self.favorites_df = pd.DataFrame(favorites_data)
            else:
                # 빈 즐겨찾기 데이터프레임 생성
                self.favorites_df = pd.DataFrame(columns=['user_id', 'car_id'])

            # 사용자 행동 데이터 저장
            self.user_behaviors = user_behaviors if user_behaviors else {}

            # 기본 데이터 전처리
            self._preprocess_data()

            # 모델 학습 완료 표시
            self.model_trained = True

            logger.info("모델 학습 완료")
            return True

        except Exception as e:
            logger.error(f"모델 학습 중 오류: {str(e)}")
            logger.error(traceback.format_exc())
            return False

    def _preprocess_data(self):
        """데이터 전처리"""
        try:
            # 차량 데이터 전처리
            if 'price' in self.cars_df.columns:
                self.cars_df['price'] = pd.to_numeric(self.cars_df['price'], errors='coerce')

            if 'year' in self.cars_df.columns:
                self.cars_df['processed_year'] = self.cars_df['year'].apply(self._extract_year)

            if 'model' in self.cars_df.columns:
                self.cars_df['brand'] = self.cars_df['model'].apply(self._extract_brand)

            logger.info("데이터 전처리 완료")

        except Exception as e:
            logger.error(f"데이터 전처리 중 오류: {str(e)}")

    def _extract_year(self, year_str):
        """연도 추출"""
        if pd.isna(year_str) or year_str == '':
            return 2020

        try:
            # 숫자만 추출
            digits = ''.join(filter(str.isdigit, str(year_str)))
            if not digits:
                return 2020

            if len(digits) >= 4:
                return int(digits[:4])
            elif len(digits) == 2:
                year = int(digits)
                return 2000 + year if year <= 25 else 1900 + year
            else:
                return 2020

        except:
            return 2020

    def _extract_brand(self, model_str):
        """브랜드 추출"""
        if pd.isna(model_str) or model_str == '':
            return "기타"

        brands = ["현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디", "볼보"]
        for brand in brands:
            if brand in str(model_str):
                return brand

        # 첫 번째 단어를 브랜드로 사용
        words = str(model_str).split()
        return words[0] if words else "기타"

    def get_recommendations(self, user_id=None, favorite_car_ids=None, candidate_cars=None, exclude_ids=None, top_k=10):
        """추천 생성"""
        try:
            if not self.model_trained or self.cars_df is None:
                return self._get_fallback_recommendations(top_k)

            # 제외할 차량 ID 리스트
            if exclude_ids is None:
                exclude_ids = []

            # 후보 차량이 제공된 경우 사용
            if candidate_cars:
                recommendations = self._recommend_from_candidates(
                    user_id, favorite_car_ids, candidate_cars, exclude_ids, top_k
                )
            else:
                # 즐겨찾기 기반 추천
                recommendations = self._recommend_from_favorites(
                    favorite_car_ids, exclude_ids, top_k
                )

            return recommendations

        except Exception as e:
            logger.error(f"추천 생성 중 오류: {str(e)}")
            logger.error(traceback.format_exc())
            return self._get_fallback_recommendations(top_k)

    def _recommend_from_candidates(self, user_id, favorite_car_ids, candidate_cars, exclude_ids, top_k):
        """후보 차량에서 추천 생성"""
        try:
            candidate_df = pd.DataFrame(candidate_cars)

            # 제외할 차량 필터링
            if exclude_ids:
                candidate_df = candidate_df[~candidate_df['id'].isin(exclude_ids)]

            # 즐겨찾기가 있는 경우 유사도 기반 정렬
            if favorite_car_ids and len(favorite_car_ids) > 0:
                candidate_df['similarity_score'] = candidate_df.apply(
                    lambda row: self._calculate_similarity(row, favorite_car_ids), axis=1
                )
                candidate_df = candidate_df.sort_values('similarity_score', ascending=False)
            else:
                # 즐겨찾기가 없는 경우 인기도/가격 기준 정렬
                candidate_df['similarity_score'] = 0.5
                if 'price' in candidate_df.columns:
                    candidate_df = candidate_df.sort_values('price')

            # 추천 결과 생성
            recommendations = []
            for _, row in candidate_df.head(top_k).iterrows():
                recommendations.append({
                    'car': row.to_dict(),
                    'similarity_score': row.get('similarity_score', 0.5),
                    'recommendation_reason': self._get_recommendation_reason(row.get('similarity_score', 0.5))
                })

            return recommendations

        except Exception as e:
            logger.error(f"후보 차량 추천 중 오류: {str(e)}")
            return self._get_fallback_recommendations(top_k)

    def _recommend_from_favorites(self, favorite_car_ids, exclude_ids, top_k):
        """즐겨찾기 기반 추천"""
        try:
            if not favorite_car_ids:
                return self._get_popular_recommendations(top_k)

            # 즐겨찾기한 차량들의 특성 분석
            favorite_cars = self.cars_df[self.cars_df['id'].isin(favorite_car_ids)]

            if favorite_cars.empty:
                return self._get_popular_recommendations(top_k)

            # 평균 가격 계산
            avg_price = favorite_cars['price'].mean() if 'price' in favorite_cars.columns else None

            # 선호 브랜드 찾기
            preferred_brands = favorite_cars['brand'].value_counts().index.tolist() if 'brand' in favorite_cars.columns else []

            # 추천 후보 생성
            candidates = self.cars_df[~self.cars_df['id'].isin(exclude_ids + favorite_car_ids)].copy()

            if candidates.empty:
                return self._get_fallback_recommendations(top_k)

            # 유사도 점수 계산
            candidates['similarity_score'] = candidates.apply(
                lambda row: self._calculate_row_similarity(row, avg_price, preferred_brands), axis=1
            )

            # 상위 추천 선택
            top_candidates = candidates.nlargest(top_k, 'similarity_score')

            recommendations = []
            for _, row in top_candidates.iterrows():
                recommendations.append({
                    'car': {
                        'id': int(row['id']),
                        'model': row.get('model', ''),
                        'year': row.get('year', ''),
                        'price': int(row.get('price', 0)) if pd.notna(row.get('price', 0)) else 0,
                        'fuel': row.get('fuel', ''),
                        'region': row.get('region', ''),
                        'brand': row.get('brand', ''),
                        'carType': row.get('carType', '')
                    },
                    'similarity_score': float(row['similarity_score']),
                    'recommendation_reason': self._get_recommendation_reason(row['similarity_score'])
                })

            return recommendations

        except Exception as e:
            logger.error(f"즐겨찾기 기반 추천 중 오류: {str(e)}")
            return self._get_fallback_recommendations(top_k)

    def _calculate_similarity(self, candidate_row, favorite_car_ids):
        """유사도 계산"""
        try:
            favorite_cars = self.cars_df[self.cars_df['id'].isin(favorite_car_ids)]

            if favorite_cars.empty:
                return 0.5

            # 평균 가격 계산
            avg_price = favorite_cars['price'].mean() if 'price' in favorite_cars.columns else 0

            # 선호 브랜드
            preferred_brands = favorite_cars['brand'].value_counts().index.tolist() if 'brand' in favorite_cars.columns else []

            return self._calculate_row_similarity(candidate_row, avg_price, preferred_brands)

        except:
            return 0.5

    def _calculate_row_similarity(self, row, avg_price, preferred_brands):
        """개별 행의 유사도 계산"""
        score = 0.0

        try:
            # 가격 유사도 (50% 가중치)
            if avg_price and pd.notna(row.get('price', 0)) and row.get('price', 0) > 0:
                price_diff = abs(row['price'] - avg_price) / avg_price
                price_score = max(0, 1 - price_diff)
                score += price_score * 0.5
            else:
                score += 0.25  # 기본값

            # 브랜드 유사도 (30% 가중치)
            if preferred_brands and row.get('brand', '') in preferred_brands:
                score += 0.3

            # 기본 점수 (20% 가중치)
            score += 0.2

            return min(score, 1.0)

        except:
            return 0.5

    def _get_recommendation_reason(self, score):
        """추천 이유 생성"""
        if score > 0.8:
            return "즐겨찾기 패턴과 매우 유사"
        elif score > 0.6:
            return "선호도 분석 기반 추천"
        elif score > 0.4:
            return "유사한 특성의 차량"
        else:
            return "다양성을 위한 추천"

    def _get_popular_recommendations(self, top_k):
        """인기 차량 추천"""
        try:
            if self.cars_df is None or self.cars_df.empty:
                return self._get_fallback_recommendations(top_k)

            # 가격 기준으로 정렬 (중간 가격대 우선)
            popular_cars = self.cars_df.copy()

            if 'price' in popular_cars.columns:
                # 가격이 있는 차량만 필터링
                popular_cars = popular_cars[popular_cars['price'].notna() & (popular_cars['price'] > 0)]
                popular_cars = popular_cars.sort_values('price')

            recommendations = []
            for _, row in popular_cars.head(top_k).iterrows():
                recommendations.append({
                    'car': {
                        'id': int(row['id']),
                        'model': row.get('model', ''),
                        'year': row.get('year', ''),
                        'price': int(row.get('price', 0)) if pd.notna(row.get('price', 0)) else 0,
                        'fuel': row.get('fuel', ''),
                        'region': row.get('region', ''),
                        'brand': row.get('brand', ''),
                        'carType': row.get('carType', '')
                    },
                    'similarity_score': 0.5,
                    'recommendation_reason': "인기 차량 추천"
                })

            return recommendations

        except Exception as e:
            logger.error(f"인기 차량 추천 중 오류: {str(e)}")
            return self._get_fallback_recommendations(top_k)

    def _get_fallback_recommendations(self, top_k):
        """기본 더미 추천"""
        recommendations = []
        for i in range(min(top_k, 5)):
            recommendations.append({
                'car': {
                    'id': i + 1000,  # 더미 ID
                    'model': f'추천 차량 {i + 1}',
                    'year': '2023',
                    'price': 2000 + i * 200,
                    'fuel': '가솔린',
                    'region': '서울',
                    'brand': '현대',
                    'carType': '중형차'
                },
                'similarity_score': 0.5,
                'recommendation_reason': '기본 추천'
            })

        return recommendations

# 전역 AI 시스템 인스턴스
ai_system = SimpleCarRecommendationSystem()

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "model_type": "simple_recommendation",
        "model_trained": ai_system.model_trained,
        "timestamp": datetime.now().isoformat()
    })

@app.route('/train', methods=['POST'])
def train_model():
    try:
        data = request.get_json(force=True)

        if not data:
            return jsonify({"error": "요청 데이터가 없습니다."}), 400

        cars_data = data.get('cars', [])
        favorites_data = data.get('favorites', [])
        user_behaviors = data.get('user_behaviors', {})

        logger.info(f"학습 요청 받음: 차량 {len(cars_data)}개, 즐겨찾기 {len(favorites_data)}개")

        if not cars_data:
            return jsonify({"error": "차량 데이터가 필요합니다."}), 400

        success = ai_system.train_model(cars_data, favorites_data, user_behaviors)

        if success:
            return jsonify({
                "message": "모델 학습 완료",
                "model_type": "simple_recommendation",
                "cars_count": len(cars_data),
                "favorites_count": len(favorites_data),
                "timestamp": datetime.now().isoformat()
            })
        else:
            return jsonify({"error": "모델 학습 실패"}), 500

    except Exception as e:
        logger.error(f"학습 중 오류: {str(e)}")
        logger.error(traceback.format_exc())
        return jsonify({"error": f"학습 중 오류: {str(e)}"}), 500

@app.route('/recommend', methods=['POST'])
def recommend():
    try:
        data = request.get_json(force=True)

        if not data:
            data = {}

        user_id = data.get('user_id')
        favorite_car_ids = data.get('favorite_car_ids', [])
        candidate_cars = data.get('candidate_cars', [])
        exclude_ids = data.get('exclude_ids', [])
        top_k = data.get('top_k', 10)

        logger.info(f"추천 요청: 사용자 {user_id}, 즐겨찾기 {len(favorite_car_ids)}개, 후보 {len(candidate_cars)}개")

        recommendations = ai_system.get_recommendations(
            user_id=user_id,
            favorite_car_ids=favorite_car_ids,
            candidate_cars=candidate_cars,
            exclude_ids=exclude_ids,
            top_k=top_k
        )

        return jsonify({
            "recommendations": recommendations,
            "total_count": len(recommendations),
            "model_type": "simple_recommendation",
            "timestamp": datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"추천 생성 중 오류: {str(e)}")
        logger.error(traceback.format_exc())

        # 에러가 발생해도 빈 결과 반환
        return jsonify({
            "recommendations": [],
            "total_count": 0,
            "model_type": "error_fallback",
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 200

if __name__ == '__main__':
    logger.info("AI 추천 서버 시작 중...")
    app.run(host='0.0.0.0', port=5001, debug=True)