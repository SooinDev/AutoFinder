package com.example.autofinder.repository;

import com.example.autofinder.model.Car;
import com.example.autofinder.model.Favorite;
import com.example.autofinder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndCar(User user, Car car);
}