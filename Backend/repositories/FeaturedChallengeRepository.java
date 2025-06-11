package com.example.demo.repositories;

import com.example.demo.model.Challenge;
import com.example.demo.model.FeaturedChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeaturedChallengeRepository extends JpaRepository<FeaturedChallenge, Long> {
    List<FeaturedChallenge> findAll();
    Optional<FeaturedChallenge> findByChallenge(Challenge challenge);
    void deleteByChallenge(Challenge challenge);

}
