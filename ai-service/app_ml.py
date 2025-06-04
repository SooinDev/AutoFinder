# app_ml.py - 응답 형식 수정
from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
from ml_recommender import MLCarRecommender
from data_analyzer import CarDataAnalyzer
import logging
from datetime import datetime
import traceback
import os

app = Flask(__name__)
app.config['JSON_ENSURE_ASCII'] = False

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 전역 ML 시스템
ml_recommender = MLCarRecommender()
MODEL_PATH = './models/ml_car_recommender.joblib'

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "model_type": "machine_learning",
        "model_trained": ml_recommender.is_trained,
        "timestamp": datetime.now().isoformat()
    })

@app.route('/train', methods=['POST'])
def train_model():
    try:
        data = request.get_json(force=True)
        cars_data = data.get('cars', [])
        favorites_data = data.get('favorites', [])
        user_behaviors = data.get('user_behaviors', {})

        logger.info(f"ML 학습 시작: 차량 {len(cars_data)}개, 즐겨찾기 {len(favorites_data)}개")

        if not cars_data:
            return jsonify({"error": "차량 데이터가 필요합니다."}), 400

        if len(favorites_data) < 5:
            return jsonify({"error": "ML 학습을 위해 최소 5개의 즐겨찾기가 필요합니다."}), 400

        # 데이터 전처리
        cars_df = pd.DataFrame(cars_data)
        favorites_df = pd.DataFrame(favorites_data)

        # 데이터 분석
        analyzer = CarDataAnalyzer(cars_data, favorites_data, user_behaviors)
        cars_df = analyzer.create_features()

        # ML 모델 학습
        success = ml_recommender.train(cars_df, favorites_df)

        if success:
            # 모델 저장
            os.makedirs('./models', exist_ok=True)
            ml_recommender.save_model(MODEL_PATH)

            return jsonify({
                "message": "ML 모델 학습 완료",
                "model_type": "gradient_boosting",
                "cars_count": len(cars_data),
                "favorites_count": len(favorites_data),
                "features_used": ml_recommender.feature_columns,
                "timestamp": datetime.now().isoformat()
            })
        else:
            return jsonify({"error": "ML 모델 학습 실패"}), 500

    except Exception as e:
        logger.error(f"ML 학습 중 오류: {str(e)}")
        logger.error(traceback.format_exc())
        return jsonify({"error": f"ML 학습 중 오류: {str(e)}"}), 500

@app.route('/recommend', methods=['POST'])
def recommend():
    try:
        data = request.get_json(force=True)

        user_id = data.get('user_id')
        favorite_car_ids = data.get('favorite_car_ids', [])
        candidate_cars = data.get('candidate_cars', [])
        exclude_ids = data.get('exclude_ids', [])
        top_k = data.get('top_k', 10)

        logger.info(f"ML 추천 요청: 사용자 {user_id}, 후보차량 {len(candidate_cars)}개")

        if not ml_recommender.is_trained:
            # 모델이 학습되지 않은 경우 로드 시도
            if os.path.exists(MODEL_PATH):
                success = ml_recommender.load_model(MODEL_PATH)
                if not success:
                    return create_empty_response("모델 로드 실패")
            else:
                return create_empty_response("모델이 학습되지 않았습니다")

        # 후보 차량이 제공된 경우 (딥러닝 추천)
        if candidate_cars and user_id:
            cars_df = pd.DataFrame(candidate_cars)

            # 데이터 전처리
            analyzer = CarDataAnalyzer(candidate_cars, [], {})
            cars_df = analyzer.create_features()

            # ML 추천 생성
            recommendations = ml_recommender.get_recommendations(
                user_id=user_id,
                cars_df=cars_df,
                exclude_ids=exclude_ids,
                top_k=top_k
            )

            logger.info(f"ML 추천 생성 완료: {len(recommendations)}개")

        else:
            # 기존 방식 (즐겨찾기 기반)
            logger.info("기존 방식으로 추천 처리")
            recommendations = create_legacy_recommendations(favorite_car_ids, exclude_ids, top_k)

        # ✅ Java 백엔드가 기대하는 정확한 형식으로 응답
        response = {
            "recommendations": recommendations,
            "totalCount": len(recommendations),  # Java에서 getTotalCount() 사용
            "timestamp": datetime.now().isoformat()
        }

        logger.info(f"응답 전송: {len(recommendations)}개 추천")
        return jsonify(response)

    except Exception as e:
        logger.error(f"ML 추천 중 오류: {str(e)}")
        logger.error(traceback.format_exc())

        # 에러 발생 시에도 빈 추천 리스트 반환 (Java 에러 방지)
        return create_empty_response(f"오류: {str(e)}")

def create_empty_response(message="추천 결과 없음"):
    """빈 응답 생성"""
    return jsonify({
        "recommendations": [],
        "totalCount": 0,
        "timestamp": datetime.now().isoformat(),
        "message": message
    })

def create_legacy_recommendations(favorite_car_ids, exclude_ids, top_k):
    """기존 방식 추천 (간단한 구현)"""
    try:
        # 실제로는 차량 데이터베이스에서 가져와야 하지만,
        # 여기서는 빈 리스트 반환 (Java 백엔드가 폴백 처리)
        return []
    except Exception:
        return []

def safe_convert_to_serializable(obj):
    """JSON 직렬화 가능한 형태로 변환"""
    if isinstance(obj, (np.int64, np.int32)):
        return int(obj)
    elif isinstance(obj, (np.float64, np.float32)):
        return float(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    elif pd.isna(obj):
        return None
    else:
        return obj

if __name__ == '__main__':
    # 시작 시 모델 로드 시도
    if os.path.exists(MODEL_PATH):
        success = ml_recommender.load_model(MODEL_PATH)
        if success:
            logger.info("✅ 기존 ML 모델을 로드했습니다.")
        else:
            logger.warning("⚠️ 모델 로드에 실패했습니다.")

    logger.info("🚀 ML 기반 AI 추천 서버 시작 중...")
    app.run(host='0.0.0.0', port=5001, debug=True)