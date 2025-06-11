package com.example.demo.repositories;


import com.example.demo.model.GlobalLeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface GlobalLeaderboardRepository extends JpaRepository<GlobalLeaderboardEntry, Long> {
    GlobalLeaderboardEntry findByUserEmail(String userEmail);
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalLeaderboardEntry e WHERE e.userEmail = :userEmail")
    void deleteByUserEmail(@Param("userEmail") String userEmail);
    GlobalLeaderboardEntry findByUsername(String username);
}
