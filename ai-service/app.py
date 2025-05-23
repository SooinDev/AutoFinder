from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.decomposition import PCA
import re
import logging
from datetime import datetime
import pickle
import os

app = Flask(__name__)
app.config['JSON_ENSURE_ASCII'] = False

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ImprovedCarRecommendationSystem:
    def __init__(self):
        self.tfidf_vectorizer = TfidfVectorizer(
            max_features=2000,
            stop_words=None,
            ngram_range=(1, 2),  # 1-gram과 2-gram 사용
            min_df=2,  # 최소 2회 이상 등장하는 단어만 사용
            max_df=0.8  # 80% 이상 문서에 등장하는 단어 제외
        )
        self.price_scaler = StandardScaler()
        self.mileage_scaler = StandardScaler()
        self.year_scaler = StandardScaler()
        self.pca = PCA(n_components=50)  # 차원 축소

        # 가중치 설정 (조정 가능)
        self.weights = {
            'text_similarity': 0.4,
            'price_similarity': 0.25,
            'year_similarity': 0.2,
            'mileage_similarity': 0.15
        }

        self.car_features_matrix = None
        self.cars_df = None
        self.popularity_scores = None
        self.price_predictor = None

    def preprocess_year(self, year_str):
        """연식 전처리 개선"""
        if not year_str or year_str == "정보 없음":
            return 0

        # 다양한 연식 표현 처리
        patterns = [
            r'(\d{4})년?식?',  # 2023년식, 2023식
            r'(\d{2})/(\d{2})',  # 23/01
            r'(\d{2})년?식?',   # 23년식
        ]

        for pattern in patterns:
            match = re.search(pattern, str(year_str))
            if match:
                if len(match.groups()) == 2:  # 23/01 형태
                    year = int(match.group(1))
                    return 2000 + year if year <= 30 else 1900 + year
                else:  # 단일 숫자
                    year = int(match.group(1))
                    if year >= 1900:  # 4자리 연도
                        return year
                    else:  # 2자리 연도
                        return 2000 + year if year <= 30 else 1900 + year
        return 0

    def preprocess_price(self, price):
        """가격 전처리 개선"""
        if pd.isna(price) or price == 9999:
            return np.nan
        return float(price)

    def preprocess_mileage(self, mileage_str):
        """주행거리 전처리 개선"""
        if not mileage_str or mileage_str == "정보 없음":
            return 0

        # 숫자만 추출
        numbers = re.findall(r'\d+', str(mileage_str))
        if numbers:
            mileage = int(''.join(numbers))
            # km 단위로 통일 (만km인 경우 처리)
            if mileage < 1000:  # 만km 단위로 입력된 경우
                mileage *= 10000
            return mileage
        return 0

    def calculate_popularity_scores(self):
        """차량별 인기도 점수 계산"""
        if self.cars_df is None:
            return

        # 모델별 차량 수 기반 인기도
        model_counts = self.cars_df.groupby('normalized_model').size()
        model_popularity = model_counts / model_counts.max()

        # 가격대별 인기도 (중간 가격대가 더 인기)
        price_bins = pd.qcut(self.cars_df['price'].dropna(), q=5, labels=False, duplicates='drop')
        price_popularity = pd.Series(index=self.cars_df.index, dtype=float)

        for idx, price in enumerate(self.cars_df['price']):
            if pd.notna(price):
                bin_idx = pd.cut([price], bins=pd.qcut(self.cars_df['price'].dropna(), q=5).cat.categories, labels=False)[0]
                # 중간 가격대에 높은 점수
                if bin_idx == 2:  # 중간 구간
                    price_popularity.iloc[idx] = 1.0
                elif bin_idx in [1, 3]:
                    price_popularity.iloc[idx] = 0.8
                else:
                    price_popularity.iloc[idx] = 0.6
            else:
                price_popularity.iloc[idx] = 0.5

        # 종합 인기도 점수
        self.popularity_scores = pd.DataFrame({
            'model_popularity': self.cars_df['normalized_model'].map(model_popularity),
            'price_popularity': price_popularity
        }).fillna(0.5).mean(axis=1)

    def normalize_model_name(self, model):
        """모델명 정규화"""
        if not model:
            return ""

        # 브랜드와 모델명 분리
        brands = ['현대', '기아', '제네시스', '르노', '쉐보레', '쌍용', 'BMW', '벤츠', '아우디', '폭스바겐', '볼보', '토요타', '혼다', '닛산']
        models = ['아반떼', '쏘나타', '그랜저', 'K3', 'K5', 'K7', 'K8', 'K9', 'SM3', 'SM5', 'SM6', 'SM7', '말리부', '스파크', '모닝']

        model_lower = model.lower()

        # 모델명 추출
        for m in models:
            if m.lower() in model_lower:
                return m

        # 브랜드 제거 후 첫 번째 단어 추출
        for brand in brands:
            model = model.replace(brand, '').strip()

        words = model.split()
        return words[0] if words else model

    def extract_features(self, cars_data):
        """특성 추출 개선"""
        df = pd.DataFrame(cars_data)

        # 기본 전처리
        df['processed_year'] = df['year'].apply(self.preprocess_year)
        df['price'] = df['price'].apply(self.preprocess_price)
        df['processed_mileage'] = df['mileage'].apply(self.preprocess_mileage)
        df['normalized_model'] = df['model'].apply(self.normalize_model_name)

        # 이상치 제거
        if not df['price'].isna().all():
            price_q1 = df['price'].quantile(0.05)
            price_q3 = df['price'].quantile(0.95)
            df = df[(df['price'] >= price_q1) & (df['price'] <= price_q3)]

        # 파생 특성 생성
        df['car_age'] = 2024 - df['processed_year']
        df['price_per_year'] = df['price'] / (df['car_age'] + 1)
        df['mileage_per_year'] = df['processed_mileage'] / (df['car_age'] + 1)

        # 텍스트 특성 개선
        df['text_features'] = (
                df['normalized_model'].fillna('') + ' ' +
                df['fuel'].fillna('') + ' ' +
                df['region'].fillna('') + ' ' +
                df['carType'].fillna('')
        )

        return df

    def build_feature_matrix(self, cars_data):
        """특성 매트릭스 구축 개선"""
        self.cars_df = self.extract_features(cars_data)

        if self.cars_df.empty:
            logger.warning("No valid car data found")
            return

        # 인기도 점수 계산
        self.calculate_popularity_scores()

        # 텍스트 특성
        text_features = self.tfidf_vectorizer.fit_transform(self.cars_df['text_features'])

        # 수치 특성 스케일링
        numeric_columns = ['processed_year', 'price', 'processed_mileage', 'car_age', 'price_per_year', 'mileage_per_year']
        numeric_data = self.cars_df[numeric_columns].fillna(self.cars_df[numeric_columns].median())

        # 각 특성별로 개별 스케일링
        scaled_year = self.year_scaler.fit_transform(numeric_data[['processed_year', 'car_age']])
        scaled_price = self.price_scaler.fit_transform(numeric_data[['price', 'price_per_year']])
        scaled_mileage = self.mileage_scaler.fit_transform(numeric_data[['processed_mileage', 'mileage_per_year']])

        # 특성 결합
        all_features = np.hstack([
            text_features.toarray(),
            scaled_year,
            scaled_price,
            scaled_mileage,
            self.popularity_scores.values.reshape(-1, 1)
        ])

        # 차원 축소 (선택사항)
        if all_features.shape[1] > 100:
            self.car_features_matrix = self.pca.fit_transform(all_features)
        else:
            self.car_features_matrix = all_features

        logger.info(f"Feature matrix built with shape: {self.car_features_matrix.shape}")

    def calculate_advanced_similarity(self, user_preference_vector, car_vectors):
        """고급 유사도 계산"""
        # 코사인 유사도
        cosine_sim = cosine_similarity(user_preference_vector.reshape(1, -1), car_vectors)[0]

        # 유클리디안 거리 기반 유사도
        distances = np.linalg.norm(car_vectors - user_preference_vector, axis=1)
        euclidean_sim = 1 / (1 + distances)  # 거리를 유사도로 변환

        # 맨하탄 거리 기반 유사도
        manhattan_distances = np.sum(np.abs(car_vectors - user_preference_vector), axis=1)
        manhattan_sim = 1 / (1 + manhattan_distances)

        # 가중 평균
        combined_similarity = (
                0.5 * cosine_sim +
                0.3 * euclidean_sim +
                0.2 * manhattan_sim
        )

        return combined_similarity

    def calculate_user_preference_vector(self, favorite_car_ids):
        """사용자 선호도 벡터 계산 개선"""
        if not favorite_car_ids or self.car_features_matrix is None:
            return None

        favorite_indices = []
        for car_id in favorite_car_ids:
            matching_cars = self.cars_df[self.cars_df['id'] == car_id]
            if not matching_cars.empty:
                favorite_indices.append(matching_cars.index[0])

        if not favorite_indices:
            return None

        favorite_features = self.car_features_matrix[favorite_indices]

        # 가중 평균 계산 (최근 즐겨찾기에 더 높은 가중치)
        weights = np.exp(np.arange(len(favorite_indices))) / np.sum(np.exp(np.arange(len(favorite_indices))))
        weights = weights[::-1]  # 최근 것에 더 높은 가중치

        weighted_preference = np.average(favorite_features, axis=0, weights=weights)

        return weighted_preference

    def recommend_cars(self, user_preference_vector, exclude_ids=None, top_k=10):
        """개선된 차량 추천"""
        if user_preference_vector is None or self.car_features_matrix is None:
            return []

        # 고급 유사도 계산
        similarities = self.calculate_advanced_similarity(user_preference_vector, self.car_features_matrix)

        # 인기도 점수와 결합
        final_scores = (
                0.7 * similarities +
                0.3 * self.popularity_scores.values
        )

        car_scores = list(enumerate(final_scores))

        # 제외할 차량 필터링
        if exclude_ids:
            exclude_indices = set()
            for car_id in exclude_ids:
                matching_cars = self.cars_df[self.cars_df['id'] == car_id]
                if not matching_cars.empty:
                    exclude_indices.add(matching_cars.index[0])
            car_scores = [(idx, score) for idx, score in car_scores if idx not in exclude_indices]

        # 점수 기준 정렬
        car_scores.sort(key=lambda x: x[1], reverse=True)

        recommendations = []
        for idx, score in car_scores[:top_k]:
            car_data = self.cars_df.iloc[idx].to_dict()

            # 추천 이유 생성
            reason = self._generate_detailed_reason(car_data, score, user_preference_vector, idx)

            recommendations.append({
                'car': car_data,
                'similarity_score': float(score),
                'recommendation_reason': reason
            })

        return recommendations

    def _generate_detailed_reason(self, car_data, score, user_vector, car_idx):
        """상세한 추천 이유 생성"""
        reasons = []

        # 유사도 기반 이유
        if score > 0.8:
            reasons.append("선호 패턴과 매우 높은 일치도")
        elif score > 0.6:
            reasons.append("선호 패턴과 높은 일치도")
        elif score > 0.4:
            reasons.append("선호 패턴과 적절한 일치도")
        else:
            reasons.append("새로운 옵션 제안")

        # 가격 기반 이유
        price = car_data.get('price', 0)
        if price < 1000:
            reasons.append("경제적인 가격대")
        elif price > 5000:
            reasons.append("프리미엄 차량")
        else:
            reasons.append("합리적인 가격대")

        # 연식 기반 이유
        year = car_data.get('processed_year', 0)
        current_year = datetime.now().year
        car_age = current_year - year
        if car_age <= 2:
            reasons.append("최신 연식")
        elif car_age <= 5:
            reasons.append("준신차급 연식")
        else:
            reasons.append("경제적인 연식")

        # 주행거리 기반 이유
        mileage = car_data.get('processed_mileage', 0)
        if mileage < 30000:
            reasons.append("저주행 차량")
        elif mileage < 100000:
            reasons.append("적정 주행거리")

        # 인기도 기반 이유
        if self.popularity_scores is not None:
            popularity = self.popularity_scores.iloc[car_idx]
            if popularity > 0.7:
                reasons.append("인기 모델")

        return " • ".join(reasons[:3])  # 최대 3개 이유만 표시

    def save_model(self, filepath):
        """모델 저장"""
        model_data = {
            'tfidf_vectorizer': self.tfidf_vectorizer,
            'price_scaler': self.price_scaler,
            'mileage_scaler': self.mileage_scaler,
            'year_scaler': self.year_scaler,
            'pca': self.pca,
            'car_features_matrix': self.car_features_matrix,
            'cars_df': self.cars_df,
            'popularity_scores': self.popularity_scores,
            'weights': self.weights
        }

        with open(filepath, 'wb') as f:
            pickle.dump(model_data, f)

        logger.info(f"Model saved to {filepath}")

    def load_model(self, filepath):
        """모델 로드"""
        if os.path.exists(filepath):
            with open(filepath, 'rb') as f:
                model_data = pickle.load(f)

            self.tfidf_vectorizer = model_data['tfidf_vectorizer']
            self.price_scaler = model_data['price_scaler']
            self.mileage_scaler = model_data['mileage_scaler']
            self.year_scaler = model_data['year_scaler']
            self.pca = model_data['pca']
            self.car_features_matrix = model_data['car_features_matrix']
            self.cars_df = model_data['cars_df']
            self.popularity_scores = model_data['popularity_scores']
            self.weights = model_data['weights']

            logger.info(f"Model loaded from {filepath}")
            return True
        return False

