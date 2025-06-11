package com.example.demo.repositories;

import com.example.demo.model.FeaturedWinner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeaturedWinnerRepository extends JpaRepository<FeaturedWinner, Long> {
}
