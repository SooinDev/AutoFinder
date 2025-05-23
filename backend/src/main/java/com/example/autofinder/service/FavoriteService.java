package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.Favorite;
import com.example.autofinder.model.User;
import com.example.autofinder.repository.CarRepository;
import com.example.autofinder.repository.FavoriteRepository;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;

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
    }

    // 사용자의 관심 차량 목록 조회
    public List<Car> getUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        return favorites.stream().map(Favorite::getCar).collect(Collectors.toList());
    }
}