# 전역 추천 시스템 인스턴스
recommendation_system = ImprovedCarRecommendationSystem()

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "model_loaded": recommendation_system.car_features_matrix is not None
    })

@app.route('/train', methods=['POST'])
def train_model():
    try:
        data = request.get_json(force=True)
        cars_data = data.get('cars', [])

        if not cars_data:
            return jsonify({"error": "No car data provided"}), 400

        # 모델 학습
        recommendation_system.build_feature_matrix(cars_data)

        # 모델 저장
        recommendation_system.save_model('car_recommendation_model.pkl')

        return jsonify({
            "message": "Improved model trained successfully",
            "cars_count": len(cars_data),
            "feature_dimensions": recommendation_system.car_features_matrix.shape[1] if recommendation_system.car_features_matrix is not None else 0,
            "timestamp": datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Error training model: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/recommend', methods=['POST'])
def recommend():
    try:
        data = request.get_json(force=True)
        favorite_car_ids = data.get('favorite_car_ids', [])
        exclude_ids = data.get('exclude_ids', [])
        top_k = data.get('top_k', 10)

        if not favorite_car_ids:
            return jsonify({"error": "No favorite car IDs provided"}), 400

        if recommendation_system.car_features_matrix is None:
            # 저장된 모델 로드 시도
            if not recommendation_system.load_model('car_recommendation_model.pkl'):
                return jsonify({"error": "Model not trained. Please train the model first."}), 400

        user_vector = recommendation_system.calculate_user_preference_vector(favorite_car_ids)
        if user_vector is None:
            return jsonify({"error": "Could not calculate user preferences"}), 400

        recommendations = recommendation_system.recommend_cars(user_vector, exclude_ids, top_k)

        return jsonify({
            "recommendations": recommendations,
            "total_count": len(recommendations),
            "algorithm_version": "improved_v2.0",
            "timestamp": datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Error generating recommendations: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/user-analysis', methods=['POST'])
def analyze_user_preferences():
    try:
        data = request.get_json(force=True)
        favorite_car_ids = data.get('favorite_car_ids', [])

        if not favorite_car_ids:
            return jsonify({"error": "No favorite car IDs provided"}), 400

        if recommendation_system.cars_df is None:
            if not recommendation_system.load_model('car_recommendation_model.pkl'):
                return jsonify({"error": "Model not trained"}), 400

        favorite_cars = recommendation_system.cars_df[
            recommendation_system.cars_df['id'].isin(favorite_car_ids)
        ]

        if favorite_cars.empty:
            return jsonify({"error": "No favorite cars found in dataset"}), 400

        # 상세 분석
        analysis = {
            "price_preferences": {
                "avg_price": float(favorite_cars['price'].mean()),
                "min_price": float(favorite_cars['price'].min()),
                "max_price": float(favorite_cars['price'].max()),
                "price_range": f"{int(favorite_cars['price'].min())}-{int(favorite_cars['price'].max())}만원",
                "price_std": float(favorite_cars['price'].std())
            },
            "year_preferences": {
                "avg_year": float(favorite_cars['processed_year'].mean()),
                "min_year": int(favorite_cars['processed_year'].min()),
                "max_year": int(favorite_cars['processed_year'].max()),
                "preferred_car_age": float(favorite_cars['car_age'].mean())
            },
            "model_preferences": {
                "top_models": favorite_cars['normalized_model'].value_counts().head(5).to_dict(),
                "model_diversity": len(favorite_cars['normalized_model'].unique())
            },
            "fuel_preferences": favorite_cars['fuel'].value_counts().to_dict(),
            "region_preferences": favorite_cars['region'].value_counts().to_dict(),
            "mileage_preferences": {
                "avg_mileage": float(favorite_cars['processed_mileage'].mean()),
                "min_mileage": int(favorite_cars['processed_mileage'].min()),
                "max_mileage": int(favorite_cars['processed_mileage'].max()),
                "mileage_range": f"{int(favorite_cars['processed_mileage'].min()/10000)}-{int(favorite_cars['processed_mileage'].max()/10000)}만km"
            },
            "derived_insights": {
                "avg_price_per_year": float(favorite_cars['price_per_year'].mean()),
                "avg_mileage_per_year": float(favorite_cars['mileage_per_year'].mean()),
                "preference_consistency": float(1 - (favorite_cars['price'].std() / favorite_cars['price'].mean()))
            }
        }

        return jsonify({
            "analysis": analysis,
            "favorite_cars_count": len(favorite_cars),
            "analysis_version": "detailed_v2.0",
            "timestamp": datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Error analyzing user preferences: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/model-info', methods=['GET'])
def get_model_info():
    """모델 정보 조회"""
    return jsonify({
        "model_status": "loaded" if recommendation_system.car_features_matrix is not None else "not_loaded",
        "feature_dimensions": recommendation_system.car_features_matrix.shape[1] if recommendation_system.car_features_matrix is not None else 0,
        "total_cars": len(recommendation_system.cars_df) if recommendation_system.cars_df is not None else 0,
        "weights": recommendation_system.weights,
        "algorithm_version": "improved_v2.0"
    })

if __name__ == '__main__':
    # 시작 시 저장된 모델 로드 시도
    recommendation_system.load_model('car_recommendation_model.pkl')
    app.run(host='0.0.0.0', port=5001, debug=True)