package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.Favorite;
import com.example.autofinder.model.User;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.FavoriteRepository;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final AIRecommendationService aiRecommendationService;

    // 관심 차량 추가
    public void addFavorite(Long userId, Long carId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));

        if (favoriteRepository.findByUserAndCar(user, car).isPresent()) {
            throw new RuntimeException("Already favorited");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setCar(car);
        favoriteRepository.save(favorite);

        // 즐겨찾기 추가 후 현재 상태 로깅
        long totalFavorites = favoriteRepository.count();
        long userFavorites = favoriteRepository.findByUser(user).size();

        log.info("✨ 즐겨찾기 추가됨:");
        log.info("   - 사용자: {} (ID: {})", user.getUsername(), userId);
        log.info("   - 차량: {} (ID: {})", car.getModel(), carId);
        log.info("   - 사용자 총 즐겨찾기: {} 개", userFavorites);
        log.info("   - 전체 즐겨찾기: {} 개", totalFavorites);

        // 즐겨찾기 추가 시 해당 사용자의 추천 캐시 무효화 및 AI 재학습 체크
        aiRecommendationService.onFavoriteChanged(userId);

        // 첫 번째 즐겨찾기인 경우 특별 메시지
        if (totalFavorites == 1) {
            log.info("🎉 첫 번째 즐겨찾기 생성! AI 개인화 학습이 곧 시작됩니다.");
        } else if (totalFavorites % 5 == 0) {
            log.info("📊 즐겨찾기 {}개 달성! AI 모델 업데이트가 진행됩니다.", totalFavorites);
        }
    }

    // 관심 차량 삭제
    public void removeFavorite(Long userId, Long carId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));

        Favorite favorite = favoriteRepository.findByUserAndCar(user, car)
                .orElseThrow(() -> new RuntimeException("Favorite not found"));

        favoriteRepository.delete(favorite);

        // 즐겨찾기 삭제 후 현재 상태 로깅
        long totalFavorites = favoriteRepository.count();
        long userFavorites = favoriteRepository.findByUser(user).size();

        log.info("🗑️ 즐겨찾기 삭제됨:");
        log.info("   - 사용자: {} (ID: {})", user.getUsername(), userId);
        log.info("   - 차량: {} (ID: {})", car.getModel(), carId);
        log.info("   - 사용자 총 즐겨찾기: {} 개", userFavorites);
        log.info("   - 전체 즐겨찾기: {} 개", totalFavorites);

        // 즐겨찾기 삭제 시 해당 사용자의 추천 캐시 무효화 및 AI 재학습 체크
        aiRecommendationService.onFavoriteChanged(userId);

        // 마지막 즐겨찾기를 삭제한 경우
        if (totalFavorites == 0) {
            log.info("⚠️ 모든 즐겨찾기가 삭제되었습니다. AI 개인화 추천이 비활성화됩니다.");
        }
    }

    // 사용자의 관심 차량 목록 조회
    public List<Car> getUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Favorite> favorites = favoriteRepository.findByUser(user);

        log.debug("사용자 {}의 즐겨찾기 조회: {} 개", userId, favorites.size());

        return favorites.stream().map(Favorite::getCar).collect(Collectors.toList());
    }

    /**
     * 즐겨찾기 통계 정보 조회
     */
    public FavoriteStatistics getFavoriteStatistics() {
        long totalFavorites = favoriteRepository.count();
        long totalUsers = userRepository.count();
        long totalCars = carRepository.count();

        // 즐겨찾기가 있는 사용자 수
        long usersWithFavorites = favoriteRepository.findAll().stream()
                .map(favorite -> favorite.getUser().getId())
                .distinct()
                .count();

        // 즐겨찾기된 차량 수
        long favoritedCars = favoriteRepository.findAll().stream()
                .map(favorite -> favorite.getCar().getId())
                .distinct()
                .count();

        return new FavoriteStatistics(
                totalFavorites,
                totalUsers,
                usersWithFavorites,
                totalCars,
                favoritedCars,
                totalUsers > 0 ? (double) usersWithFavorites / totalUsers : 0.0,
                totalCars > 0 ? (double) favoritedCars / totalCars : 0.0
        );
    }

    /**
     * 사용자별 즐겨찾기 개수 확인
     */
    public int getUserFavoriteCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUser(user).size();
    }

    /**
     * 특정 차량이 즐겨찾기되었는지 확인
     */
    public boolean isCarFavorited(Long userId, Long carId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));

        return favoriteRepository.findByUserAndCar(user, car).isPresent();
    }

    /**
     * 즐겨찾기 통계 내부 클래스
     */
    public static class FavoriteStatistics {
        private final long totalFavorites;
        private final long totalUsers;
        private final long usersWithFavorites;
        private final long totalCars;
        private final long favoritedCars;
        private final double userParticipationRate;
        private final double carCoverageRate;

        public FavoriteStatistics(long totalFavorites, long totalUsers, long usersWithFavorites,
                                  long totalCars, long favoritedCars, double userParticipationRate,
                                  double carCoverageRate) {
            this.totalFavorites = totalFavorites;
            this.totalUsers = totalUsers;
            this.usersWithFavorites = usersWithFavorites;
            this.totalCars = totalCars;
            this.favoritedCars = favoritedCars;
            this.userParticipationRate = userParticipationRate;
            this.carCoverageRate = carCoverageRate;
        }

        // Getters
        public long getTotalFavorites() { return totalFavorites; }
        public long getTotalUsers() { return totalUsers; }
        public long getUsersWithFavorites() { return usersWithFavorites; }
        public long getTotalCars() { return totalCars; }
        public long getFavoritedCars() { return favoritedCars; }
        public double getUserParticipationRate() { return userParticipationRate; }
        public double getCarCoverageRate() { return carCoverageRate; }

        @Override
        public String toString() {
            return String.format(
                    "FavoriteStatistics{총즐겨찾기=%d, 전체사용자=%d, 즐겨찾기사용자=%d(%.1f%%), 전체차량=%d, 즐겨찾기차량=%d(%.1f%%)}",
                    totalFavorites, totalUsers, usersWithFavorites, userParticipationRate * 100,
                    totalCars, favoritedCars, carCoverageRate * 100
            );
        }
    }
}