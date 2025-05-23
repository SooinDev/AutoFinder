package com.example.autofinder.service;

import com.example.autofinder.model.Car;
import com.example.autofinder.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;

    // 차량 추가 (CREATE)
    public Car addCar(Car car) {
        return carRepository.save(car);
    }

    // 특정 ID로 차량 조회 (READ)
    public Optional<Car> getCarById(Long id) {
        return carRepository.findById(id);
    }

    // 차량 목록 조회 (페이징 포함)
    public Page<Car> getAllCars(Pageable pageable) {
        return carRepository.findAll(pageable);
    }

    // 차량 목록 검색 (페이징 및 필터링 포함)
    public Page<Car> searchCars(String model, Integer minPrice, Integer maxPrice,
                                Integer minMileage, Integer maxMileage, String fuel,
                                String region, String year, Pageable pageable) {
        try {
            if (fuel != null) {
                fuel = URLDecoder.decode(fuel, StandardCharsets.UTF_8);
            }
            if (model != null) {
                model = URLDecoder.decode(model, StandardCharsets.UTF_8);
            }
            if (region != null) {
                region = URLDecoder.decode(region, StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 URL 인코딩 요청", e);
        }

        return carRepository.findByFilters(model, minPrice, maxPrice, minMileage, maxMileage, fuel, region, year, pageable);
    }

    // 차량 정보 수정 (UPDATE)
    public Car updateCar(Long id, Car updatedCar) {
        return carRepository.findById(id).map(car -> {
            car.setCarType(updatedCar.getCarType());
            car.setModel(updatedCar.getModel());
            car.setYear(updatedCar.getYear());
            car.setMileage(updatedCar.getMileage());
            car.setFuel(updatedCar.getFuel());
            car.setRegion(updatedCar.getRegion());
            car.setUrl(updatedCar.getUrl());
            car.setUrl(updatedCar.getImageUrl());
            return carRepository.save(car);
        }).orElseThrow(() -> new IllegalArgumentException("해당 ID의 차량을 찾을 수 없습니다."));
    }

    // 차량 삭제 (DELETE)
    public void deleteCar(Long id) {
        if (!carRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 ID의 차량을 찾을 수 없습니다.");
        }
        carRepository.deleteById(id);
    }
}
