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

        // 즐겨찾기 추가 시 해당 사용자의 추천 캐시 무효화
        aiRecommendationService.onFavoriteChanged(userId);
        log.info("사용자 {}가 차량 {}을 즐겨찾기에 추가함. 추천 캐시 무효화됨.", userId, carId);
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

        // 즐겨찾기 삭제 시 해당 사용자의 추천 캐시 무효화
        aiRecommendationService.onFavoriteChanged(userId);
        log.info("사용자 {}가 차량 {}을 즐겨찾기에서 삭제함. 추천 캐시 무효화됨.", userId, carId);
    }

    // 사용자의 관심 차량 목록 조회
    public List<Car> getUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        return favorites.stream().map(Favorite::getCar).collect(Collectors.toList());
    }
}