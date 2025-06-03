import re
import time
import random
import pymysql
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import chromedriver_autoinstaller

# 자동으로 현재 Chrome 버전에 맞는 ChromeDriver 다운로드 및 설치
chromedriver_path = chromedriver_autoinstaller.install(cwd=True)
print(f"ChromeDriver installed at: {chromedriver_path}")

# MySQL 연결 설정
conn = pymysql.connect(
    host="localhost",
    user="root",
    password="Mysql1234!",
    database="autofinder",
    charset="utf8mb4"
)
cursor = conn.cursor()

# Selenium 옵션 설정
chrome_options = Options()
chrome_options.add_argument("--no-sandbox")
chrome_options.add_argument("--disable-dev-shm-usage")
chrome_options.add_argument("--headless")  # 백그라운드 실행
chrome_options.add_argument("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.7049.96 Safari/537.36")
chrome_options.add_argument("--disable-gpu")  # GPU 가속 비활성화

try:
    # Selenium WebDriver 실행
    service = Service(chromedriver_path)
    driver = webdriver.Chrome(service=service, options=chrome_options)
    print("Chrome WebDriver 초기화 성공")

    # 크롤링할 엔카 검색 URL
    base_urls = {
        "국산차": "http://www.encar.com/dc/dc_carsearchlist.do?carType=kor",
        "수입차": "http://www.encar.com/fc/fc_carsearchlist.do?carType=for"
    }

    # 차량 크롤링 실행
    for car_type, base_url in base_urls.items():
        for page in range(1, 6):  # 5페이지까지 크롤링
            search_url = f"{base_url}&page={page}"
            print(f"페이지 접속 시도: {search_url}")
            driver.get(search_url)
            time.sleep(random.uniform(0.5, 1.5))  # 요청 속도 조절

            # 차량 목록 가져오기 함수
            def get_car_list():
                try:
                    return WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.ID, "sr_normal"))
                    ).find_elements(By.TAG_NAME, "tr")
                except Exception as e:
                    print(f"차량 목록 가져오기 실패: {e}")
                    return []  # 차량이 없을 경우 빈 리스트 반환

            car_rows = get_car_list()
            print(f"[{car_type}] {page}페이지 차량 개수: {len(car_rows)}")

            if not car_rows:  # 차량이 없는 경우 다음 페이지로 이동
                print(f"[{car_type}] {page}페이지에 차량이 없습니다. 다음 페이지로 이동합니다.")
                continue

            for i in range(len(car_rows)):
                try:
                    # 최신 차량 목록 다시 가져오기 (핵심 수정)
                    car_rows = get_car_list()
                    if i >= len(car_rows):  # 인덱스 초과 방지
                        print(f"[경고] 차량 리스트 인덱스 초과 (i: {i}, 차량 개수: {len(car_rows)})")
                        break

                    car = car_rows[i]  # 최신 리스트에서 차량 가져오기

                    # 차량 정보 가져오기
                    name_element = car.find_element(By.CSS_SELECTOR, "td.inf")
                    raw_text = " ".join(name_element.text.strip().split("\n"))

                    car_model_match = re.search(r"^[가-힣A-Za-z0-9\s\-\(\)\.]+", raw_text)
                    car_model = car_model_match.group(0).strip() if car_model_match else "차종 정보 없음"

                    # 차량명이 '차종 정보 없음'이면 저장하지 않고 건너뛰기
                    if car_model == "차종 정보 없음":
                        print(f"[경고] 차량명 정보 없음으로 저장 건너뜀: {raw_text}")
                        continue

                    if re.search(r"\s\d{2}$", car_model):
                        car_model = car_model.rsplit(" ", 1)[0]

                    year_match = re.search(r"(\d{2}/\d{2}식|\d{2}/\d{2}식\(\d{2}년형\))", raw_text)
                    year = year_match.group(0) if year_match else "연식 정보 없음"

                    km_match = re.search(r"(\d{1,3}(?:,\d{3})*)km", raw_text)
                    km = km_match.group(1).replace(",", "") if km_match else None  # 주행거리가 없으면 None 저장

                    try:
                        km = int(km) if km is not None else None  # 문자열 → 정수 변환 (예외 방지)
                    except ValueError:
                        km = None  # 변환 불가능하면 None

                    price_element = car.find_element(By.CSS_SELECTOR, "td.prc_hs strong")
                    price_text = price_element.text.strip().replace(",", "").replace("원", "")
                    price = int(price_text) if price_text.isdigit() else 0

                    fuel_match = re.search(r"(가솔린|디젤|LPG|하이브리드|전기)", raw_text)
                    fuel = fuel_match.group(0) if fuel_match else "연료 정보 없음"

                    region_match = re.search(r"(서울|경기|부산|대구|대전|광주|울산|인천|강원|충청|전라|경상|제주)", raw_text)
                    region = region_match.group(0) if region_match else "지역 정보 없음"

                    link_element = car.find_element(By.TAG_NAME, "a")
                    detail_url = link_element.get_attribute("href")

                    # 차량 상세 페이지 이동
                    print(f"상세 페이지 접속: {detail_url}")
                    driver.get(detail_url)
                    time.sleep(random.uniform(2, 4))

                    # 정확한 차량 메인 이미지 가져오기 (swiper-slide-active)
                    try:
                        active_slide = WebDriverWait(driver, 5).until(
                            EC.presence_of_element_located((By.CSS_SELECTOR, "div.swiper-slide-active img"))
                        )
                        high_res_image_url = active_slide.get_attribute("src")
                    except Exception as e:
                        print(f"이미지 가져오기 실패: {e}")
                        high_res_image_url = "이미지 없음"

                    # 다시 메인 페이지로 복귀
                    driver.execute_script("window.history.back()")

                    # 페이지 로드 대기 (핵심 수정)
                    WebDriverWait(driver, 10).until(
                        EC.presence_of_element_located((By.ID, "sr_normal"))
                    )

                    # MySQL에 저장할 데이터
                    car_data = (car_type, car_model, year, km, price, fuel, region, detail_url, high_res_image_url)

                    query = """
                    INSERT INTO cars (car_type, model, year, mileage, price, fuel, region, url, image_url)
                    SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s FROM DUAL
                    WHERE NOT EXISTS (
                        SELECT 1 FROM cars 
                        WHERE model = %s 
                        AND year = %s 
                        AND (mileage = %s OR mileage IS NULL)
                    );
                    """
                    cursor.execute(query, car_data + (car_model, year, km))
                    conn.commit()

                    print(f"[{car_type}] 저장 완료: {car_model}, {year}, {km}, {fuel}, {region}")

                except Exception as e:
                    print(f"차량 크롤링 오류 발생: {e}")
                    # 오류가 발생해도 계속 진행
                    try:
                        # 메인 리스트 페이지로 돌아가기 시도
                        driver.get(search_url)
                        time.sleep(2)
                    except:
                        pass

except Exception as e:
    print(f"초기화 오류 발생: {e}")

finally:
    # WebDriver 종료
    try:
        driver.quit()
        print("WebDriver 종료 완료")
    except:
        print("WebDriver 종료 중 오류 발생")

    # MySQL 연결 종료
    try:
        cursor.close()
        conn.close()
        print("MySQL 연결 종료 완료")
    except:
        print("MySQL 연결 종료 중 오류 발생")

    print("크롤링 작업 완료!")