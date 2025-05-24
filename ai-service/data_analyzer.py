# data_analyzer.py
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

class CarDataAnalyzer:
    def __init__(self, cars_data, favorites_data, user_behaviors=None):
        self.cars_df = pd.DataFrame(cars_data)
        self.favorites_df = pd.DataFrame(favorites_data)
        self.user_behaviors = user_behaviors or {}

    def analyze_data_quality(self):
        """데이터 품질 분석"""
        print("=== 차량 데이터 분석 ===")
        print(f"총 차량 수: {len(self.cars_df)}")
        print(f"컬럼: {list(self.cars_df.columns)}")
        print(f"결측치:\n{self.cars_df.isnull().sum()}")

        print("\n=== 즐겨찾기 데이터 분석 ===")
        print(f"총 즐겨찾기 수: {len(self.favorites_df)}")
        print(f"유니크 사용자 수: {self.favorites_df['user_id'].nunique()}")
        print(f"유니크 차량 수: {self.favorites_df['car_id'].nunique()}")

    def create_features(self):
        """ML을 위한 특성 생성"""
        # 차량 특성 인코딩
        self.cars_df['brand'] = self.cars_df['model'].apply(self._extract_brand)
        self.cars_df['year_numeric'] = self.cars_df['year'].apply(self._extract_year)
        self.cars_df['age'] = 2024 - self.cars_df['year_numeric']

        # 가격 정규화
        self.cars_df['price_log'] = np.log1p(self.cars_df['price'])

        return self.cars_df

    def _extract_brand(self, model):
        brands = ["현대", "기아", "제네시스", "BMW", "벤츠", "아우디"]
        for brand in brands:
            if brand in str(model):
                return brand
        return "기타"

    def _extract_year(self, year_str):
        # 기존 로직과 동일
        pass