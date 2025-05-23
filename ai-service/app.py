from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import StandardScaler
import re
import logging
from datetime import datetime

app = Flask(__name__)
app.config['JSON_ENSURE_ASCII'] = False

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class CarRecommendationSystem:
    def __init__(self):
        self.tfidf_vectorizer = TfidfVectorizer(max_features=1000, stop_words=None)
        self.scaler = StandardScaler()
        self.car_features_matrix = None
        self.cars_df = None

    def preprocess_year(self, year_str):
        if not year_str or year_str == "정보 없음":
            return 0
        digits = re.findall(r'\d+', str(year_str))
        if not digits:
            return 0
        year_num = int(digits[0])
        if len(digits[0]) == 2:
            return 1900 + year_num if year_num >= 90 else 2000 + year_num
        elif len(digits[0]) == 4:
            return year_num
        return 0

    def extract_features(self, cars_data):
        df = pd.DataFrame(cars_data)
        df['processed_year'] = df['year'].apply(self.preprocess_year)
        df['price'] = pd.to_numeric(df['price'], errors='coerce').fillna(0)
        df = df[df['price'] != 9999]
        df['mileage'] = pd.to_numeric(df['mileage'], errors='coerce').fillna(0)
        df['text_features'] = df['model'].fillna('') + ' ' + df['fuel'].fillna('') + ' ' + df['region'].fillna('')
        return df

    def build_feature_matrix(self, cars_data):
        self.cars_df = self.extract_features(cars_data)
        if self.cars_df.empty:
            logger.warning("No valid car data found")
            return
        text_features = self.tfidf_vectorizer.fit_transform(self.cars_df['text_features'])
        numeric_data = self.cars_df[['processed_year', 'price', 'mileage']].fillna(0)
        numeric_features_scaled = self.scaler.fit_transform(numeric_data)
        self.car_features_matrix = np.hstack([text_features.toarray(), numeric_features_scaled])
        logger.info(f"Feature matrix built with shape: {self.car_features_matrix.shape}")

    def calculate_user_preference_vector(self, favorite_car_ids):
        if not favorite_car_ids or self.car_features_matrix is None:
            return None
        favorite_indices = [self.cars_df[self.cars_df['id'] == car_id].index[0]
                            for car_id in favorite_car_ids if not self.cars_df[self.cars_df['id'] == car_id].empty]
        if not favorite_indices:
            return None
        favorite_features = self.car_features_matrix[favorite_indices]
        return np.mean(favorite_features, axis=0)

    def recommend_cars(self, user_preference_vector, exclude_ids=None, top_k=10):
        if user_preference_vector is None or self.car_features_matrix is None:
            return []
        similarities = cosine_similarity(user_preference_vector.reshape(1, -1), self.car_features_matrix)[0]
        car_scores = list(enumerate(similarities))
        if exclude_ids:
            exclude_indices = {self.cars_df[self.cars_df['id'] == car_id].index[0]
                               for car_id in exclude_ids if not self.cars_df[self.cars_df['id'] == car_id].empty}
            car_scores = [(idx, score) for idx, score in car_scores if idx not in exclude_indices]
        car_scores.sort(key=lambda x: x[1], reverse=True)
        recommendations = []
        for idx, score in car_scores[:top_k]:
            car_data = self.cars_df.iloc[idx].to_dict()
            recommendations.append({
                'car': car_data,
                'similarity_score': float(score),
                'recommendation_reason': self._generate_reason(car_data, score)
            })
        return recommendations

    def _generate_reason(self, car_data, score):
        reasons = ["선호 패턴과 매우 유사" if score > 0.8 else "선호 패턴과 유사" if score > 0.6 else "관심 가질만한 차량"]
        price = car_data.get('price', 0)
        if price < 1000:
            reasons.append("경제적인 가격")
        elif price > 5000:
            reasons.append("프리미엄 차량")
        year = car_data.get('processed_year', 0)
        if year > datetime.now().year - 3:
            reasons.append("최신 연식")
        return " • ".join(reasons)

recommendation_system = CarRecommendationSystem()

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy", "timestamp": datetime.now().isoformat()})

@app.route('/train', methods=['POST'])
def train_model():
    try:
        data = request.get_json(force=True)
        cars_data = data.get('cars', [])
        if not cars_data:
            return jsonify({"error": "No car data provided"}), 400
        recommendation_system.build_feature_matrix(cars_data)
        return jsonify({"message": "Model trained successfully", "cars_count": len(cars_data), "timestamp": datetime.now().isoformat()})
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
            return jsonify({"error": "Model not trained. Please train the model first."}), 400
        user_vector = recommendation_system.calculate_user_preference_vector(favorite_car_ids)
        if user_vector is None:
            return jsonify({"error": "Could not calculate user preferences"}), 400
        recommendations = recommendation_system.recommend_cars(user_vector, exclude_ids, top_k)
        return jsonify({"recommendations": recommendations, "total_count": len(recommendations), "timestamp": datetime.now().isoformat()})
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
            return jsonify({"error": "Model not trained"}), 400
        favorite_cars = recommendation_system.cars_df[recommendation_system.cars_df['id'].isin(favorite_car_ids)]
        if favorite_cars.empty:
            return jsonify({"error": "No favorite cars found in dataset"}), 400
        analysis = {
            "price_preferences": {
                "avg_price": float(favorite_cars['price'].mean()),
                "min_price": float(favorite_cars['price'].min()),
                "max_price": float(favorite_cars['price'].max()),
                "price_range": f"{int(favorite_cars['price'].min())}-{int(favorite_cars['price'].max())}만원"
            },
            "year_preferences": {
                "avg_year": float(favorite_cars['processed_year'].mean()),
                "preferred_year_range": f"{int(favorite_cars['processed_year'].min())}-{int(favorite_cars['processed_year'].max())}"
            },
            "brand_preferences": favorite_cars['model'].value_counts().head(3).to_dict(),
            "fuel_preferences": favorite_cars['fuel'].value_counts().to_dict(),
            "region_preferences": favorite_cars['region'].value_counts().to_dict(),
            "mileage_preferences": {
                "avg_mileage": float(favorite_cars['mileage'].mean()),
                "mileage_range": f"{int(favorite_cars['mileage'].min())}-{int(favorite_cars['mileage'].max())}km"
            }
        }
        return jsonify({"analysis": analysis, "favorite_cars_count": len(favorite_cars), "timestamp": datetime.now().isoformat()})
    except Exception as e:
        logger.error(f"Error analyzing user preferences: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)