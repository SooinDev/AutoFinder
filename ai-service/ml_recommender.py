# ml_recommender.py (NaN 처리 강화 버전)
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import mean_squared_error, mean_absolute_error
from sklearn.impute import SimpleImputer  # 결측값 처리를 위해 추가
import pandas as pd
import numpy as np
import joblib

class MLCarRecommender:
    def __init__(self):
        self.model = GradientBoostingRegressor(
            n_estimators=100,
            learning_rate=0.1,
            max_depth=6,
            random_state=42
        )
        self.scaler = StandardScaler()
        self.imputer = SimpleImputer(strategy='median')  # 결측값 처리기 추가
        self.label_encoders = {}
        self.feature_columns = []
        self.is_trained = False

    def prepare_training_data(self, cars_df, favorites_df):
        """학습 데이터 준비"""
        print(f"원본 차량 데이터 크기: {cars_df.shape}")
        print(f"원본 즐겨찾기 데이터 크기: {favorites_df.shape}")

        # 데이터 정제
        cars_df_clean = self.clean_car_data(cars_df)
        print(f"정제된 차량 데이터 크기: {cars_df_clean.shape}")

        # 긍정적 샘플 (즐겨찾기한 차량) - 평점 4-5
        positive_samples = []
        for _, fav in favorites_df.iterrows():
            car_data = cars_df_clean[cars_df_clean['id'] == fav['car_id']]
            if not car_data.empty:
                sample = car_data.iloc[0].copy()
                sample['user_id'] = fav['user_id']
                sample['rating'] = np.random.uniform(4.0, 5.0)  # 높은 평점
                positive_samples.append(sample)

        print(f"긍정적 샘플 수: {len(positive_samples)}")

        # 부정적 샘플 (랜덤 차량) - 평점 1-3
        negative_samples = []
        users = favorites_df['user_id'].unique()
        favorited_cars = set(favorites_df['car_id'].unique())

        for user_id in users:
            # 각 사용자마다 즐겨찾기하지 않은 차량 중 랜덤 샘플링
            non_favorited = cars_df_clean[~cars_df_clean['id'].isin(favorited_cars)]
            if len(non_favorited) > 0:
                n_samples = min(3, len(non_favorited))  # 샘플 수를 줄여서 균형 맞추기
                sampled_cars = non_favorited.sample(n=n_samples, random_state=42)

                for _, car in sampled_cars.iterrows():
                    sample = car.copy()
                    sample['user_id'] = user_id
                    sample['rating'] = np.random.uniform(1.0, 3.0)  # 낮은 평점
                    negative_samples.append(sample)

        print(f"부정적 샘플 수: {len(negative_samples)}")

        # 전체 학습 데이터 생성
        all_samples = positive_samples + negative_samples
        training_df = pd.DataFrame(all_samples)

        # 학습 데이터도 정제
        training_df = self.clean_car_data(training_df)

        print(f"최종 학습 데이터 크기: {training_df.shape}")
        print(f"학습 데이터 생성 완료: 긍정 샘플 {len(positive_samples)}개, 부정 샘플 {len(negative_samples)}개")

        return training_df

    def clean_car_data(self, df):
        """차량 데이터 정제"""
        df_clean = df.copy()

        # 필수 컬럼 확인 및 생성
        if 'id' not in df_clean.columns:
            df_clean['id'] = range(len(df_clean))

        # 기본값으로 결측값 처리
        default_values = {
            'model': '알 수 없음',
            'year': '2020',
            'price': 2000,
            'mileage': 50000,
            'fuel': '가솔린',
            'region': '서울',
            'carType': '중형차'
        }

        for col, default_val in default_values.items():
            if col in df_clean.columns:
                df_clean[col] = df_clean[col].fillna(default_val)
            else:
                df_clean[col] = default_val

        # 수치형 데이터 변환 및 정제
        df_clean['price'] = pd.to_numeric(df_clean['price'], errors='coerce')
        df_clean['mileage'] = pd.to_numeric(df_clean['mileage'], errors='coerce')

        # 이상값 처리
        df_clean['price'] = df_clean['price'].clip(lower=100, upper=50000)  # 100만원 ~ 5억원
        df_clean['mileage'] = df_clean['mileage'].clip(lower=0, upper=500000)  # 0 ~ 50만km

        # 다시 한번 결측값 처리
        df_clean['price'] = df_clean['price'].fillna(2000)
        df_clean['mileage'] = df_clean['mileage'].fillna(50000)

        # 파생 변수 생성
        df_clean['year_numeric'] = df_clean['year'].apply(self._extract_year)
        df_clean['age'] = 2024 - df_clean['year_numeric']
        df_clean['brand'] = df_clean['model'].apply(self._extract_brand)

        # 연식이 이상한 경우 처리
        df_clean['age'] = df_clean['age'].clip(lower=0, upper=50)

        return df_clean

    def engineer_features(self, df):
        """특성 공학 - NaN 처리 강화"""
        features_df = df.copy()

        # 데이터 정제 먼저 수행
        features_df = self.clean_car_data(features_df)

        print("특성 공학 전 데이터 확인:")
        print(f"데이터 크기: {features_df.shape}")
        print(f"결측값 확인:\n{features_df.isnull().sum()}")

        # 범주형 변수 인코딩
        categorical_columns = ['brand', 'fuel', 'region', 'carType']
        for col in categorical_columns:
            if col in features_df.columns:
                # 결측값을 'Unknown'으로 대체
                features_df[col] = features_df[col].fillna('Unknown').astype(str)

                if col not in self.label_encoders:
                    self.label_encoders[col] = LabelEncoder()
                    features_df[f'{col}_encoded'] = self.label_encoders[col].fit_transform(features_df[col])
                else:
                    # 학습 시 보지 못한 값 처리
                    def safe_transform(x):
                        try:
                            return self.label_encoders[col].transform([str(x)])[0]
                        except ValueError:
                            # 새로운 값은 가장 빈번한 값으로 대체
                            most_common = self.label_encoders[col].classes_[0]
                            return self.label_encoders[col].transform([most_common])[0]

                    features_df[f'{col}_encoded'] = features_df[col].apply(safe_transform)

        # 수치형 특성 선택
        numerical_features = ['user_id', 'price', 'year_numeric', 'age', 'mileage']
        categorical_encoded = [f'{col}_encoded' for col in categorical_columns if col in features_df.columns]

        all_features = numerical_features + categorical_encoded

        # 존재하는 컬럼만 선택
        available_features = [col for col in all_features if col in features_df.columns]

        # 특성 데이터 추출
        X = features_df[available_features].copy()

        # 모든 컬럼을 수치형으로 변환
        for col in X.columns:
            X[col] = pd.to_numeric(X[col], errors='coerce')

        # 결측값 최종 처리
        print("특성 추출 후 결측값 확인:")
        print(X.isnull().sum())

        # SimpleImputer로 결측값 처리
        if X.isnull().any().any():
            print("결측값 처리 중...")
            X_filled = pd.DataFrame(
                self.imputer.fit_transform(X),
                columns=X.columns,
                index=X.index
            )
        else:
            X_filled = X

        # 무한값 처리
        X_filled = X_filled.replace([np.inf, -np.inf], np.nan)
        X_filled = X_filled.fillna(0)

        # 최종 확인
        print("최종 특성 데이터:")
        print(f"크기: {X_filled.shape}")
        print(f"결측값: {X_filled.isnull().sum().sum()}")
        print(f"무한값: {np.isinf(X_filled.values).sum()}")

        self.feature_columns = available_features
        print(f"사용할 특성: {self.feature_columns}")

        return X_filled

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
                year = int(digits[:4])
                # 합리적인 범위 확인
                if 1990 <= year <= 2024:
                    return year
                else:
                    return 2020
            elif len(digits) == 2:
                year = int(digits)
                if year <= 24:
                    return 2000 + year
                else:
                    return 1900 + year
            else:
                return 2020

        except:
            return 2020

    def _extract_brand(self, model_str):
        """브랜드 추출"""
        if pd.isna(model_str) or model_str == '':
            return "기타"

        brands = ["현대", "기아", "제네시스", "르노", "쉐보레", "쌍용", "BMW", "벤츠", "아우디", "볼보"]
        model_str = str(model_str)

        for brand in brands:
            if brand in model_str:
                return brand

        # 첫 번째 단어를 브랜드로 사용
        words = model_str.split()
        return words[0] if words else "기타"

    def train(self, cars_df, favorites_df):
        """모델 학습"""
        try:
            print("=== ML 모델 학습 시작 ===")

            print("1. 학습 데이터 준비 중...")
            training_df = self.prepare_training_data(cars_df, favorites_df)

            if training_df.empty:
                print("❌ 학습 데이터가 없습니다.")
                return False

            print("2. 특성 공학 수행 중...")
            X = self.engineer_features(training_df)

            if 'rating' not in training_df.columns:
                print("❌ 평점 데이터가 없습니다.")
                return False

            y = training_df['rating'].copy()

            # y 값도 정제
            y = pd.to_numeric(y, errors='coerce')
            y = y.fillna(3.0)  # 기본값 3.0
            y = y.clip(lower=1.0, upper=5.0)  # 1-5 범위로 제한

            print(f"특성 데이터 크기: {X.shape}")
            print(f"타겟 데이터 크기: {y.shape}")
            print(f"특성 데이터 결측값: {X.isnull().sum().sum()}")
            print(f"타겟 데이터 결측값: {y.isnull().sum()}")

            # 최소 학습 데이터 확인
            if len(X) < 5:
                print("❌ 학습 데이터가 너무 적습니다. 최소 5개 필요.")
                return False

            # 인덱스 정렬
            X = X.reset_index(drop=True)
            y = y.reset_index(drop=True)

            print("3. 데이터 분할 중...")
            # 데이터 분할
            if len(X) > 10:
                X_train, X_test, y_train, y_test = train_test_split(
                    X, y, test_size=0.2, random_state=42
                )
            else:
                # 데이터가 적으면 전체를 학습에 사용
                X_train, X_test = X, X
                y_train, y_test = y, y

            print("4. 특성 정규화 중...")
            # 특성 정규화
            X_train_scaled = self.scaler.fit_transform(X_train)
            X_test_scaled = self.scaler.transform(X_test)

            # 정규화 후에도 확인
            if np.isnan(X_train_scaled).any() or np.isinf(X_train_scaled).any():
                print("❌ 정규화 후에도 NaN 또는 무한값이 존재합니다.")
                print("NaN 개수:", np.isnan(X_train_scaled).sum())
                print("무한값 개수:", np.isinf(X_train_scaled).sum())
                return False

            print("5. 모델 학습 중...")
            self.model.fit(X_train_scaled, y_train)

            print("6. 모델 평가 중...")
            # 모델 평가
            train_pred = self.model.predict(X_train_scaled)
            test_pred = self.model.predict(X_test_scaled)

            train_mse = mean_squared_error(y_train, train_pred)
            test_mse = mean_squared_error(y_test, test_pred)

            print(f"✅ 훈련 MSE: {train_mse:.4f}")
            print(f"✅ 테스트 MSE: {test_mse:.4f}")

            # 특성 중요도 출력
            if hasattr(self.model, 'feature_importances_'):
                feature_importance = pd.DataFrame({
                    'feature': self.feature_columns,
                    'importance': self.model.feature_importances_
                }).sort_values('importance', ascending=False)

                print("\n📊 특성 중요도 TOP 5:")
                print(feature_importance.head().to_string(index=False))

            self.is_trained = True
            print("🎉 모델 학습 완료!")
            return True

        except Exception as e:
            print(f"❌ 모델 학습 중 오류: {str(e)}")
            import traceback
            traceback.print_exc()
            return False

    def predict_user_preferences(self, user_id, candidate_cars_df):
        """사용자 선호도 예측"""
        if not self.is_trained:
            raise ValueError("모델이 학습되지 않았습니다.")

        # 후보 차량에 사용자 ID 추가
        candidates = candidate_cars_df.copy()
        candidates['user_id'] = user_id

        # 특성 추출
        X = self.engineer_features(candidates)
        X_scaled = self.scaler.transform(X)

        # 예측
        predictions = self.model.predict(X_scaled)

        # 결과 정리
        candidates['predicted_rating'] = predictions
        candidates = candidates.sort_values('predicted_rating', ascending=False)

        return candidates

    def get_recommendations(self, user_id, cars_df, exclude_ids=None, top_k=10):
        """추천 생성"""
        if exclude_ids is None:
            exclude_ids = []

        # 후보 차량 필터링
        candidate_cars = cars_df[~cars_df['id'].isin(exclude_ids)].copy()

        if candidate_cars.empty:
            return []

        try:
            # 예측
            predictions = self.predict_user_preferences(user_id, candidate_cars)

            # 상위 K개 선택
            top_recommendations = predictions.head(top_k)

            # 결과 포맷
            recommendations = []
            for _, row in top_recommendations.iterrows():
                recommendations.append({
                    'car': {
                        'id': int(row['id']),
                        'model': str(row.get('model', '')),
                        'year': str(row.get('year', '')),
                        'price': int(row.get('price', 0)),
                        'fuel': str(row.get('fuel', '')),
                        'region': str(row.get('region', '')),
                        'brand': str(row.get('brand', '')),
                        'carType': str(row.get('carType', ''))
                    },
                    'similarity_score': float(min(max(row['predicted_rating'] / 5.0, 0), 1)),
                    'recommendation_reason': f"ML 예측 점수: {row['predicted_rating']:.2f}/5.0"
                })

            return recommendations

        except Exception as e:
            print(f"추천 생성 중 오류: {str(e)}")
            import traceback
            traceback.print_exc()
            return []

    def save_model(self, path):
        """모델 저장"""
        try:
            model_data = {
                'model': self.model,
                'scaler': self.scaler,
                'imputer': self.imputer,
                'label_encoders': self.label_encoders,
                'feature_columns': self.feature_columns,
                'is_trained': self.is_trained
            }
            joblib.dump(model_data, path)
            print(f"💾 모델이 {path}에 저장되었습니다.")
        except Exception as e:
            print(f"❌ 모델 저장 실패: {e}")

    def load_model(self, path):
        """모델 로드"""
        try:
            model_data = joblib.load(path)
            self.model = model_data['model']
            self.scaler = model_data['scaler']
            self.imputer = model_data.get('imputer', SimpleImputer(strategy='median'))
            self.label_encoders = model_data['label_encoders']
            self.feature_columns = model_data['feature_columns']
            self.is_trained = model_data['is_trained']
            print(f"📂 모델이 {path}에서 로드되었습니다.")
            return True
        except Exception as e:
            print(f"❌ 모델 로드 실패: {e}")
            return False